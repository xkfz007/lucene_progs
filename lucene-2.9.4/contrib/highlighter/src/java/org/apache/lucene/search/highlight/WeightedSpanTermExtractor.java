package org.apache.lucene.search.highlight;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.CachingTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.FieldMaskingSpanQuery;
import org.apache.lucene.search.spans.SpanFirstQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.StringHelper;

/**
 * Class used to extract {@link WeightedSpanTerm}s from a {@link Query} based on whether 
 * {@link Term}s from the {@link Query} are contained in a supplied {@link TokenStream}.
 */
public class WeightedSpanTermExtractor {

  private String fieldName;
  private TokenStream tokenStream;
  private Map readers = new HashMap(10); // Map<String, IndexReader>
  private String defaultField;
  private boolean expandMultiTermQuery;
  private boolean cachedTokenStream;
  private boolean wrapToCaching = true;

  public WeightedSpanTermExtractor() {
  }

  public WeightedSpanTermExtractor(String defaultField) {
    if (defaultField != null) {
      this.defaultField = StringHelper.intern(defaultField);
    }
  }

  private void closeReaders() {
    Collection readerSet = readers.values();
    Iterator it = readerSet.iterator();

    while (it.hasNext()) {
      IndexReader reader = (IndexReader) it.next();
      try {
        reader.close();
      } catch (IOException e) {
        // alert?
      }
    }
  }

