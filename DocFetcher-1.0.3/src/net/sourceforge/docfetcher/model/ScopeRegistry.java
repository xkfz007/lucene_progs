/*******************************************************************************
 * Copyright (c) 2007, 2008 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.UtilFile;
import net.sourceforge.docfetcher.util.UtilGUI;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.eclipse.swt.widgets.Display;

/**
 * A registry for managing registered scopes.
 * 
 * @author Tran Nam Quang
 */
public class ScopeRegistry implements Serializable {
	
	// Setting this avoids errors when searching for very generic search terms like "*?".
	static {
		BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
	}
	
	static final long serialVersionUID = 1;
	
	/**
	 * Singleton instance
	 */
	private static ScopeRegistry instance;
	
	/**
	 * This registry's entries
	 */
	private Set<RootScope> rootScopes = new TreeSet<RootScope> ();
	
	/**
	 * The indexing queue.
	 */
	private transient List<Job> indexingJobs = new ArrayList<Job> ();
	
	/**
	 * The currently processed indexing job.
	 */
	private transient Job currentJob;
	
	/**
	 * The thread that carries out the indexing.
	 */
	private transient Thread indexingThread;
	
	/**
	 * Event: Changes in the indexing queue.
	 */
	private transient Event<ScopeRegistry> evtQueueChanged = new Event<ScopeRegistry> ();
	
	/**
	 * Event: The list of registered <tt>RootScope</tt>'s has changed.
	 */
	private transient Event<ScopeRegistry>  evtRegistryRootChanged = new Event<ScopeRegistry> ();
	
	/**
	 * Event: Changes somewhere down the registry.
	 */
	private transient Event<ScopeRegistry> evtRegistryChanged = new Event<ScopeRegistry> ();
	
	/**
	 * Singleton constructor
	 */
	private ScopeRegistry() {}
	
	/**
	 * Event: Changes in the indexing queue.
	 */
	public Event<ScopeRegistry> getEvtQueueChanged() {
		return evtQueueChanged;
	}
	
	/**
	 * Event: The list of registered <tt>RootScope</tt>s has changed.
	 */
	public Event<ScopeRegistry> getEvtRegistryRootChanged() {
		return evtRegistryRootChanged;
	}
	
	/**
	 * Event: Changes somewhere down the registry.
	 */
	public Event<ScopeRegistry> getEvtRegistryChanged() {
		return evtRegistryChanged;
	}
	
	/**
	 * Returns the entries of this registry.
	 */
	public RootScope[] getEntries() {
		return rootScopes.toArray(new RootScope[rootScopes.size()]);
	}
	
	/**
	 * Returns the entries of this registry.
	 */
	public List<RootScope> getEntriesList() {
		return new ArrayList<RootScope> (rootScopes);
	}
	
	/**
	 * Returns the checked entries of this registry.
	 */
	public RootScope[] getCheckedEntries() {
		List<RootScope> checkedEntries = new ArrayList<RootScope> (rootScopes.size());
		for (RootScope entry : rootScopes) {
			if (entry.isChecked())
				checkedEntries.add(entry);
		}
		return checkedEntries.toArray(new RootScope[checkedEntries.size()]);
	}
	
	/**
	 * Returns the entries that are marked for deletion on exit.
	 */
	public List<RootScope> getTemporaryEntries() {
		List<RootScope> tempEntries = new ArrayList<RootScope> (rootScopes.size());
		for (RootScope entry : rootScopes)
			if (entry.isDeleteOnExit())
				tempEntries.add(entry);
		return tempEntries;
	}
	
	/**
	 * Checks whether the given <tt>RootScope</tt>s intersect with registered
	 * entries or entries in the indexing queue. If so, a string containing a
	 * warning message is returned, which can be used for display in a message
	 * box. Otherwise, null is returned.
	 */
	public String checkIntersection(RootScope... newScopes) {
		for (RootScope newScope : newScopes) {
			if (intersectsEntry(newScope))
				return Msg.inters_indexes.value();
			if (intersectsQueue(newScope))
				return Msg.inters_queue.value();
		}
		return null;
	}
	
	/**
	 * Returns whether the given <tt>RootScope</tt> intersects with registered
	 * entries.
	 */
	public boolean intersectsEntry(RootScope newScope) {
		for (RootScope oldScope : rootScopes)
			if (oldScope.equals(newScope) ||
				oldScope.contains(newScope) ||
				newScope.contains(oldScope))
				return true;
		return false;
	}
	
