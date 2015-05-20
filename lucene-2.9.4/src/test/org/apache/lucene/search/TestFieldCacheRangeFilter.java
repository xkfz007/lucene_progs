package org.apache.lucene.search;

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
import java.text.Collator;
import java.util.Locale;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.store.RAMDirectory;

/**
 * A basic 'positive' Unit test class for the FieldCacheRangeFilter class.
 *
 * <p>
 * NOTE: at the moment, this class only tests for 'positive' results,
 * it does not verify the results to ensure there are no 'false positives',
 * nor does it adequately test 'negative' results.  It also does not test
 * that garbage in results in an Exception.
 */
public class TestFieldCacheRangeFilter extends BaseTestRangeFilter {

  public TestFieldCacheRangeFilter(String name) {
    super(name);
  }
  public TestFieldCacheRangeFilter() {
    super();
  }

  public void testRangeFilterId() throws IOException {

    IndexReader reader = IndexReader.open(signedIndex.index);
    IndexSearcher search = new IndexSearcher(reader);

    int medId = ((maxId - minId) / 2);
        
    String minIP = pad(minId);
    String maxIP = pad(maxId);
    String medIP = pad(medId);
    
    int numDocs = reader.numDocs();
        
    assertEquals("num of docs", numDocs, 1+ maxId - minId);
        
    ScoreDoc[] result;
    Query q = new TermQuery(new Term("body","body"));

    // test id, bounded on both ends
    FieldCacheRangeFilter fcrf;
    result = search.search(q,fcrf = FieldCacheRangeFilter.newStringRange("id",minIP,maxIP,T,T), numDocs).scoreDocs;
    assertTrue(fcrf.getDocIdSet(reader.getSequentialSubReaders()[0]).isCacheable());
    assertEquals("find all", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",minIP,maxIP,T,F), numDocs).scoreDocs;
    assertEquals("all but last", numDocs-1, result.length);

    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",minIP,maxIP,F,T), numDocs).scoreDocs;
    assertEquals("all but first", numDocs-1, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",minIP,maxIP,F,F), numDocs).scoreDocs;
    assertEquals("all but ends", numDocs-2, result.length);
    
    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",medIP,maxIP,T,T), numDocs).scoreDocs;
    assertEquals("med and up", 1+ maxId-medId, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",minIP,medIP,T,T), numDocs).scoreDocs;
    assertEquals("up to med", 1+ medId-minId, result.length);

    // unbounded id

    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",null,null,T,T), numDocs).scoreDocs;
    assertEquals("find all", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",minIP,null,T,F), numDocs).scoreDocs;
    assertEquals("min and up", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",null,maxIP,F,T), numDocs).scoreDocs;
    assertEquals("max and down", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",minIP,null,F,F), numDocs).scoreDocs;
    assertEquals("not min, but up", numDocs-1, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",null,maxIP,F,F), numDocs).scoreDocs;
    assertEquals("not max, but down", numDocs-1, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",medIP,maxIP,T,F), numDocs).scoreDocs;
    assertEquals("med and up, not max", maxId-medId, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",minIP,medIP,F,T), numDocs).scoreDocs;
    assertEquals("not min, up to med", medId-minId, result.length);

    // very small sets

    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",minIP,minIP,F,F), numDocs).scoreDocs;
    assertEquals("min,min,F,F", 0, result.length);
    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",medIP,medIP,F,F), numDocs).scoreDocs;
    assertEquals("med,med,F,F", 0, result.length);
    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",maxIP,maxIP,F,F), numDocs).scoreDocs;
    assertEquals("max,max,F,F", 0, result.length);
                     
    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",minIP,minIP,T,T), numDocs).scoreDocs;
    assertEquals("min,min,T,T", 1, result.length);
    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",null,minIP,F,T), numDocs).scoreDocs;
    assertEquals("nul,min,F,T", 1, result.length);

    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",maxIP,maxIP,T,T), numDocs).scoreDocs;
    assertEquals("max,max,T,T", 1, result.length);
    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",maxIP,null,T,F), numDocs).scoreDocs;
    assertEquals("max,nul,T,T", 1, result.length);

    result = search.search(q,FieldCacheRangeFilter.newStringRange("id",medIP,medIP,T,T), numDocs).scoreDocs;
    assertEquals("med,med,T,T", 1, result.length);
        
  }

  public void testFieldCacheRangeFilterRand() throws IOException {

    IndexReader reader = IndexReader.open(signedIndex.index);
    IndexSearcher search = new IndexSearcher(reader);

    String minRP = pad(signedIndex.minR);
    String maxRP = pad(signedIndex.maxR);
    
    int numDocs = reader.numDocs();
        
    assertEquals("num of docs", numDocs, 1+ maxId - minId);
        
    ScoreDoc[] result;
    Query q = new TermQuery(new Term("body","body"));

    // test extremes, bounded on both ends
        
    result = search.search(q,FieldCacheRangeFilter.newStringRange("rand",minRP,maxRP,T,T), numDocs).scoreDocs;
    assertEquals("find all", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newStringRange("rand",minRP,maxRP,T,F), numDocs).scoreDocs;
    assertEquals("all but biggest", numDocs-1, result.length);

    result = search.search(q,FieldCacheRangeFilter.newStringRange("rand",minRP,maxRP,F,T), numDocs).scoreDocs;
    assertEquals("all but smallest", numDocs-1, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newStringRange("rand",minRP,maxRP,F,F), numDocs).scoreDocs;
    assertEquals("all but extremes", numDocs-2, result.length);
    
    // unbounded

    result = search.search(q,FieldCacheRangeFilter.newStringRange("rand",minRP,null,T,F), numDocs).scoreDocs;
    assertEquals("smallest and up", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newStringRange("rand",null,maxRP,F,T), numDocs).scoreDocs;
    assertEquals("biggest and down", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newStringRange("rand",minRP,null,F,F), numDocs).scoreDocs;
    assertEquals("not smallest, but up", numDocs-1, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newStringRange("rand",null,maxRP,F,F), numDocs).scoreDocs;
    assertEquals("not biggest, but down", numDocs-1, result.length);
        
    // very small sets

    result = search.search(q,FieldCacheRangeFilter.newStringRange("rand",minRP,minRP,F,F), numDocs).scoreDocs;
    assertEquals("min,min,F,F", 0, result.length);
    result = search.search(q,FieldCacheRangeFilter.newStringRange("rand",maxRP,maxRP,F,F), numDocs).scoreDocs;
    assertEquals("max,max,F,F", 0, result.length);
                     
    result = search.search(q,FieldCacheRangeFilter.newStringRange("rand",minRP,minRP,T,T), numDocs).scoreDocs;
    assertEquals("min,min,T,T", 1, result.length);
    result = search.search(q,FieldCacheRangeFilter.newStringRange("rand",null,minRP,F,T), numDocs).scoreDocs;
    assertEquals("nul,min,F,T", 1, result.length);

    result = search.search(q,FieldCacheRangeFilter.newStringRange("rand",maxRP,maxRP,T,T), numDocs).scoreDocs;
    assertEquals("max,max,T,T", 1, result.length);
    result = search.search(q,FieldCacheRangeFilter.newStringRange("rand",maxRP,null,T,F), numDocs).scoreDocs;
    assertEquals("max,nul,T,T", 1, result.length);
  }
  
  // byte-ranges cannot be tested, because all ranges are too big for bytes, need an extra range for that

  public void testFieldCacheRangeFilterShorts() throws IOException {

    IndexReader reader = IndexReader.open(signedIndex.index);
    IndexSearcher search = new IndexSearcher(reader);

    int numDocs = reader.numDocs();
    int medId = ((maxId - minId) / 2);
    Short minIdO = new Short((short) minId);
    Short maxIdO = new Short((short) maxId);
    Short medIdO = new Short((short) medId);
        
    assertEquals("num of docs", numDocs, 1+ maxId - minId);
        
    ScoreDoc[] result;
    Query q = new TermQuery(new Term("body","body"));

    // test id, bounded on both ends
    FieldCacheRangeFilter fcrf;
    result = search.search(q,fcrf=FieldCacheRangeFilter.newShortRange("id",minIdO,maxIdO,T,T), numDocs).scoreDocs;
    assertTrue(fcrf.getDocIdSet(reader.getSequentialSubReaders()[0]).isCacheable());
    assertEquals("find all", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",minIdO,maxIdO,T,F), numDocs).scoreDocs;
    assertEquals("all but last", numDocs-1, result.length);

    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",minIdO,maxIdO,F,T), numDocs).scoreDocs;
    assertEquals("all but first", numDocs-1, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",minIdO,maxIdO,F,F), numDocs).scoreDocs;
    assertEquals("all but ends", numDocs-2, result.length);
    
    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",medIdO,maxIdO,T,T), numDocs).scoreDocs;
    assertEquals("med and up", 1+ maxId-medId, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",minIdO,medIdO,T,T), numDocs).scoreDocs;
    assertEquals("up to med", 1+ medId-minId, result.length);
    
    // unbounded id

    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",null,null,T,T), numDocs).scoreDocs;
    assertEquals("find all", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",minIdO,null,T,F), numDocs).scoreDocs;
    assertEquals("min and up", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",null,maxIdO,F,T), numDocs).scoreDocs;
    assertEquals("max and down", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",minIdO,null,F,F), numDocs).scoreDocs;
    assertEquals("not min, but up", numDocs-1, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",null,maxIdO,F,F), numDocs).scoreDocs;
    assertEquals("not max, but down", numDocs-1, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",medIdO,maxIdO,T,F), numDocs).scoreDocs;
    assertEquals("med and up, not max", maxId-medId, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",minIdO,medIdO,F,T), numDocs).scoreDocs;
    assertEquals("not min, up to med", medId-minId, result.length);

    // very small sets

    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",minIdO,minIdO,F,F), numDocs).scoreDocs;
    assertEquals("min,min,F,F", 0, result.length);
    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",medIdO,medIdO,F,F), numDocs).scoreDocs;
    assertEquals("med,med,F,F", 0, result.length);
    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",maxIdO,maxIdO,F,F), numDocs).scoreDocs;
    assertEquals("max,max,F,F", 0, result.length);
                     
    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",minIdO,minIdO,T,T), numDocs).scoreDocs;
    assertEquals("min,min,T,T", 1, result.length);
    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",null,minIdO,F,T), numDocs).scoreDocs;
    assertEquals("nul,min,F,T", 1, result.length);

    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",maxIdO,maxIdO,T,T), numDocs).scoreDocs;
    assertEquals("max,max,T,T", 1, result.length);
    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",maxIdO,null,T,F), numDocs).scoreDocs;
    assertEquals("max,nul,T,T", 1, result.length);

    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",medIdO,medIdO,T,T), numDocs).scoreDocs;
    assertEquals("med,med,T,T", 1, result.length);
    
    // special cases
    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",new Short(Short.MAX_VALUE),null,F,F), numDocs).scoreDocs;
    assertEquals("overflow special case", 0, result.length);
    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",null,new Short(Short.MIN_VALUE),F,F), numDocs).scoreDocs;
    assertEquals("overflow special case", 0, result.length);
    result = search.search(q,FieldCacheRangeFilter.newShortRange("id",maxIdO,minIdO,T,T), numDocs).scoreDocs;
    assertEquals("inverse range", 0, result.length);
  }
  
  public void testFieldCacheRangeFilterInts() throws IOException {

    IndexReader reader = IndexReader.open(signedIndex.index);
    IndexSearcher search = new IndexSearcher(reader);

    int numDocs = reader.numDocs();
    int medId = ((maxId - minId) / 2);
    Integer minIdO = new Integer(minId);
    Integer maxIdO = new Integer(maxId);
    Integer medIdO = new Integer(medId);
        
    assertEquals("num of docs", numDocs, 1+ maxId - minId);
        
    ScoreDoc[] result;
    Query q = new TermQuery(new Term("body","body"));

    // test id, bounded on both ends
        
    FieldCacheRangeFilter fcrf;
    result = search.search(q,fcrf=FieldCacheRangeFilter.newIntRange("id",minIdO,maxIdO,T,T), numDocs).scoreDocs;
    assertTrue(fcrf.getDocIdSet(reader.getSequentialSubReaders()[0]).isCacheable());
    assertEquals("find all", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",minIdO,maxIdO,T,F), numDocs).scoreDocs;
    assertEquals("all but last", numDocs-1, result.length);

    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",minIdO,maxIdO,F,T), numDocs).scoreDocs;
    assertEquals("all but first", numDocs-1, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",minIdO,maxIdO,F,F), numDocs).scoreDocs;
    assertEquals("all but ends", numDocs-2, result.length);
    
    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",medIdO,maxIdO,T,T), numDocs).scoreDocs;
    assertEquals("med and up", 1+ maxId-medId, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",minIdO,medIdO,T,T), numDocs).scoreDocs;
    assertEquals("up to med", 1+ medId-minId, result.length);
    
    // unbounded id

    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",null,null,T,T), numDocs).scoreDocs;
    assertEquals("find all", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",minIdO,null,T,F), numDocs).scoreDocs;
    assertEquals("min and up", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",null,maxIdO,F,T), numDocs).scoreDocs;
    assertEquals("max and down", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",minIdO,null,F,F), numDocs).scoreDocs;
    assertEquals("not min, but up", numDocs-1, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",null,maxIdO,F,F), numDocs).scoreDocs;
    assertEquals("not max, but down", numDocs-1, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",medIdO,maxIdO,T,F), numDocs).scoreDocs;
    assertEquals("med and up, not max", maxId-medId, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",minIdO,medIdO,F,T), numDocs).scoreDocs;
    assertEquals("not min, up to med", medId-minId, result.length);

    // very small sets

    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",minIdO,minIdO,F,F), numDocs).scoreDocs;
    assertEquals("min,min,F,F", 0, result.length);
    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",medIdO,medIdO,F,F), numDocs).scoreDocs;
    assertEquals("med,med,F,F", 0, result.length);
    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",maxIdO,maxIdO,F,F), numDocs).scoreDocs;
    assertEquals("max,max,F,F", 0, result.length);
                     
    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",minIdO,minIdO,T,T), numDocs).scoreDocs;
    assertEquals("min,min,T,T", 1, result.length);
    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",null,minIdO,F,T), numDocs).scoreDocs;
    assertEquals("nul,min,F,T", 1, result.length);

    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",maxIdO,maxIdO,T,T), numDocs).scoreDocs;
    assertEquals("max,max,T,T", 1, result.length);
    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",maxIdO,null,T,F), numDocs).scoreDocs;
    assertEquals("max,nul,T,T", 1, result.length);

    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",medIdO,medIdO,T,T), numDocs).scoreDocs;
    assertEquals("med,med,T,T", 1, result.length);
    
    // special cases
    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",new Integer(Integer.MAX_VALUE),null,F,F), numDocs).scoreDocs;
    assertEquals("overflow special case", 0, result.length);
    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",null,new Integer(Integer.MIN_VALUE),F,F), numDocs).scoreDocs;
    assertEquals("overflow special case", 0, result.length);
    result = search.search(q,FieldCacheRangeFilter.newIntRange("id",maxIdO,minIdO,T,T), numDocs).scoreDocs;
    assertEquals("inverse range", 0, result.length);
  }
  
  public void testFieldCacheRangeFilterLongs() throws IOException {

    IndexReader reader = IndexReader.open(signedIndex.index);
    IndexSearcher search = new IndexSearcher(reader);

    int numDocs = reader.numDocs();
    int medId = ((maxId - minId) / 2);
    Long minIdO = new Long(minId);
    Long maxIdO = new Long(maxId);
    Long medIdO = new Long(medId);
        
    assertEquals("num of docs", numDocs, 1+ maxId - minId);
        
    ScoreDoc[] result;
    Query q = new TermQuery(new Term("body","body"));

    // test id, bounded on both ends
        
    FieldCacheRangeFilter fcrf;
    result = search.search(q,fcrf=FieldCacheRangeFilter.newLongRange("id",minIdO,maxIdO,T,T), numDocs).scoreDocs;
    assertTrue(fcrf.getDocIdSet(reader.getSequentialSubReaders()[0]).isCacheable());
    assertEquals("find all", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",minIdO,maxIdO,T,F), numDocs).scoreDocs;
    assertEquals("all but last", numDocs-1, result.length);

    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",minIdO,maxIdO,F,T), numDocs).scoreDocs;
    assertEquals("all but first", numDocs-1, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",minIdO,maxIdO,F,F), numDocs).scoreDocs;
    assertEquals("all but ends", numDocs-2, result.length);
    
    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",medIdO,maxIdO,T,T), numDocs).scoreDocs;
    assertEquals("med and up", 1+ maxId-medId, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",minIdO,medIdO,T,T), numDocs).scoreDocs;
    assertEquals("up to med", 1+ medId-minId, result.length);
    
    // unbounded id

    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",null,null,T,T), numDocs).scoreDocs;
    assertEquals("find all", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",minIdO,null,T,F), numDocs).scoreDocs;
    assertEquals("min and up", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",null,maxIdO,F,T), numDocs).scoreDocs;
    assertEquals("max and down", numDocs, result.length);

    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",minIdO,null,F,F), numDocs).scoreDocs;
    assertEquals("not min, but up", numDocs-1, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",null,maxIdO,F,F), numDocs).scoreDocs;
    assertEquals("not max, but down", numDocs-1, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",medIdO,maxIdO,T,F), numDocs).scoreDocs;
    assertEquals("med and up, not max", maxId-medId, result.length);
        
    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",minIdO,medIdO,F,T), numDocs).scoreDocs;
    assertEquals("not min, up to med", medId-minId, result.length);

    // very small sets

    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",minIdO,minIdO,F,F), numDocs).scoreDocs;
    assertEquals("min,min,F,F", 0, result.length);
    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",medIdO,medIdO,F,F), numDocs).scoreDocs;
    assertEquals("med,med,F,F", 0, result.length);
    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",maxIdO,maxIdO,F,F), numDocs).scoreDocs;
    assertEquals("max,max,F,F", 0, result.length);
                     
    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",minIdO,minIdO,T,T), numDocs).scoreDocs;
    assertEquals("min,min,T,T", 1, result.length);
    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",null,minIdO,F,T), numDocs).scoreDocs;
    assertEquals("nul,min,F,T", 1, result.length);

    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",maxIdO,maxIdO,T,T), numDocs).scoreDocs;
    assertEquals("max,max,T,T", 1, result.length);
    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",maxIdO,null,T,F), numDocs).scoreDocs;
    assertEquals("max,nul,T,T", 1, result.length);

    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",medIdO,medIdO,T,T), numDocs).scoreDocs;
    assertEquals("med,med,T,T", 1, result.length);
    
    // special cases
    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",new Long(Long.MAX_VALUE),null,F,F), numDocs).scoreDocs;
    assertEquals("overflow special case", 0, result.length);
    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",null,new Long(Long.MIN_VALUE),F,F), numDocs).scoreDocs;
    assertEquals("overflow special case", 0, result.length);
    result = search.search(q,FieldCacheRangeFilter.newLongRange("id",maxIdO,minIdO,T,T), numDocs).scoreDocs;
    assertEquals("inverse range", 0, result.length);
  }
  
  // float and double tests are a bit minimalistic, but its complicated, because missing precision
  
  public void testFieldCacheRangeFilterFloats() throws IOException {

    IndexReader reader = IndexReader.open(signedIndex.index);
    IndexSearcher search = new IndexSearcher(reader);

    int numDocs = reader.numDocs();
    Float minIdO = new Float(minId + .5f);
    Float medIdO = new Float(minIdO.floatValue() + ((float) (maxId-minId))/2.0f);
        
    ScoreDoc[] result;
    Query q = new TermQuery(new Term("body","body"));

    result = search.search(q,FieldCacheRangeFilter.newFloatRange("id",minIdO,medIdO,T,T), numDocs).scoreDocs;
    assertEquals("find all", numDocs/2, result.length);
    int count = 0;
    result = search.search(q,FieldCacheRangeFilter.newFloatRange("id",null,medIdO,F,T), numDocs).scoreDocs;
    count += result.length;
    result = search.search(q,FieldCacheRangeFilter.newFloatRange("id",medIdO,null,F,F), numDocs).scoreDocs;
    count += result.length;
    assertEquals("sum of two concenatted ranges", numDocs, count);
    result = search.search(q,FieldCacheRangeFilter.newFloatRange("id",null,null,T,T), numDocs).scoreDocs;
    assertEquals("find all", numDocs, result.length);
    result = search.search(q,FieldCacheRangeFilter.newFloatRange("id",new Float(Float.POSITIVE_INFINITY),null,F,F), numDocs).scoreDocs;
    assertEquals("infinity special case", 0, result.length);
    result = search.search(q,FieldCacheRangeFilter.newFloatRange("id",null,new Float(Float.NEGATIVE_INFINITY),F,F), numDocs).scoreDocs;
    assertEquals("infinity special case", 0, result.length);
  }
  
  public void testFieldCacheRangeFilterDoubles() throws IOException {

    IndexReader reader = IndexReader.open(signedIndex.index);
    IndexSearcher search = new IndexSearcher(reader);

    int numDocs = reader.numDocs();
    Double minIdO = new Double(minId + .5);
    Double medIdO = new Double(minIdO.floatValue() + ((double) (maxId-minId))/2.0);
        
    ScoreDoc[] result;
    Query q = new TermQuery(new Term("body","body"));

    result = search.search(q,FieldCacheRangeFilter.newDoubleRange("id",minIdO,medIdO,T,T), numDocs).scoreDocs;
    assertEquals("find all", numDocs/2, result.length);
    int count = 0;
    result = search.search(q,FieldCacheRangeFilter.newDoubleRange("id",null,medIdO,F,T), numDocs).scoreDocs;
    count += result.length;
    result = search.search(q,FieldCacheRangeFilter.newDoubleRange("id",medIdO,null,F,F), numDocs).scoreDocs;
    count += result.length;
    assertEquals("sum of two concenatted ranges", numDocs, count);
    result = search.search(q,FieldCacheRangeFilter.newDoubleRange("id",null,null,T,T), numDocs).scoreDocs;
    assertEquals("find all", numDocs, result.length);
    result = search.search(q,FieldCacheRangeFilter.newDoubleRange("id",new Double(Double.POSITIVE_INFINITY),null,F,F), numDocs).scoreDocs;
    assertEquals("infinity special case", 0, result.length);
    result = search.search(q,FieldCacheRangeFilter.newDoubleRange("id",null, new Double(Double.NEGATIVE_INFINITY),F,F), numDocs).scoreDocs;
    assertEquals("infinity special case", 0, result.length);
  }
  
  // test using a sparse index (with deleted docs). The DocIdSet should be not cacheable, as it uses TermDocs if the range contains 0
  public void testSparseIndex() throws IOException {
    RAMDirectory dir = new RAMDirectory();
    IndexWriter writer = new IndexWriter(dir, new SimpleAnalyzer(), T, IndexWriter.MaxFieldLength.LIMITED);

    for (int d = -20; d <= 20; d++) {
      Document doc = new Document();
      doc.add(new Field("id",Integer.toString(d), Field.Store.NO, Field.Index.NOT_ANALYZED));
      doc.add(new Field("body","body", Field.Store.NO, Field.Index.NOT_ANALYZED));
      writer.addDocument(doc);
    }
    
    writer.optimize();
    writer.deleteDocuments(new Term("id","0"));
    writer.close();

    IndexReader reader = IndexReader.open(dir);
    IndexSearcher search = new IndexSearcher(reader);
    assertTrue(reader.hasDeletions());

    ScoreDoc[] result;
    FieldCacheRangeFilter fcrf;
    Query q = new TermQuery(new Term("body","body"));

    result = search.search(q,fcrf=FieldCacheRangeFilter.newByteRange("id",new Byte((byte) -20),new Byte((byte) 20),T,T), 100).scoreDocs;
    assertFalse("DocIdSet must be not cacheable", fcrf.getDocIdSet(reader.getSequentialSubReaders()[0]).isCacheable());
    assertEquals("find all", 40, result.length);

    result = search.search(q,fcrf=FieldCacheRangeFilter.newByteRange("id",new Byte((byte) 0),new Byte((byte) 20),T,T), 100).scoreDocs;
    assertFalse("DocIdSet must be not cacheable", fcrf.getDocIdSet(reader.getSequentialSubReaders()[0]).isCacheable());
    assertEquals("find all", 20, result.length);

    result = search.search(q,fcrf=FieldCacheRangeFilter.newByteRange("id",new Byte((byte) -20),new Byte((byte) 0),T,T), 100).scoreDocs;
    assertFalse("DocIdSet must be not cacheable", fcrf.getDocIdSet(reader.getSequentialSubReaders()[0]).isCacheable());
    assertEquals("find all", 20, result.length);

    result = search.search(q,fcrf=FieldCacheRangeFilter.newByteRange("id",new Byte((byte) 10),new Byte((byte) 20),T,T), 100).scoreDocs;
    assertTrue("DocIdSet must be cacheable", fcrf.getDocIdSet(reader.getSequentialSubReaders()[0]).isCacheable());
    assertEquals("find all", 11, result.length);

    result = search.search(q,fcrf=FieldCacheRangeFilter.newByteRange("id",new Byte((byte) -20),new Byte((byte) -10),T,T), 100).scoreDocs;
    assertTrue("DocIdSet must be cacheable", fcrf.getDocIdSet(reader.getSequentialSubReaders()[0]).isCacheable());
    assertEquals("find all", 11, result.length);
  }
  
}