  /**
   * Fills a <code>Map</code> with <@link WeightedSpanTerm>s using the terms from the supplied <code>Query</code>.
   * 
   * @param query
   *          Query to extract Terms from
   * @param terms
   *          Map to place created WeightedSpanTerms in
   * @throws IOException
   */
  private void extract(Query query, Map terms) throws IOException {
    if (query instanceof BooleanQuery) {
      BooleanClause[] queryClauses = ((BooleanQuery) query).getClauses();

      for (int i = 0; i < queryClauses.length; i++) {
        if (!queryClauses[i].isProhibited()) {
          extract(queryClauses[i].getQuery(), terms);
        }
      }
    } else if (query instanceof PhraseQuery) {
      PhraseQuery phraseQuery = ((PhraseQuery) query);
      Term[] phraseQueryTerms = phraseQuery.getTerms();
      SpanQuery[] clauses = new SpanQuery[phraseQueryTerms.length];
      for (int i = 0; i < phraseQueryTerms.length; i++) {
        clauses[i] = new SpanTermQuery(phraseQueryTerms[i]);
      }
      int slop = phraseQuery.getSlop();
      int[] positions = phraseQuery.getPositions();
      // add largest position increment to slop
      if (positions.length > 0) {
        int lastPos = positions[0];
        int largestInc = 0;
        int sz = positions.length;
        for (int i = 1; i < sz; i++) {
          int pos = positions[i];
          int inc = pos - lastPos;
          if (inc > largestInc) {
            largestInc = inc;
          }
          lastPos = pos;
        }
        if(largestInc > 1) {
          slop += largestInc;
        }
      }

      boolean inorder = false;

      if (slop == 0) {
        inorder = true;
      }

      SpanNearQuery sp = new SpanNearQuery(clauses, slop, inorder);
      sp.setBoost(query.getBoost());
      extractWeightedSpanTerms(terms, sp);
    } else if (query instanceof TermQuery) {
      extractWeightedTerms(terms, query);
    } else if (query instanceof SpanQuery) {
      extractWeightedSpanTerms(terms, (SpanQuery) query);
    } else if (query instanceof FilteredQuery) {
      extract(((FilteredQuery) query).getQuery(), terms);
    } else if (query instanceof DisjunctionMaxQuery) {
      for (Iterator iterator = ((DisjunctionMaxQuery) query).iterator(); iterator.hasNext();) {
        extract((Query) iterator.next(), terms);
      }
    } else if (query instanceof MultiTermQuery && expandMultiTermQuery) {
      MultiTermQuery mtq = ((MultiTermQuery)query);
      if(mtq.getRewriteMethod() != MultiTermQuery.SCORING_BOOLEAN_QUERY_REWRITE) {
        mtq = copyMultiTermQuery(mtq);
        mtq.setRewriteMethod(MultiTermQuery.SCORING_BOOLEAN_QUERY_REWRITE);
        query = mtq;
      }
      String field = null;
      if(mtq instanceof TermRangeQuery) {
        field = ((TermRangeQuery)mtq).getField();
      } else {
        Term term = mtq.getTerm();
        if(term != null) {
          field = term.field();
        }
      }
      if(field != null) {
        IndexReader ir = getReaderForField(field);
        extract(query.rewrite(ir), terms);
      }
    } else if (query instanceof RangeQuery) {
      String field = ((RangeQuery)query).getField();
      IndexReader ir = getReaderForField(field);
      extract(query.rewrite(ir), terms);
    } else if (query instanceof MultiPhraseQuery) {
      final MultiPhraseQuery mpq = (MultiPhraseQuery) query;
      final List termArrays = mpq.getTermArrays();
      final int[] positions = mpq.getPositions();
      if (positions.length > 0) {

        int maxPosition = positions[positions.length - 1];
        for (int i = 0; i < positions.length - 1; ++i) {
          if (positions[i] > maxPosition) {
            maxPosition = positions[i];
          }
        }

        final List[] disjunctLists = new List[maxPosition + 1];
        int distinctPositions = 0;

        for (int i = 0; i < termArrays.size(); ++i) {
          final Term[] termArray = (Term[]) termArrays.get(i);
          List disjuncts = disjunctLists[positions[i]];
          if (disjuncts == null) {
            disjuncts = (disjunctLists[positions[i]] = new ArrayList(termArray.length));
            ++distinctPositions;
          }
          for (int j = 0; j < termArray.length; ++j) {
            disjuncts.add(new SpanTermQuery(termArray[j]));
          }
        }

        int positionGaps = 0;
        int position = 0;
        final SpanQuery[] clauses = new SpanQuery[distinctPositions];
        for (int i = 0; i < disjunctLists.length; ++i) {
          List disjuncts = disjunctLists[i];
          if (disjuncts != null) {
            clauses[position++] = new SpanOrQuery((SpanQuery[]) disjuncts
                .toArray(new SpanQuery[disjuncts.size()]));
          } else {
            ++positionGaps;
          }
        }

        final int slop = mpq.getSlop();
        final boolean inorder = (slop == 0);

        SpanNearQuery sp = new SpanNearQuery(clauses, slop + positionGaps, inorder);
        sp.setBoost(query.getBoost());
        extractWeightedSpanTerms(terms, sp);
      }
    }
  }