	/**
	 * Returns whether the given <tt>RootScope</tt> intersects with entries in
	 * the indexing queue.
	 */
	public boolean intersectsQueue(RootScope newScope) {
		for (Job job : getJobs()) {
			Scope js = job.getScope();
			Scope ns = newScope;
			if (ns.equals(js) || ns.contains(js) || js.contains(ns))
				return true;
		}
		return false;
	}
	
	/**
	 * Returns whether the given <tt>RootScope</tt> intersects with entries in
	 * the indexing queue, excluding the currently processed entry.
	 */
	public boolean intersectsInactiveQueue(RootScope newScope) {
		for (Job job : getJobs()) {
			if (job == currentJob) // Don't use equals(..) here, this must be identity
				continue;
			Scope js = job.getScope();
			Scope ns = newScope;
			if (ns.equals(js) || ns.contains(js) || js.contains(ns))
				return true;
		}
		return false;
	}
	
	/**
	 * Returns whether the given directory is already registered herein.
	 */
	public boolean containsEntry(File directory) {
		File relativeDir = UtilFile.getRelativeFile(Const.USER_DIR_FILE, directory);
		for (RootScope rootScope : rootScopes) {
			if (rootScope.file.equals(relativeDir))
				return true;
		}
		return false;
	}

	/**
	 * Returns the <tt>RootScope</tt> for a given directory
	 */
	public RootScope getEntry(File directory) {
		File relativeDir = UtilFile.getRelativeFile(Const.USER_DIR_FILE, directory);
		for (RootScope rootScope : rootScopes) {
			if (rootScope.file.equals(relativeDir))
				return rootScope;
		}
		return null;
	}

	/**
	 * Returns whether the given <tt>RootScope</tt> is already registered
	 * herein.
	 */
	public boolean containsEntry(RootScope rootScope) {
		return rootScopes.contains(rootScope);
	}
	
	/**
	 * Returns whether the given directory is an index directory of one of the
	 * registered <tt>RootScope</tt> entries. An index directory of a
	 * <tt>RootScope</tt> is the place where its Lucene index files are stored.
	 */
	public boolean containsIndexDir(File indexDir) {
		for (RootScope rootScope : rootScopes) {
			if (rootScope.getIndexDir().equals(indexDir))
				return true;
		}
		return false;
	}
	
	/**
	 * Returns a <tt>Scope</tt> object for the given directory. The directory
	 * can be anywhere in the tree of the registered <tt>Scope</tt>s. Null is
	 * returned if the directory is not registered.
	 */
	public Scope getScopeDeep(File directory) {
		for (RootScope rootScope : rootScopes) {
			Scope scope = rootScope.getScopeDeep(directory);
			if (scope != null)
				return scope;
		}
		return null;
	}
	
	/**
	 * Returns an HTMLPair object somewhere under the registered entries for the
	 * given HTML directory, or null if none is found (recursive).
	 */
	public HTMLPair getHTMLPair(File directory) {
		for (RootScope rootScope : rootScopes) {
			HTMLPair htmlPair = rootScope.getHTMLPair(directory);
			if (htmlPair != null)
				return htmlPair;
		}
		return null;
	}

	/**
	 * Removes the given <tt>RootScope</tt>s from the registry and deletes
	 * their corresponding index files.
	 */
	public void remove(RootScope... scopes) {
		for (RootScope rootScope : scopes)
			if (rootScopes.remove(rootScope))
				rootScope.deleteIndex();
		evtRegistryRootChanged.fireUpdate(this);
		evtRegistryChanged.fireUpdate(this);
	}
	
	/**
	 * Adds a new indexing job to the queue. It does not check for intersection
	 * with registered entries or entries in the queue. If the indexing box is
	 * visible, the appropriate method in the <tt>IndexingDialog</tt> class
	 * should be used istead.
	 */
	public void addJob(Job newJob) {
		indexingJobs.add(newJob);
		newJob.evtReadyStateChanged.add(new Event.Listener<Job> () {
			public void update(Job job) {
				evtQueueChanged.fireUpdate(ScopeRegistry.this);
				startNextJob();
			}
		});
		evtQueueChanged.fireUpdate(this);
		startNextJob();
	}
	
