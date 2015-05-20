package org.apache.lucene.index.memory;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

/**
 * Verifies that Lucene MemoryIndex and RAMDirectory have the same behaviour,
 * returning the same results for queries on some randomish indexes.
 */
public class MemoryIndexTest extends BaseTokenStreamTestCase {
  private Set<String> queries = new HashSet<String>();
  private Random random;
  
  public static final int ITERATIONS = 100;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    queries.addAll(readQueries("testqueries.txt"));
    queries.addAll(readQueries("testqueries2.txt"));
    random = newRandom();
  }
  
  /**
   * read a set of queries from a resource file
   */
  private Set<String> readQueries(String resource) throws IOException {
    Set<String> queries = new HashSet<String>();
    InputStream stream = getClass().getResourceAsStream(resource);
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
    String line = null;
    while ((line = reader.readLine()) != null) {
      line = line.trim();
      if (line.length() > 0 && !line.startsWith("#") && !line.startsWith("//")) {
        queries.add(line);
      }
    }
    return queries;
  }
  
  
  /**
   * runs random tests, up to ITERATIONS times.
   */
  public void testRandomQueries() throws Exception {
    for (int i = 0; i < ITERATIONS; i++)
      assertAgainstRAMDirectory();
  }

  /**
   * Build a randomish document for both RAMDirectory and MemoryIndex,
   * and run all the queries against it.
   */
  public void assertAgainstRAMDirectory() throws Exception {
    StringBuilder fooField = new StringBuilder();
    StringBuilder termField = new StringBuilder();
 
    // add up to 250 terms to field "foo"
    for (int i = 0; i < random.nextInt(250); i++) {
      fooField.append(" ");
      fooField.append(randomTerm());
    }

    // add up to 250 terms to field "term"
    for (int i = 0; i < random.nextInt(250); i++) {
      termField.append(" ");
      termField.append(randomTerm());
    }
    
    RAMDirectory ramdir = new RAMDirectory();
    Analyzer analyzer = randomAnalyzer();
    IndexWriter writer = new IndexWriter(ramdir, analyzer,
        IndexWriter.MaxFieldLength.UNLIMITED);
    Document doc = new Document();
    Field field1 = new Field("foo", fooField.toString(), Field.Store.NO, Field.Index.ANALYZED);
    Field field2 = new Field("term", termField.toString(), Field.Store.NO, Field.Index.ANALYZED);
    doc.add(field1);
    doc.add(field2);
    writer.addDocument(doc);
    writer.close();
    
    MemoryIndex memory = new MemoryIndex();
    memory.addField("foo", fooField.toString(), analyzer);
    memory.addField("term", termField.toString(), analyzer);
    assertAllQueries(memory, ramdir, analyzer);  
  }
  
  /**
   * Run all queries against both the RAMDirectory and MemoryIndex, ensuring they are the same.
   */
  public void assertAllQueries(MemoryIndex memory, RAMDirectory ramdir, Analyzer analyzer) throws Exception {
    IndexSearcher ram = new IndexSearcher(ramdir);
    IndexSearcher mem = memory.createSearcher();
    QueryParser qp = new QueryParser(Version.LUCENE_CURRENT, "foo", analyzer);
    for (String query : queries) {
      TopDocs ramDocs = ram.search(qp.parse(query), 1);
      TopDocs memDocs = mem.search(qp.parse(query), 1);
      assertEquals(ramDocs.totalHits, memDocs.totalHits);
    }
  }
  
  /**
   * Return a random analyzer (Simple, Stop, Standard) to analyze the terms.
   */
  private Analyzer randomAnalyzer() {
    switch(random.nextInt(3)) {
      case 0: return new SimpleAnalyzer();
      case 1: return new StopAnalyzer(Version.LUCENE_CURRENT);
      default: return new StandardAnalyzer(Version.LUCENE_CURRENT);
    }
  }
  
  /**
   * Some terms to be indexed, in addition to random words. 
   * These terms are commonly used in the queries. 
   */
  private static final String[] TEST_TERMS = {"term", "Term", "tErm", "TERM",
      "telm", "stop", "drop", "roll", "phrase", "a", "c", "bar", "blar",
      "gack", "weltbank", "worlbank", "hello", "on", "the", "apache", "Apache",
      "copyright", "Copyright"};
  
  
  /**
   * half of the time, returns a random term from TEST_TERMS.
   * the other half of the time, returns a random unicode string.
   */
  private String randomTerm() {
    if (random.nextBoolean()) {
      // return a random TEST_TERM
      return TEST_TERMS[random.nextInt(TEST_TERMS.length)];
    } else {
      // return a random unicode term
      return randomString();
    }
  }
  
  /**
   * Return a random unicode term, like TestStressIndexing.
   */
  private String randomString() {
    final int end = random.nextInt(20);
    if (buffer.length < 1 + end) {
      char[] newBuffer = new char[(int) ((1 + end) * 1.25)];
      System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
      buffer = newBuffer;
    }
    for (int i = 0; i < end - 1; i++) {
      int t = random.nextInt(6);
      if (0 == t && i < end - 1) {
        // Make a surrogate pair
        // High surrogate
        buffer[i++] = (char) nextInt(0xd800, 0xdc00);
        // Low surrogate
        buffer[i] = (char) nextInt(0xdc00, 0xe000);
      } else if (t <= 1) buffer[i] = (char) random.nextInt(0x80);
      else if (2 == t) buffer[i] = (char) nextInt(0x80, 0x800);
      else if (3 == t) buffer[i] = (char) nextInt(0x800, 0xd7ff);
      else if (4 == t) buffer[i] = (char) nextInt(0xe000, 0xffff);
      else if (5 == t) {
        // Illegal unpaired surrogate
        if (random.nextBoolean()) buffer[i] = (char) nextInt(0xd800, 0xdc00);
        else buffer[i] = (char) nextInt(0xdc00, 0xe000);
      }
    }
    return new String(buffer, 0, end);
  }
  
  private char buffer[] = new char[20];
  // start is inclusive and end is exclusive
  private int nextInt(int start, int end) {
    return start + random.nextInt(end - start);
  }
}
