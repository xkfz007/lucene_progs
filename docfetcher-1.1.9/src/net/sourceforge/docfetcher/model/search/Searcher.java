/*******************************************************************************
 * Copyright (c) 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.model.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.model.Fields;
import net.sourceforge.docfetcher.model.IndexLoadingProblems.CorruptedIndex;
import net.sourceforge.docfetcher.model.IndexRegistry;
import net.sourceforge.docfetcher.model.IndexRegistry.ExistingIndexesHandler;
import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.model.Path;
import net.sourceforge.docfetcher.model.PendingDeletion;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.file.FileFactory;
import net.sourceforge.docfetcher.model.index.outlook.OutlookMailFactory;
import net.sourceforge.docfetcher.model.parse.Parser;
import net.sourceforge.docfetcher.util.CheckedOutOfMemoryError;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.ImmutableCopy;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.NotThreadSafe;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.ThreadSafe;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;
import net.sourceforge.docfetcher.util.collect.AlphanumComparator;
import net.sourceforge.docfetcher.util.collect.LazyList;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.ChainedFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.MultiTermQuery.RewriteMethod;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.PrefixFilter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.TermsFilter;
import org.apache.lucene.search.TopDocs;

import com.google.common.io.Closeables;

/**
 * A search API on top of the index registry. This class is completely
 * thread-safe, so usually only one instance of it is needed for handling
 * concurrent search requests.
 * <p>
 * <b>Important</b>: Instances of this class must be disposed after usage by
 * calling {@link #shutdown()}.
 * 
 * @author Tran Nam Quang
 */
@ThreadSafe
public final class Searcher {
	
	/**
	 * A single page of results.
	 */
	public static final class ResultPage {
		/** The result documents for this page. */
		@ImmutableCopy
		public final List<ResultDocument> resultDocuments;
		
		/** The zero-based index of this page. */
		public final int pageIndex;
		
		/** The total number of pages. */
		public final int pageCount;
		
		/** The total number of result documents across all pages. */
		public final int hitCount;
		
		private ResultPage(	@NotNull List<ResultDocument> resultDocuments,
							int pageIndex,
							int pageCount,
							int hitCount) {
			this.resultDocuments = Util.checkNotNull(resultDocuments);
			this.pageIndex = pageIndex;
			this.pageCount = pageCount;
			this.hitCount = hitCount;
		}
	}
	
	private static final int PAGE_SIZE = ProgramConf.Int.WebInterfacePageSize.get();
	public static final int MAX_RESULTS = ProgramConf.Int.MaxResultsTotal.get();
	
	private final IndexRegistry indexRegistry;
	private final FileFactory fileFactory;
	private final OutlookMailFactory outlookMailFactory;
	private final Event.Listener<LuceneIndex> addedListener;
	
	private final BlockingQueue<List<PendingDeletion>> deletionQueue = new LinkedBlockingQueue<List<PendingDeletion>>(); // guarded by 'this' lock
	private final Thread deletionThread; // guarded by 'this' lock
	
	@NotNull private MultiSearcher luceneSearcher; // guarded by read-write lock
	@NotNull private List<LuceneIndex> indexes; // guarded by read-write lock
	@Nullable private volatile IOException ioException;
	
	private final Lock readLock;
	private final Lock writeLock;