  /**
   * Fills a <code>Map</code> with <@link WeightedSpanTerm>s using the terms from the supplied <code>SpanQuery</code>.
   * 
   * @param terms
   *          Map to place created WeightedSpanTerms in
   * @param spanQuery
   *          SpanQuery to extract Terms from
   * @throws IOException
   */
  private void extractWeightedSpanTerms(Map terms, SpanQuery spanQuery) throws IOException {
    Set fieldNames;
    
    if (fieldName == null) {
      fieldNames = new HashSet();
      collectSpanQueryFields(spanQuery, fieldNames);
    } else {
      fieldNames = new HashSet(1);
      fieldNames.add(fieldName);
    }
    // To support the use of the default field name
    if (defaultField != null) {
      fieldNames.add(defaultField);
    }
    

    Map queries = new HashMap();
    
    Set nonWeightedTerms = new HashSet();
    final boolean mustRewriteQuery = mustRewriteQuery(spanQuery);
    if (mustRewriteQuery) {
      Iterator it = fieldNames.iterator();
      while(it.hasNext()) {
        String field = (String) it.next();
        final SpanQuery rewrittenQuery = (SpanQuery) spanQuery.rewrite(getReaderForField(field));
        queries.put(field, rewrittenQuery);
        rewrittenQuery.extractTerms(nonWeightedTerms);
      }
    } else {
      spanQuery.extractTerms(nonWeightedTerms);
    }

    Iterator it = fieldNames.iterator();
    List spanPositions = new ArrayList();

    while (it.hasNext()) {
      String field = (String) it.next();

      IndexReader reader = getReaderForField(field);
      final Spans spans;
      if (mustRewriteQuery) {
        spans = ((SpanQuery) queries.get(field)).getSpans(reader);
      } else {
        spans = spanQuery.getSpans(reader);
      }

      // collect span positions
      while (spans.next()) {
        spanPositions.add(new PositionSpan(spans.start(), spans.end() - 1));
      }
      
    }

    if (spanPositions.size() == 0) {
      // no spans found
      return;
    }

    for (Iterator iter = nonWeightedTerms.iterator(); iter.hasNext();) {
      Term queryTerm = (Term) iter.next();

      if (fieldNameComparator(queryTerm.field())) {
        WeightedSpanTerm weightedSpanTerm = (WeightedSpanTerm) terms.get(queryTerm.text());

        if (weightedSpanTerm == null) {
          weightedSpanTerm = new WeightedSpanTerm(spanQuery.getBoost(), queryTerm.text());
          weightedSpanTerm.addPositionSpans(spanPositions);
          weightedSpanTerm.positionSensitive = true;
          terms.put(queryTerm.text(), weightedSpanTerm);
        } else {
          if (spanPositions.size() > 0) {
            weightedSpanTerm.addPositionSpans(spanPositions);
          }
        }
      }
    }
  }

  /**
   * Fills a <code>Map</code> with <@link WeightedSpanTerm>s using the terms from the supplied <code>Query</code>.
   * 
   * @param terms
   *          Map to place created WeightedSpanTerms in
   * @param query
   *          Query to extract Terms from
   * @throws IOException
   */
  private void extractWeightedTerms(Map terms, Query query) throws IOException {
    Set nonWeightedTerms = new HashSet();
    query.extractTerms(nonWeightedTerms);

    for (Iterator iter = nonWeightedTerms.iterator(); iter.hasNext();) {
      Term queryTerm = (Term) iter.next();

      if (fieldNameComparator(queryTerm.field())) {
        WeightedSpanTerm weightedSpanTerm = new WeightedSpanTerm(query.getBoost(), queryTerm.text());
        terms.put(queryTerm.text(), weightedSpanTerm);
      }
    }
  }

  /**
   * Necessary to implement matches for queries against <code>defaultField</code>
   */
  private boolean fieldNameComparator(String fieldNameToCheck) {
    boolean rv = fieldName == null || fieldNameToCheck == fieldName
        || fieldNameToCheck == defaultField;
    return rv;
  }

  private IndexReader getReaderForField(String field) throws IOException {
    if(wrapToCaching && !cachedTokenStream && !(tokenStream instanceof CachingTokenFilter)) {
      tokenStream = new CachingTokenFilter(tokenStream);
      cachedTokenStream = true;
    }
    IndexReader reader = (IndexReader) readers.get(field);
    if (reader == null) {
      MemoryIndex indexer = new MemoryIndex();
      indexer.addField(field, tokenStream);
      tokenStream.reset();
      IndexSearcher searcher = indexer.createSearcher();
      reader = searcher.getIndexReader();
      readers.put(field, reader);
    }

    return reader;
  }

  /**
   * Creates a Map of <code>WeightedSpanTerms</code> from the given <code>Query</code> and <code>TokenStream</code>.
   * 
   * <p>
   * 
   * @param query
   *          that caused hit
   * @param tokenStream
   *          of text to be highlighted
   * @return Map containing WeightedSpanTerms
   * @throws IOException
   */
  public Map getWeightedSpanTerms(Query query, TokenStream tokenStream)
      throws IOException {
    return getWeightedSpanTerms(query, tokenStream, null);
  }

