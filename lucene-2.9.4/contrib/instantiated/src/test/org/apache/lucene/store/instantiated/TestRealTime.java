/**
 * Copyright 2006 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.store.instantiated;

import junit.framework.TestCase;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocCollector;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;

/**
 * Assert that the content of an index 
 * is instantly available
 * for all open searchers
 * also after a commit.
 */
public class TestRealTime extends TestCase {

  public void test() throws Exception {

    InstantiatedIndex index = new InstantiatedIndex();
    InstantiatedIndexReader reader = new InstantiatedIndexReader(index);
    IndexSearcher searcher = new IndexSearcher(reader);
    InstantiatedIndexWriter writer = new InstantiatedIndexWriter(index);

    Document doc;
    Collector collector;

    doc = new Document();
    doc.add(new Field("f", "a", Field.Store.NO, Field.Index.NOT_ANALYZED));
    writer.addDocument(doc);
    writer.commit();

    collector = new Collector();
    searcher.search(new TermQuery(new Term("f", "a")), collector);
    assertEquals(1, collector.hits);

    doc = new Document();
    doc.add(new Field("f", "a", Field.Store.NO, Field.Index.NOT_ANALYZED));
    writer.addDocument(doc);
    writer.commit();

    collector = new Collector();
    searcher.search(new TermQuery(new Term("f", "a")), collector);
    assertEquals(2, collector.hits);

  }

  public static class Collector extends HitCollector {
    private int hits = 0;
    public void collect(int doc, float score) {
      hits++;
    }
  }

}
