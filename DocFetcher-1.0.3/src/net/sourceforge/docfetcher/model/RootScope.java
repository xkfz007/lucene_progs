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
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.parse.ParseException;
import net.sourceforge.docfetcher.parse.ParserRegistry;
import net.sourceforge.docfetcher.util.UtilFile;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.ThreadInterruptedException;
import org.apache.lucene.util.Version;

/**
 * An object representation for top-level-directories in the search scope.
 * Enhances the <tt>Scope</tt> class with indexing abilites.
 * 
 * @author Tran Nam Quang
 */
public class RootScope extends Scope {
	
	static final long serialVersionUID = 2;
	
	/** The Lucene Analyzer used. */
	public static final Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT, new HashSet<String> ());
	
	/** The Lucene IndexWriter used. */
	private transient IndexWriter writer;
	
	/** The Lucene IndexReader used. */
	private transient IndexReader reader;
	
	/** The directory in which the index files for this RootScope are stored. */
	private File indexDir;
	
	/**
	 * A unique identifier used to avoid name clashes when storing the index in
	 * the indexes folder.
	 */
	private String id;
	
	private boolean detectHTMLPairs = true;
	
	private String[] textExtensions = Pref.StrArray.TextExtensions.getValue();
	
	private String[] htmlExtensions = Pref.StrArray.HTMLExtensions.getValue();
	
	private String[] exclusionFilters = Pref.Str.ExclusionFilter.getValue().split("\\s*\\$+\\s*"); //$NON-NLS-1$
	
	/** The parse errors that occurred during indexing */
	private List<ParseException> parseExceptions = new ArrayList<ParseException> ();
	
	/** Duration of the parse process in milliseconds. */
	private long parseTime = -1;
	
	/** Whether any errors occurred during indexing */
	private boolean finishedWithErrors = false;
	
	/** Whether this object and its indexes should be deleted on program termination */
	private boolean deleteOnExit = false;

	/**
	 * Creates an instance of this class that represents the given file, which
	 * must be a directory. The created instance has no corresponding index
	 * files yet; it's up to the caller to create these files.
	 */
	public RootScope(File file) {
		super(null, file);
		id = UtilFile.getUniqueID();
		
		/*
		 * Note: If the given directory is a hard drive (e.g. "C:"), then the
		 * resulting name of the index directory will start with an underscore
		 * character.
		 */
		indexDir = new File(Const.INDEX_PARENT_FILE, file.getName() + "_" + id); //$NON-NLS-1$
	}

	/**
	 * Returns the directory in which the index files for this RootScope are
	 * stored.
	 */
	public File getIndexDir() {
		return indexDir;
	}
	
	/**
	 * Updates the index that corresponds to this RootScope.
	 * 
	 * @throws FileNotFoundException
	 *             if the directory represented by this class does not exist
	 *             anymore.
	 */
	void updateIndex() throws FileNotFoundException, IOException {
		if (! file.exists()) {
			setFinishedWithErrors(true);
			throw new FileNotFoundException(file.getAbsolutePath());
		}
		
		// Initialization
		parseTime = System.currentTimeMillis();
		parseExceptions.clear();
		finishedWithErrors = false;
		ParserRegistry.setTextExtensions(textExtensions);
		ParserRegistry.setHTMLExtensions(htmlExtensions);
		
		try {
			// Create index if it doesn't exist yet
			Directory luceneIndexDir = new SimpleFSDirectory(indexDir);
			writer = new IndexWriter(luceneIndexDir, analyzer, MaxFieldLength.UNLIMITED); 
			writer.close();
			
			/*
			 * Clean up ScopeRegistry from missing and modified entries. Cache
			 * the changes for later use when running over the index
			 */
			List<File> removeFromIndex = new ArrayList<File> ();
			cleanupRegistry(this, removeFromIndex);
			
			// Delete missing files from Lucene index
			try {
				reader = IndexReader.open(luceneIndexDir, false);
				
				/*
				 * Do not use 'reader.numDocs()' in the for-loop header; it will
				 * decrease with each deleted document, causing the loop to
				 * terminate prematurely!
				 */
				int numDocs = reader.numDocs();
				for (int i = 0; i < numDocs; i++) {
					if (Thread.currentThread().isInterrupted()) break;
					
					/*
					 * DocFetcher crashes if we try to access a deleted
					 * document. See bug #2881245 and bug #2925127.
					 */
					if (reader.isDeleted(i)) continue;
					
					String pathCandidate = reader.document(i).get(Document.path);
					pathCandidate = new File(pathCandidate).getAbsolutePath();
					File removeFile = null;
					for (File f : removeFromIndex) {
						if (UtilFile.equalPaths(f.getAbsolutePath(), pathCandidate)) {
							reader.deleteDocument(i);
							removeFile = f;
							break;
						}
					}
					if (removeFile != null)
						removeFromIndex.remove(removeFile);
				}
			}
			finally {
				if (reader != null) {
					try {
						reader.close();
						reader = null;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			// Recursively index new files
			if (! Thread.currentThread().isInterrupted()) {
				writer = new IndexWriter(luceneIndexDir, analyzer, MaxFieldLength.UNLIMITED);
				indexNewFiles(this);
				try {
					writer.optimize();
				} catch (ThreadInterruptedException e) {
					// Ignore, see bug report #2971390 and #2953613
				}
			}
		} finally {
			ParserRegistry.resetExtensions();
			if (writer != null) {
				try {
					writer.close();
					writer = null;
				} catch (ThreadInterruptedException e) {
					// Ignore, see bug report #2971390 and #2953613
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		parseTime = System.currentTimeMillis() - parseTime;
	}

	/**
	 * Removes missing and modified entries from the <tt>ScopeRegistry</tt>
	 * and puts them into the provided list.
	 */
	private void cleanupRegistry(Scope scope, List<File> removeFromIndex) {
		if (Thread.currentThread().isInterrupted()) return;
		
		// Remove modified files
		List<Indexable> removeLocal = new ArrayList<Indexable> ();
		for (FileWrapper fileWrapper : scope.subFiles) {
			if (fileWrapper.isModified() || isExcluded(fileWrapper.getFile())) {
				removeFromIndex.add(fileWrapper.file);
				removeLocal.add(fileWrapper);
			}
		}
		scope.subFiles.removeAll(removeLocal);
		
		// List basenames of new HTML folders and
		// new HTML files (separately)
		Set<String> newHTMLDirBasenames = new HashSet<String> ();
		Set<String> newHTMLFileBasenames = new HashSet<String> ();
		getHTMLBasenames(scope, newHTMLDirBasenames, newHTMLFileBasenames);
		
		// Remove modified HTML files
		removeLocal.clear(); // We can reuse the list from the previous step
		for (HTMLPair htmlPair : scope.subHTMLPairs) {
			if (htmlPair.isModified() || isExcluded(htmlPair.getFile()) ||
					newHTMLDirBasenames.contains(UtilFile.getNameNoExt(htmlPair.file))) {
				removeFromIndex.add(htmlPair.file);
				removeLocal.add(htmlPair);
			}
		}
		scope.subHTMLPairs.removeAll(removeLocal);
		
		// Remove deleted scopes and everything underneath them
		removeLocal.clear(); // We can reuse the list from the previous step
		for (Scope subScope : scope.subScopes) {
			if (! subScope.file.exists() ||
					newHTMLFileBasenames.contains(UtilFile.getHTMLDirBasename(subScope.file))) {
				removeLocal.add(subScope);
				insertAllIndexables(subScope, removeFromIndex);
			}
		}
		scope.subScopes.removeAll(removeLocal);
		
		// Recursion
		for (Scope subScope : scope.subScopes)
			cleanupRegistry(subScope, removeFromIndex);
	}
	
	/**
	 * Recursively puts all the children of <tt>scope</tt> into the provided
	 * list (a.k.a. "flattening").
	 */
	private void insertAllIndexables(Scope scope, List<File> list) {
		for (FileWrapper fileWrapper : scope.subFiles)
			list.add(fileWrapper.file);
		for (HTMLPair htmlPair : scope.subHTMLPairs)
			list.add(htmlPair.file);
		for (Scope subScope : scope.subScopes)
			insertAllIndexables(subScope, list);
	}
	
	/**
	 * Puts the basenames of all new HTML files and HTML folders under the
	 * folder represented by <tt>scope</tt> into the two provided lists
	 * (non-recursive).
	 * <p>
	 * A file or folder is 'new' if no representation of it in
	 * <tt>scope</tt> has been created yet.
	 * <p>
	 * The basename of an HTML folder is
	 * its name without the HTML suffix and the separator character (e.g.
	 * "foo_files" -> "foo"). The basename of an HTML file is simply its
	 * filename without the file extension (e.g. "foo.htm" -> "foo").
	 */
	private void getHTMLBasenames(final Scope scope, Set<String> newHTMLDirBasenames, Set<String> newHTMLFileBasenames) {
		FileFilter newHTMLFolderFilter = new FileFilter() {
			public boolean accept(File candidate) {
				if (! candidate.isDirectory()) return false;
				String baseName = UtilFile.getHTMLDirBasename(candidate);
				if (baseName == null) return false;
				if (scope.getRegisteredScope(candidate) != null) return false;
				for (HTMLPair htmlPair : scope.subHTMLPairs)
					if (candidate.equals(htmlPair.getHtmlFolder()))
						return false;
				return true;
			}
		};
		FileFilter newHTMLFileFilter = new FileFilter() {
			public boolean accept(File candidate) {
				return candidate.isFile() &&
				ParserRegistry.isHTMLFile(candidate) &&
				! (scope.isHTMLFileRegistered(candidate));
			}
		};
		File[] newHTMLFolders = UtilFile.listAll(scope.file, newHTMLFolderFilter);
		File[] newHTMLFiles = UtilFile.listAll(scope.file, newHTMLFileFilter);
		if (newHTMLFolders != null)
			for (File newHTMLFolder : newHTMLFolders)
				newHTMLDirBasenames.add(UtilFile.getHTMLDirBasename(newHTMLFolder));
		if (newHTMLFiles != null)
			for (File newHTMLFile : newHTMLFiles)
				newHTMLFileBasenames.add(UtilFile.getNameNoExt(newHTMLFile));
	}

	/**
	 * Recursively indexes all newly inserted documents in the directory
	 * represented by the given <tt>Scope</tt> and remembers them in local
	 * fields. It is expected that the <tt>writer</tt> field has been set.
	 * 
	 * @throws IOException
	 *             if documents could not be written to the index.
	 */
	private void indexNewFiles(Scope scope) throws IOException {
		if (Thread.currentThread().isInterrupted()) return;
		
		// Separate files in the current directory
		List<File> subFiles = new ArrayList<File> ();
		List<File> subDirs = new ArrayList<File> ();
		List<HTMLPair> subHTMLPairs = new ArrayList<HTMLPair> ();
		if (detectHTMLPairs)
			separateChildrenHTMLPaired(scope, subFiles, subDirs, subHTMLPairs);
		else
			separateChildrenHTMLUnpaired(scope, subFiles, subDirs, subHTMLPairs);
		
		/*
		 * The regex-based file exclusion must be applied *after* the HTML pairing.
		 */

		// Process normal files in the current directory
		for (File subFile : subFiles) {
			if (Thread.currentThread().isInterrupted()) return;
			
			// See bug #2927439: DocFetcher fails on temporary MS Word files
			if (subFile.getName().matches("~\\$.*\\.docx?")) //$NON-NLS-1$
				continue;
			
			if (scope.isFileRegistered(subFile)) continue;
			if (isExcluded(subFile)) continue;
			try {
				FileWrapper wrapper = new FileWrapper(scope, subFile);
				try {
					// Both the addDocument(..) and the parse() method can run out of memory!
					writer.addDocument(wrapper.parse().getLuceneDoc());
				}
				catch (OutOfMemoryError e) {
					throw new ParseException(subFile, Msg.out_of_jvm_memory.value());
				}
				catch (StackOverflowError e) {
					throw new ParseException(subFile, Msg.send_file_for_debugging.value());
				}
				scope.subFiles.add(wrapper);
			}
			catch (ParseException e) {
				parseExceptions.add(e);
			}
		}
		
		// Process HTML pairs in the current directory
		for (HTMLPair subHTMLPair : subHTMLPairs) {
			if (Thread.currentThread().isInterrupted()) return;
			if (scope.isHTMLPairRegistered(subHTMLPair)) continue;
			if (isExcluded(subHTMLPair.getFile())) continue;
			try {
				subHTMLPair.setParent(scope);
				try {
					// Both the addDocument(..) and the parse() method can run out of memory!
					writer.addDocument(subHTMLPair.parse().getLuceneDoc());
				}
				catch (OutOfMemoryError e) {
					throw new ParseException(subHTMLPair.file, Msg.out_of_jvm_memory.value());
				}
				catch (StackOverflowError e) {
					throw new ParseException(subHTMLPair.file, Msg.send_file_for_debugging.value());
				}
				scope.subHTMLPairs.add(subHTMLPair);
			} catch (ParseException e) {
				parseExceptions.add(e);
			}
		}

		// Process subdirectories
		for (File subDir : subDirs) {
			if (Thread.currentThread().isInterrupted()) return;
			Scope subScope = scope.getRegisteredScope(subDir);
			if (subScope == null) {
				subScope = new Scope(scope, subDir);
				scope.subScopes.add(subScope);
			}
			indexNewFiles(subScope);
		}
	}

	/**
	 * Puts the folders and parsable files inside the <tt>parent</tt>
	 * directory into the provided lists. Pairs of HTML files and their
	 * associated directories are grouped together. Symbolic links are excluded.
	 * 
	 * @param parent
	 *            The directory whose File contents should be separated.
	 * @param files
	 *            A list for the parsable files in <tt>parent</tt>, excluding
	 *            HTML files.
	 * @param directories
	 *            A list for the directories in <tt>parent</tt>, excluding
	 *            directories that are associated with HTML files.
	 * @param htmlPairs
	 *            A list for pairs of HTML files and their associated folders
	 *            inside <tt>parent</tt>
	 * @throws IOException
	 *             if the <tt>parent</tt> directory could not be read.
	 */
	private void separateChildrenHTMLPaired(
			Scope parent,
			List<File> files,
			List<File> directories,
			List<HTMLPair> htmlPairs) {
		File[] entries = UtilFile.listAll(parent.file);
		if (entries.length == 0) return;

		// Preprocessing: Separation of files without HTML pair detection,
		// excluding symbolic links
		List<File> tmpDirs = new ArrayList<File> (entries.length);
		separateChildrenHTMLUnpaired(parent, files, tmpDirs, htmlPairs);

		// HTML pair detection based on the lists of separated files in the previous step
		for (File dirCandidate : tmpDirs) {
			String dirBasename = UtilFile.getHTMLDirBasename(dirCandidate);

			if (dirBasename == null) { // Directory is not an HTML directory
				directories.add(dirCandidate);
			}
			else {
				// Find the HTML file that corresponds to this HTML directory
				boolean foundUnboundHTMLFile = false;
				for (HTMLPair htmlCandidate : htmlPairs) {
					if (htmlCandidate.getHtmlFolder() == null &&
							UtilFile.getNameNoExt(htmlCandidate.file).equals(dirBasename)) {
						htmlCandidate.setHtmlFolder(dirCandidate);
						foundUnboundHTMLFile = true;
						break;
					}
				}

				// HTML directory does not have a corresponding HTML file,
				// therefore treat it as a regular directory
				if (! foundUnboundHTMLFile)
					directories.add(dirCandidate);
			}
		}
	}
	
	/**
	 * Puts the folders and parsable files inside the <tt>parent</tt>
	 * directory into the provided lists. Pairs of HTML files and their
	 * associated directories are <b>not</b> grouped together. Symbolic links
	 * are excluded.
	 * 
	 * @param parent
	 *            The directory whose File contents should be separated.
	 * @param files
	 *            A list for the parsable files in <tt>parent</tt>, excluding
	 *            HTML files.
	 * @param directories
	 *            A list for the directories in <tt>parent</tt>, excluding
	 *            directories that are associated with HTML files.
	 * @param htmlPairs
	 *            A list for pairs of HTML files and their associated folders
	 *            inside <tt>parent</tt>
	 * @throws IOException
	 *             if the <tt>parent</tt> directory could not be read.
	 */
	private void separateChildrenHTMLUnpaired(
			Scope parent,
			List<File> files,
			List<File> directories,
			List<HTMLPair> htmlPairs) {
		File[] entries = UtilFile.listAll(parent.file);
		if (entries.length == 0) return;

		for (File entry : entries) {
			if (UtilFile.isSymLink(entry)) continue;
			if (entry.isFile()) {
				/*
				 * Check if it's an HTML file first, so that the (possibly user
				 * customized) HTML extensions will get higher priority in case
				 * they contradict the predefined extensions.
				 */
				if (ParserRegistry.isHTMLFile(entry))
					htmlPairs.add(new HTMLPair(parent, entry, null));
				else if (ParserRegistry.getSingleFileParser(entry) != null)
					files.add(entry);
			}
			// Make sure we don't index the index files themselves
			else if (entry.isDirectory() && ! entry.equals(Const.INDEX_PARENT_FILE))
				directories.add(entry);
		}

	}
	
	/**
	 * Returns true if the given file should be excluded from indexing according
	 * to the set file exclusion patterns.
	 */
	private boolean isExcluded(File file) {
		for (String pattern : exclusionFilters)
			if (file.getName().matches(pattern))
				return true;
		return false;
	}

	/**
	 * Fully rebuilds the underlying index.
	 * 
	 * @throws FileNotFoundException
	 *             if the directory represented by this class does not exist
	 *             anymore.
	 */
	void reindex() throws FileNotFoundException, IOException {
		UtilFile.delete(indexDir, false);
		subFiles.clear();
		subScopes.clear();
		subHTMLPairs.clear();
		updateIndex();
	}

	/**
	 * Deletes the underlying index file. This method should only be called from
	 * <tt>ScopeRegistry</tt>, so that the latter stays in sync with the
	 * index files.
	 */
	void deleteIndex() {
		UtilFile.delete(indexDir, true);
		indexDir = null;
		parseExceptions.clear();
	}

	public boolean isDetectHTMLPairs() {
		return detectHTMLPairs;
	}

	public void setDetectHTMLPairs(boolean detectHTMLPairs) {
		this.detectHTMLPairs = detectHTMLPairs;
	}

	public String[] getTextExtensions() {
		return textExtensions;
	}

	public void setTextExtensions(String[] textExtensions) {
		this.textExtensions = textExtensions;
	}

	public String[] getHtmlExtensions() {
		return htmlExtensions;
	}

	public void setHtmlExtensions(String[] htmlExtensions) {
		this.htmlExtensions = htmlExtensions;
	}

	public String[] getExclusionFilters() {
		return exclusionFilters;
	}

	public void setExclusionFilters(String[] exclusionFilters) {
		this.exclusionFilters = exclusionFilters;
	}

	public List<ParseException> getParseExceptions() {
		return parseExceptions;
	}

	public void setParseExceptions(List<ParseException> parseExceptions) {
		this.parseExceptions = parseExceptions;
		if (! parseExceptions.isEmpty())
			finishedWithErrors = true;
	}

	public long getParseTime() {
		return parseTime;
	}

	public void setParseTime(long parseTime) {
		this.parseTime = parseTime;
	}
	
	// Not identical with parseExceptions.size() == 0! Flag can be set externally!
	public boolean isFinishedWithErrors() {
		if (! parseExceptions.isEmpty())
			finishedWithErrors = true;
		return finishedWithErrors;
	}
	
	/**
	 * Sets whether the indexing of the folder represented by the receiver has
	 * been finished with errors. A value of false has no effect if parse
	 * exceptions have already occurred.
	 */
	public void setFinishedWithErrors(boolean finishedWithErrors) {
		this.finishedWithErrors = finishedWithErrors || ! parseExceptions.isEmpty();
	}
	
	public boolean isDeleteOnExit() {
		return deleteOnExit;
	}

	public void setDeleteOnExit(boolean deleteOnExit) {
		this.deleteOnExit = deleteOnExit;
	}

	/**
	 * Returns all documents under the given <tt>Scope</tt>s.
	 */
	public static ResultDocument[] listDocuments(Scope... scopes) {
		// Get the root elements of the given scopes
		Set<RootScope> rootScopeSet = new HashSet<RootScope> ();
		for (Scope scope : scopes)
			rootScopeSet.add(scope.getRootScope());
		RootScope[] rootScopes = rootScopeSet.toArray(new RootScope[rootScopeSet.size()]);
		
		try {
			// Get all documents under the root elements
			IndexReader[] readers = new IndexReader[rootScopes.length];
			for (int i = 0; i < rootScopes.length; i++) {
				Directory dir = new SimpleFSDirectory(rootScopes[i].getIndexDir());
				readers[i] = IndexReader.open(dir);
			}
			MultiReader multiReader = new MultiReader(readers);
			ResultDocument[] rootScopeDocs = new ResultDocument[multiReader.numDocs()];
			for (int i = 0; i < multiReader.numDocs(); i++)
				rootScopeDocs[i] = new ResultDocument(multiReader.document(i), 0, null);
			multiReader.close();
			
			/*
			 * From the documents of the previous step, filter out those that
			 * aren't inside the given scopes, and return the remaining
			 * documents.
			 */
			Set<ResultDocument> scopeDocs = new HashSet<ResultDocument> ();
			for (ResultDocument rootScopeDoc : rootScopeDocs)
				for (Scope scope : scopes)
					if (scope.contains(rootScopeDoc.file)) {
						scopeDocs.add(rootScopeDoc);
						break;
					}
			return scopeDocs.toArray(new ResultDocument[scopeDocs.size()]);
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ResultDocument[0];
	}
	
	/**
	 * Special comparison function: RootScopes where deleteOnExit is true always
	 * go first.
	 */
	public int compareTo(Indexable o) {
		if (! (o instanceof RootScope))
			return super.compareTo(o);
		RootScope oRootScope = (RootScope) o;
		if (deleteOnExit && ! oRootScope.deleteOnExit)
			return -1;
		else if (! deleteOnExit && oRootScope.deleteOnExit)
			return 1;
		return super.compareTo(o);
	}

}