	/**
	 * This method should not be called by clients. Use
	 * {@link IndexRegistry#getSearcher()} instead.
	 * 
	 * @param corruptedIndexes
	 *            A list that will be filled by this constructor with indexes
	 *            that couldn't be loaded.
	 */
	@VisibleForPackageGroup
	public Searcher(@NotNull IndexRegistry indexRegistry,
					@NotNull FileFactory fileFactory,
					@NotNull OutlookMailFactory outlookMailFactory,
					@NotNull final List<CorruptedIndex> corruptedIndexes)
			throws IOException {
		Util.checkNotNull(indexRegistry, fileFactory, outlookMailFactory);
		this.indexRegistry = indexRegistry;
		this.fileFactory = fileFactory;
		this.outlookMailFactory = outlookMailFactory;
		
		readLock = indexRegistry.getReadLock();
		writeLock = indexRegistry.getWriteLock();
		
		// Handler for index additions
		addedListener = new Event.Listener<LuceneIndex>() {
			public void update(LuceneIndex eventData) {
				replaceLuceneSearcher();
			}
		};
		
		/*
		 * This lock could be moved into the indexes handler, but we'll put it
		 * here to avoid releasing and reacquiring it.
		 */
		writeLock.lock();
		try {
			indexRegistry.addListeners(new ExistingIndexesHandler() {
				// Handle existing indexes
				public void handleExistingIndexes(List<LuceneIndex> indexes) {
					try {
						corruptedIndexes.addAll(setLuceneSearcher(indexes));
					}
					catch (IOException e) {
						ioException = e;
					}
				}
			}, addedListener, null); // removedListener is null, see deletion thread below
		}
		finally {
			writeLock.unlock();
		}
		
		if (ioException != null)
			throw ioException;
		
		// Handler for index removals
		deletionThread = new Thread(Searcher.class.getName() + " (Approve pending deletions)") {
			public void run() {
				while (true) {
					try {
						List<PendingDeletion> deletions = deletionQueue.take();
						replaceLuceneSearcher();
						for (PendingDeletion deletion : deletions)
							deletion.setApprovedBySearcher();
					}
					catch (InterruptedException e) {
						break;
					}
				}
			}
		};
		deletionThread.start();
	}
	
	/**
	 * Updates the cached indexes and replaces the current Lucene searcher with
	 * a new one.
	 */
	@ThreadSafe
	@VisibleForPackageGroup
	public void replaceLuceneSearcher() {
		writeLock.lock();
		try {
			Closeables.close(luceneSearcher, false);
			setLuceneSearcher(indexRegistry.getIndexes());
		}
		catch (IOException e) {
			ioException = e; // Will be thrown later
		}
		finally {
			writeLock.unlock();
		}
	}
	
	// Caller must close returned searcher
	@NotNull
	@NotThreadSafe
	private List<CorruptedIndex> setLuceneSearcher(@NotNull List<LuceneIndex> indexes)
			throws IOException {
		this.indexes = Util.checkNotNull(indexes);
        Searchable[] searchables = new Searchable[indexes.size()];
        LazyList<CorruptedIndex> corrupted = new LazyList<CorruptedIndex>();
		for (int i = 0; i < indexes.size(); i++) {
			LuceneIndex index = indexes.get(i);
            try {
                searchables[i] = new IndexSearcher(index.getLuceneDir());
            }
            catch (IOException e) {
            	Util.printErr(e);
                searchables[i] = new DummySearchable();
                corrupted.add(new CorruptedIndex(index, e));
            }
        }
        luceneSearcher = new MultiSearcher(searchables);
        return corrupted;
	}
	