  /**
   * Creates a Map of <code>WeightedSpanTerms</code> from the given <code>Query</code> and <code>TokenStream</code>.
   * 
   * <p>
   * 
   * @param query
   *          that caused hit
   * @param tokenStream
   *          of text to be highlighted
   * @param fieldName
   *          restricts Term's used based on field name
   * @return Map containing WeightedSpanTerms
   * @throws IOException
   */
  public Map getWeightedSpanTerms(Query query, TokenStream tokenStream,
      String fieldName) throws IOException {
    if (fieldName != null) {
      this.fieldName = StringHelper.intern(fieldName);
    } else {
      this.fieldName = null;
    }

    Map terms = new PositionCheckingMap();
    this.tokenStream = tokenStream;
    try {
      extract(query, terms);
    } finally {
      closeReaders();
    }

    return terms;
  }

  /**
   * Creates a Map of <code>WeightedSpanTerms</code> from the given <code>Query</code> and <code>TokenStream</code>. Uses a supplied
   * <code>IndexReader</code> to properly weight terms (for gradient highlighting).
   * 
   * <p>
   * 
   * @param query
   *          that caused hit
   * @param tokenStream
   *          of text to be highlighted
   * @param fieldName
   *          restricts Term's used based on field name
   * @param reader
   *          to use for scoring
   * @return Map of WeightedSpanTerms with quasi tf/idf scores
   * @throws IOException
   */
  public Map getWeightedSpanTermsWithScores(Query query, TokenStream tokenStream, String fieldName,
      IndexReader reader) throws IOException {
    if (fieldName != null) {
      this.fieldName = StringHelper.intern(fieldName);
    } else {
      this.fieldName = null;
    }
    this.tokenStream = tokenStream;

    Map terms = new PositionCheckingMap();
    extract(query, terms);

    int totalNumDocs = reader.numDocs();
    Set weightedTerms = terms.keySet();
    Iterator it = weightedTerms.iterator();

    try {
      while (it.hasNext()) {
        WeightedSpanTerm weightedSpanTerm = (WeightedSpanTerm) terms.get(it.next());
        int docFreq = reader.docFreq(new Term(fieldName, weightedSpanTerm.term));
        // docFreq counts deletes
        if(totalNumDocs < docFreq) {
          docFreq = totalNumDocs;
        }
        // IDF algorithm taken from DefaultSimilarity class
        float idf = (float) (Math.log((float) totalNumDocs / (double) (docFreq + 1)) + 1.0);
        weightedSpanTerm.weight *= idf;
      }
    } finally {

      closeReaders();
    }

    return terms;
  }
  

  private void collectSpanQueryFields(SpanQuery spanQuery, Set fieldNames) {
    if (spanQuery instanceof FieldMaskingSpanQuery) {
      collectSpanQueryFields(((FieldMaskingSpanQuery)spanQuery).getMaskedQuery(), fieldNames);
    } else if (spanQuery instanceof SpanFirstQuery) {
      collectSpanQueryFields(((SpanFirstQuery)spanQuery).getMatch(), fieldNames);
    } else if (spanQuery instanceof SpanNearQuery) {
      SpanQuery[] clauses = ((SpanNearQuery)spanQuery).getClauses();
      for(int i = 0; i < clauses.length; i++) {
        collectSpanQueryFields(clauses[i], fieldNames);
      }
    } else if (spanQuery instanceof SpanNotQuery) {
      collectSpanQueryFields(((SpanNotQuery)spanQuery).getInclude(), fieldNames);
    } else if (spanQuery instanceof SpanOrQuery) {
      SpanQuery[] clauses = ((SpanOrQuery)spanQuery).getClauses();
      for(int i = 0; i < clauses.length; i++) {
        collectSpanQueryFields(clauses[i], fieldNames);
      }
    } else {
      fieldNames.add(spanQuery.getField());
    }
  }
  
