package org.apache.lucene.index;
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

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import junit.framework.TestCase;

public class TestMultiPassIndexSplitter extends TestCase {
  IndexReader input;
  int NUM_DOCS = 11;

  @Override
  public void setUp() throws Exception {
    RAMDirectory dir = new RAMDirectory();
    IndexWriter w = new IndexWriter(dir, new WhitespaceAnalyzer(), true,
            MaxFieldLength.LIMITED);
    Document doc;
    for (int i = 0; i < NUM_DOCS; i++) {
      doc = new Document();
      doc.add(new Field("id", i + "", Field.Store.YES, Field.Index.NOT_ANALYZED));
      doc.add(new Field("f", i + " " + i, Field.Store.YES, Field.Index.ANALYZED));
      w.addDocument(doc);
    }
    w.close();
    input = IndexReader.open(dir, false);
    // delete the last doc
    input.deleteDocument(input.maxDoc() - 1);
    input = input.reopen(true);
  }
  
  /**
   * Test round-robin splitting.
   */
  public void testSplitRR() throws Exception {
    MultiPassIndexSplitter splitter = new MultiPassIndexSplitter();
    Directory[] dirs = new Directory[]{
            new RAMDirectory(),
            new RAMDirectory(),
            new RAMDirectory()
    };
    splitter.split(input, dirs, false);
    IndexReader ir;
    ir = IndexReader.open(dirs[0], true);
    assertTrue(ir.numDocs() - NUM_DOCS / 3 <= 1); // rounding error
    Document doc = ir.document(0);
    assertEquals("0", doc.get("id"));
    Term t;
    TermEnum te;
    t = new Term("id", "1");
    te = ir.terms(t);
    assertNotSame(t, te.term());
    ir.close();
    ir = IndexReader.open(dirs[1], true);
    assertTrue(ir.numDocs() - NUM_DOCS / 3 <= 1);
    doc = ir.document(0);
    assertEquals("1", doc.get("id"));
    t = new Term("id", "0");
    te = ir.terms(t);
    assertNotSame(t, te.term());
    ir.close();
    ir = IndexReader.open(dirs[2], true);
    assertTrue(ir.numDocs() - NUM_DOCS / 3 <= 1);
    doc = ir.document(0);
    assertEquals("2", doc.get("id"));
    t = new Term("id", "1");
    te = ir.terms(t);
    assertNotSame(t, te.term());
    t = new Term("id", "0");
    te = ir.terms(t);
    assertNotSame(t, te.term());    
  }
  
  /**
   * Test sequential splitting.
   */
  public void testSplitSeq() throws Exception {
    MultiPassIndexSplitter splitter = new MultiPassIndexSplitter();
    Directory[] dirs = new Directory[]{
            new RAMDirectory(),
            new RAMDirectory(),
            new RAMDirectory()
    };
    splitter.split(input, dirs, true);
    IndexReader ir;
    ir = IndexReader.open(dirs[0], true);
    assertTrue(ir.numDocs() - NUM_DOCS / 3 <= 1);
    Document doc = ir.document(0);
    assertEquals("0", doc.get("id"));
    int start = ir.numDocs();
    ir.close();
    ir = IndexReader.open(dirs[1], true);
    assertTrue(ir.numDocs() - NUM_DOCS / 3 <= 1);
    doc = ir.document(0);
    assertEquals(start + "", doc.get("id"));
    start += ir.numDocs();
    ir.close();
    ir = IndexReader.open(dirs[2], true);
    assertTrue(ir.numDocs() - NUM_DOCS / 3 <= 1);
    doc = ir.document(0);
    assertEquals(start + "", doc.get("id"));
    // make sure the deleted doc is not here
    Term t;
    TermEnum te;
    t = new Term("id", (NUM_DOCS - 1) + "");
    te = ir.terms(t);
    assertNotSame(t, te.term());    
  }
}