	@ImmutableCopy
	@NotNull
	@ThreadSafe
	public List<ResultDocument> search(@NotNull String queryString)
			throws SearchException, CheckedOutOfMemoryError {
		/*
		 * Note: For the desktop interface, we'll always search in all available
		 * indexes, even those which are unchecked on the filter panel. This
		 * allows the user to re-check the unchecked indexes and see previously
		 * hidden results without starting another search.
		 */
		
		// Create Lucene query
		QueryWrapper queryWrapper = createQuery(queryString);
		Query query = queryWrapper.query;
		boolean isPhraseQuery = queryWrapper.isPhraseQuery;
		
		/*
		 * Notes regarding the following code:
		 * 
		 * 1) Lucene will throw an IOException if the user deletes one or more
		 * indexes while a search is running over the affected indexes. This can
		 * happen when two DocFetcher instances are running.
		 * 
		 * 2) All the information needed for displaying the results must be
		 * loaded and returned immediately rather than lazily, because after the
		 * search the user might delete one or more indexes. This also means the
		 * result documents must not access the indexes later on.
		 */

		readLock.lock();
		try {
			checkIndexesExist();
			
			// Perform search; might throw OutOfMemoryError
			ScoreDoc[] scoreDocs = luceneSearcher.search(query, MAX_RESULTS).scoreDocs;

			// Create result documents
			ResultDocument[] results = new ResultDocument[scoreDocs.length];
			for (int i = 0; i < scoreDocs.length; i++) {
				Document doc = luceneSearcher.doc(scoreDocs[i].doc);
				float score = scoreDocs[i].score;
				LuceneIndex index = indexes.get(luceneSearcher.subSearcher(i));
				IndexingConfig config = index.getConfig();
				results[i] = new ResultDocument(
					doc, score, query, isPhraseQuery, config, fileFactory,
					outlookMailFactory);
			}
			return Arrays.asList(results);
		}
		catch (IllegalArgumentException e) {
			throw wrapEmptyIndexException(e);
		}
		catch (IOException e) {
			throw new SearchException(e.getMessage()); // TODO i18n
		}
		catch (OutOfMemoryError e) {
			throw new CheckedOutOfMemoryError(e);
		}
		finally {
			readLock.unlock();
		}
	}
	
	@NotNull
	private static SearchException wrapEmptyIndexException(@NotNull IllegalArgumentException e)
			throws SearchException {
		/*
		 * Workaround for bug #390: Lucene 3.5 throws this exception if the
		 * indexes are empty, i.e. if no documents have been indexed so far.
		 * This happens if the user indexes an empty folder hierarchy with no
		 * files in it. Apparently, this problem has been fixed in Lucene 4.0,
		 * so when the Lucene jar is upgraded to 4.0, this workaround may be
		 * removed.
		 */
		if (e.getMessage().contains("numHits must be > 0"))
			return new SearchException("No files were indexed."); // not internationalized
		else
			throw e;
	}
	
	@ImmutableCopy
	@NotNull
	@ThreadSafe
	public List<ResultDocument> list(@NotNull Set<String> uids)
			throws SearchException, CheckedOutOfMemoryError {
		// Construct a filter that only matches documents with the given UIDs
		TermsFilter uidFilter = new TermsFilter();
		String fieldName = Fields.UID.key();
		for (String uid : uids)
			uidFilter.addTerm(new Term(fieldName, uid));
		
		Query query = new MatchAllDocsQuery();
		
		readLock.lock();
		try {
			checkIndexesExist();
			
			// Perform search; might throw OutOfMemoryError
			ScoreDoc[] scoreDocs = luceneSearcher.search(query, uidFilter, MAX_RESULTS).scoreDocs;
			
			// Create result documents
			ResultDocument[] results = new ResultDocument[scoreDocs.length];
			for (int i = 0; i < results.length; i++) {
				Document doc = luceneSearcher.doc(scoreDocs[i].doc);
				float score = scoreDocs[i].score;
				LuceneIndex index = indexes.get(luceneSearcher.subSearcher(i));
				IndexingConfig config = index.getConfig();
				results[i] = new ResultDocument(
					doc, score, query, true, config, fileFactory,
					outlookMailFactory);
			}
			
			// Sort results by title
			Arrays.sort(results, new Comparator<ResultDocument>() {
				public int compare(ResultDocument o1, ResultDocument o2) {
					return AlphanumComparator.ignoreCaseInstance.compare(
						o1.getTitle(), o2.getTitle());
				}
			});
			
			return Arrays.asList(results);
		}
		catch (IllegalArgumentException e) {
			throw wrapEmptyIndexException(e);
		}
		catch (IOException e) {
			throw new SearchException(e.getMessage()); // TODO i18n
		}
		catch (OutOfMemoryError e) {
			throw new CheckedOutOfMemoryError(e);
		}
		finally {
			readLock.unlock();
		}
	}
	