	/**
	 * Returns the entries of the indexing queue.
	 */
	public Job[] getJobs() {
		return indexingJobs.toArray(new Job[indexingJobs.size()]);
	}
	
	/**
	 * Returns the currently processed entry in the indexing queue, or null if
	 * none is processed right now. This is not necessarily the first item in
	 * the queue.
	 */
	public Job getCurrentJob() {
		return currentJob;
	}
	
	/**
	 * Returns the entries of the indexing queue that are 'ready for indexing',
	 * i.e. the entries for which the user has given indexing permission by
	 * pressing the 'submit' button on the corresponding indexing tab.
	 */
	public Job[] getSubmittedJobs() {
		List<Job> sj = new ArrayList<Job> ();
		for (Job candidate : indexingJobs)
			if (candidate.isReadyForIndexing())
				sj.add(candidate);
		return sj.toArray(new Job[sj.size()]);
	}
	
	/**
	 * Process the next entry in the indexing queue. After each processed entry,
	 * the method will move on to the next allowed entry until none is left.
	 */
	private void startNextJob() {
		if (indexingThread != null) return;
		
		// Get the next entry that is ready for indexing
		currentJob = null;
		for (Job candidate : indexingJobs) {
			if (candidate.isReadyForIndexing()) {
				currentJob = candidate;
				break;
			}
		}
		
		// Stop if there's no ready entry left
		if (currentJob == null) {
			try {
				save(); // Save registry after queue is emptied, just in case the user successfully kills the app...
			} catch (IOException e) {
				UtilGUI.showErrorMsg(Msg.write_error.value());
			}
			return;
		}
		
		evtQueueChanged.fireUpdate(this);
		indexingThread = new Thread() {
			public void run() {
				try {
					// Caching variables (necessary, because later currentJob may be null)
					boolean addToReg = currentJob.isAddToRegistry();
					boolean doRebuild = currentJob.isDoRebuild();
					RootScope currentScope = currentJob.getScope();
					
					// Indexing
					if (doRebuild && ! addToReg)
						currentScope.reindex();
					else
						currentScope.updateIndex();
					
					// Postprocessing
					boolean interrupted = Thread.currentThread().isInterrupted();
					if (doRebuild && interrupted) {
						if (addToReg)
							currentScope.deleteIndex();
						else
							remove(currentScope);
					}
					else if (addToReg && ! interrupted) {
						rootScopes.add(currentScope);
						evtRegistryRootChanged.fireUpdate(ScopeRegistry.this);
					}
					evtRegistryChanged.fireUpdate(ScopeRegistry.this);
				} catch (FileNotFoundException e) {
					// Do nothing here, this will be handled by aspect 'IndexingFeedback'
				} catch (IOException e) {
					// This happens when the file system is not writable for some reason
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							UtilGUI.showErrorMsg(Msg.write_error.value());
						}
					});
				} finally {
					indexingThread = null;
					indexingJobs.remove(currentJob);
					currentJob = null;
					evtQueueChanged.fireUpdate(ScopeRegistry.this);
					startNextJob();
				}
			}
		};
		indexingThread.start();
	}

	/**
	 * Removes the given job from the queue (based on an equality check). If the
	 * given job is the currently processed job, the processing will be
	 * terminated and processing of the next entry will start.
	 * <p>
	 * If the indexing box is open, the appropriate method in the
	 * <tt>IndexingDialog</tt> should be used instead.
	 */
	public void removeFromQueue(Job job) {
		// Remove entries from queue
		List<Job> removals = new ArrayList<Job> ();
		for (Job candidate : indexingJobs)
			if (candidate.equals(job))
				removals.add(candidate);
		indexingJobs.removeAll(removals);
		
		// Send interrupt signal to the indexing thread if it's processing the given job
		if (job.equals(currentJob)) {
			/*
			 * This assignment here is important, because the next interrupt
			 * call won't be fast enough in doing the very same thing, causing
			 * the program to freeze under some circumstances. (And I have no
			 * idea why...)
			 */
			currentJob = null;
			
			indexingThread.interrupt(); // thread will continue with the next entry
		}
		evtQueueChanged.fireUpdate(this);
	}
	
	/**
	 * Removes all entries from the indexing queue. If the indexing box is open,
	 * the appropriate method in the <tt>IndexingDialog</tt> should be used
	 * instead.
	 */
	public void clearQueue() {
		if (indexingThread != null)
			indexingThread.interrupt();
		indexingJobs.clear();
		currentJob = null;
		indexingThread = null;
		evtQueueChanged.fireUpdate(this);
	}
	
	/**
	 * Loads and returns the singleton instance of this class. Returns the
	 * instance without loading if it has already been loaded.
	 */
	public static ScopeRegistry getInstance() {
		if (instance != null) return instance;
		try {
			instance = (ScopeRegistry) Serializer.load(ScopeRegistry.class, Const.INDEX_PARENT_FILE);
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
		}
		if (instance == null) {
			instance = new ScopeRegistry();
		}
		else {
			instance.indexingJobs = new ArrayList<Job> ();
			instance.evtQueueChanged = new Event<ScopeRegistry> ();
			instance.evtRegistryRootChanged = new Event<ScopeRegistry> ();
			instance.evtRegistryChanged = new Event<ScopeRegistry> ();
		}
		return instance;
	}
	
	/**
	 * Saves this registry to disk.
	 * 
	 * @throws IOException
	 *             if the write process failed.
	 */
	public void save() throws IOException {
		Serializer.save(this, Const.INDEX_PARENT_FILE);
		
		// Write indexes.txt file used by the daemon
		FileWriter writer = new FileWriter(Const.INDEX_DAEMON_FILE);
		for (RootScope rootScope : rootScopes) {
			writer.write(rootScope.file.getAbsolutePath());
			writer.write(Const.LS);
		}
		writer.close();
	}
	
	/**
	 * Performs a search on all indexes for <tt>searchString</tt> and returns an
	 * array of results. Term strings for highlighting in the preview panel will
	 * be inserted into the provided <tt>terms</tt> list.
	 * 
	 * @throws SearchException
	 *             Thrown if no indexes have been created yet, if the
	 *             <tt>searchString</tt> is invalid, or if an IOException
	 *             occurred.
	 */
	public ResultDocument[] search(String searchString) throws SearchException {
		MultiSearcher multiSearcher = null;
		try {
			// Build a lucene query object
			QueryParser queryParser = new QueryParser(
					Version.LUCENE_CURRENT,
					Document.contents,
					RootScope.analyzer
			);
			queryParser.setAllowLeadingWildcard(true);
			queryParser.setMultiTermRewriteMethod(MultiTermQuery.SCORING_BOOLEAN_QUERY_REWRITE);
			if (! Pref.Bool.UseOrOperator.getValue())
				queryParser.setDefaultOperator(QueryParser.AND_OPERATOR);
			Query query = queryParser.parse(searchString);

			// Check that all indexes still exist
			for (RootScope rootScope : rootScopes)
				if (! rootScope.getIndexDir().exists())
					throw new SearchException(Msg.folders_not_found.value() + "\n" + //$NON-NLS-1$
							rootScope.getIndexDir().getAbsolutePath());
			
			// Perform search
			Searchable[] searchables = new Searchable[rootScopes.size()];
			int i = 0;
			for (RootScope rootScope : rootScopes) {
				Directory luceneIndexDir = new SimpleFSDirectory(rootScope.getIndexDir());
				searchables[i++] = new IndexSearcher(luceneIndexDir);
			}
			multiSearcher = new MultiSearcher(searchables);
			
			TopScoreDocCollector collector = TopScoreDocCollector.create(Pref.Int.MaxResultsTotal.getValue(), false);
			multiSearcher.search(query, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;
			
			// Process results 
			final ResultDocument[] results = new ResultDocument[hits.length];
			for (i = 0; i < results.length; i++)
				results[i] = new ResultDocument(
						multiSearcher.doc(hits[i].doc),
						hits[i].score,
						query
				);
			
			return results;
		}
		catch (final ParseException e) {
			throw new SearchException(Msg.invalid_query.value() + "\n" + e.getLocalizedMessage()); //$NON-NLS-1$
		}
		catch (final IOException e) {
			throw new SearchException(e.getLocalizedMessage());
		}
		finally {
			if (multiSearcher != null) {
				try {
					multiSearcher.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public class SearchException extends Exception {
		static final long serialVersionUID = 1;
		public SearchException(String msg) {
			super(msg);
		}
	}
	
}