  private boolean mustRewriteQuery(SpanQuery spanQuery) {
    if (!expandMultiTermQuery) {
      return false; // Will throw UnsupportedOperationException in case of a SpanRegexQuery.
    } else if (spanQuery instanceof FieldMaskingSpanQuery) {
      return mustRewriteQuery(((FieldMaskingSpanQuery)spanQuery).getMaskedQuery());
    } else if (spanQuery instanceof SpanFirstQuery) {
      return mustRewriteQuery(((SpanFirstQuery)spanQuery).getMatch());
    } else if (spanQuery instanceof SpanNearQuery) {
      SpanQuery[] clauses = ((SpanNearQuery)spanQuery).getClauses();
      for(int i = 0; i < clauses.length; i++) {
        if (mustRewriteQuery(clauses[i])) {
          return true;
        }
      }
      return false; 
    } else if (spanQuery instanceof SpanNotQuery) {
      SpanNotQuery spanNotQuery = (SpanNotQuery)spanQuery;
      return mustRewriteQuery(spanNotQuery.getInclude()) || mustRewriteQuery(spanNotQuery.getExclude());
    } else if (spanQuery instanceof SpanOrQuery) {
      SpanQuery[] clauses = ((SpanOrQuery)spanQuery).getClauses();
      for(int i = 0; i < clauses.length; i++) {  
        if (mustRewriteQuery(clauses[i])) {
          return true;
        }
      }
      return false; 
    } else if (spanQuery instanceof SpanTermQuery) {
      return false;
    } else {
      return true;
    }
  }
  
  
  /**
   * This class makes sure that if both position sensitive and insensitive
   * versions of the same term are added, the position insensitive one wins.
   */
  static private class PositionCheckingMap extends HashMap {

    public void putAll(Map m) {
      Iterator it = m.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry entry = (java.util.Map.Entry) it.next();
        this.put(entry.getKey(), entry.getValue());
      }
    }

    public Object put(Object key, Object value) {
      Object prev = super.put(key, value);
      if (prev == null) return prev;
      WeightedSpanTerm prevTerm = (WeightedSpanTerm)prev;
      WeightedSpanTerm newTerm = (WeightedSpanTerm)value;
      if (!prevTerm.positionSensitive) {
        newTerm.positionSensitive = false;
      }
      return prev;
    }
    
  }
  
  private MultiTermQuery copyMultiTermQuery(MultiTermQuery query) {
    if(query instanceof TermRangeQuery) {
      TermRangeQuery q = (TermRangeQuery)query;
      q.setBoost(query.getBoost());
      return new TermRangeQuery(q.getField(), q.getLowerTerm(), q.getUpperTerm(), q.includesLower(), q.includesUpper());
    } else if(query instanceof WildcardQuery) {
      MultiTermQuery q = new WildcardQuery(query.getTerm());
      q.setBoost(query.getBoost());
      return q;
    } else if(query instanceof PrefixQuery) {
      MultiTermQuery q = new PrefixQuery(query.getTerm());
      q.setBoost(q.getBoost());
      return q;
    } else if(query instanceof FuzzyQuery) {
      FuzzyQuery q = (FuzzyQuery)query;
      q.setBoost(q.getBoost());
      return new FuzzyQuery(q.getTerm(), q.getMinSimilarity(), q.getPrefixLength());
    }
    
    return query;
  }
  
  
  public boolean getExpandMultiTermQuery() {
    return expandMultiTermQuery;
  }

  public void setExpandMultiTermQuery(boolean expandMultiTermQuery) {
    this.expandMultiTermQuery = expandMultiTermQuery;
  }
  
  public boolean isCachedTokenStream() {
    return cachedTokenStream;
  }
  
  public TokenStream getTokenStream() {
    return tokenStream;
  }
  
  /**
   * By default, {@link TokenStream}s that are not of the type
   * {@link CachingTokenFilter} are wrapped in a {@link CachingTokenFilter} to
   * ensure an efficient reset - if you are already using a different caching
   * {@link TokenStream} impl and you don't want it to be wrapped, set this to
   * false.
   * 
   * @param wrap
   */
  public void setWrapIfNotCachingTokenFilter(boolean wrap) {
    this.wrapToCaching = wrap;
  }
}