	/**
	 * For the given query, returns the requested page of results. This method
	 * should not be called anymore after {@link #shutdown()} has been called,
	 * otherwise an IOException will be thrown.
	 */
	@NotNull
	@ThreadSafe
	public ResultPage search(@NotNull WebQuery webQuery)
			throws IOException, SearchException, CheckedOutOfMemoryError {
		Util.checkNotNull(webQuery);
		
		if (ioException != null)
			throw ioException;
		
		List<Filter> filters = new ArrayList<Filter>(3);
		
		// Add size filter to filter chain
		if (webQuery.minSize != null || webQuery.maxSize != null) {
			filters.add(NumericRangeFilter.newLongRange(
				Fields.SIZE.key(), webQuery.minSize, webQuery.maxSize, true,
				true));
		}
		
		// Add type filter to filter chain
		if (webQuery.parsers != null) {
			TermsFilter typeFilter = new TermsFilter();
			String fieldName = Fields.PARSER.key();
			typeFilter.addTerm(new Term(fieldName, Fields.EMAIL_PARSER));
			for (Parser parser : webQuery.parsers) {
				String parserName = parser.getClass().getSimpleName();
				typeFilter.addTerm(new Term(fieldName, parserName));
			}
			filters.add(typeFilter);
		}
		
		// Add location filter to filter chain
		if (webQuery.indexes != null) {
			Filter[] indexFilters = new Filter[webQuery.indexes.size()];
			int i = 0;
			for (LuceneIndex index : webQuery.indexes) {
				Path path = index.getRootFolder().getPath();
				String uid = index.getDocumentType().createUniqueId(path);
				Term prefix = new Term(Fields.UID.key(), uid + "/");
				indexFilters[i++] = new PrefixFilter(prefix);
			}
			filters.add(new ChainedFilter(indexFilters, ChainedFilter.OR));
		}
		
		// Construct filter chain
		Filter filter = filters.size() == 0 ? null : new ChainedFilter(
			filters.toArray(new Filter[filters.size()]), ChainedFilter.AND);
		
		// Create query
		QueryWrapper queryWrapper = createQuery(webQuery.query);
		Query query = queryWrapper.query;
		boolean isPhraseQuery = queryWrapper.isPhraseQuery;
		
		readLock.lock();
		try {
			checkIndexesExist();
			
			// Perform search; might throw OutOfMemoryError
			int maxResults = (webQuery.pageIndex + 1) * PAGE_SIZE;
			TopDocs topDocs = luceneSearcher.search(query, filter, maxResults);
			ScoreDoc[] scoreDocs = topDocs.scoreDocs;
			
			// Compute start and end indices of returned page
			int start;
			int end = scoreDocs.length;
			if (end <= PAGE_SIZE) {
				start = 0;
			}
			else {
				int r = end % PAGE_SIZE;
				start = end - (r == 0 ? PAGE_SIZE : r);
			}

			// Create and fill list of result documents to return
			ResultDocument[] results = new ResultDocument[end - start];
			for (int i = start; i < end; i++) {
				Document doc = luceneSearcher.doc(scoreDocs[i].doc);
				float score = scoreDocs[i].score;
				LuceneIndex index = indexes.get(luceneSearcher.subSearcher(i));
				IndexingConfig config = index.getConfig();
				results[i - start] = new ResultDocument(
					doc, score, query, isPhraseQuery, config, fileFactory,
					outlookMailFactory);
			}
			
			int hitCount = topDocs.totalHits;
			int newPageIndex = start / PAGE_SIZE;
			int pageCount = (int) Math.ceil((float) hitCount / PAGE_SIZE);
			
			return new ResultPage(
				Arrays.asList(results), newPageIndex, pageCount, hitCount);
		}
		catch (IllegalArgumentException e) {
			throw wrapEmptyIndexException(e);
		}
		catch (OutOfMemoryError e) {
			throw new CheckedOutOfMemoryError(e);
		}
		finally {
			readLock.unlock();
		}
	}
	
	@NotNull
	@ThreadSafe
	private static QueryWrapper createQuery(@NotNull String queryString)
			throws SearchException {
		PhraseDetectingQueryParser queryParser = new PhraseDetectingQueryParser(
			IndexRegistry.LUCENE_VERSION, Fields.CONTENT.key(), IndexRegistry.analyzer);
		queryParser.setAllowLeadingWildcard(true);
		RewriteMethod rewriteMethod = MultiTermQuery.SCORING_BOOLEAN_QUERY_REWRITE;
		queryParser.setMultiTermRewriteMethod(rewriteMethod);
		if (!SettingsConf.Bool.UseOrOperator.get())
			queryParser.setDefaultOperator(QueryParser.AND_OPERATOR);
		
		try {
			Query query = queryParser.parse(queryString);
			boolean isPhraseQuery = queryParser.isPhraseQuery();
			return new QueryWrapper(query, isPhraseQuery);
		}
		catch (IllegalArgumentException e) {
			/*
			 * This happens for example when you enter a fuzzy search with
			 * similarity >= 1, e.g. "fuzzy~1".
			 */
			String msg = Msg.invalid_query.get() + "\n\n" + e.getMessage();
			throw new SearchException(msg);
		}
		catch (ParseException e) {
			String msg = Msg.invalid_query.get() + "\n\n" + e.getMessage();
			throw new SearchException(msg);
		}
	}
	
	// Checks that all indexes still exist
	@NotNull
	@NotThreadSafe
	private void checkIndexesExist() throws SearchException {
		if (indexes.isEmpty())
			throw new SearchException("Nothing to search in: No indexes have been created yet."); // TODO i18n
		for (LuceneIndex index : indexes) {
			File indexDir = index.getIndexDirPath().getCanonicalFile();
			if (indexDir != null && !indexDir.isDirectory()) {
				String msg = "Folders not found:"; // TODO i18n folders_not_found
				msg += "\n" + indexDir;
				throw new SearchException(msg);
			}
		}
	}
	
	// Given deletions should not be in the registry anymore, since the receiver
	// will retrieve a fresh set of indexes from the registry before approval
	@ThreadSafe
	@VisibleForPackageGroup
	public void approveDeletions(@NotNull List<PendingDeletion> deletions) {
		Util.checkNotNull(deletions);
		if (deletions.isEmpty())
			return;
		
		/*
		 * If the deletion thread is not available anymore, approve of deletions
		 * immediately. - Otherwise the given deletion objects would just hang
		 * around in the queue until program shutdown and never receive
		 * approval, thus the associated indexes would never get deleted.
		 */
		synchronized (this) {
			if (deletionThread.isInterrupted()) {
				for (PendingDeletion pendingDeletion : deletions)
					pendingDeletion.setApprovedBySearcher();
			}
			else {
				deletionQueue.add(deletions);
			}
		}
	}
	
	/**
	 * Disposes of the receiver. The caller should make sure that no more search
	 * requests are submitted to the receiver after this method is called.
	 */
	@ThreadSafe
	public void shutdown() {
		if (ioException != null)
			Util.printErr(ioException);
		
		writeLock.lock();
		try {
			indexRegistry.removeListeners(addedListener, null);
			Closeables.closeQuietly(luceneSearcher);
		}
		finally {
			writeLock.unlock();
		}
		
		/*
		 * This should be done after closing the Lucene searcher in order to
		 * ensure that no indexes will be deleted outside the deletion queue
		 * while the Lucene searcher is still open.
		 */
		synchronized (this) {
			deletionThread.interrupt();
		}
	}
	
	private static final class QueryWrapper {
		public final Query query;
		public final boolean isPhraseQuery;
		
		private QueryWrapper(@NotNull Query query, boolean isPhraseQuery) {
			this.query = Util.checkNotNull(query);
			this.isPhraseQuery = isPhraseQuery;
		}
	}

}
