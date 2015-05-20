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

import java.util.Random;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.TestIndexWriterReader.HeavyAtomicInt;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MockRAMDirectory;
import org.apache.lucene.util.LuceneTestCase;

public class TestNRTReaderWithThreads extends LuceneTestCase {
  Random random = new Random();
  HeavyAtomicInt seq = new HeavyAtomicInt(1);

  public void testIndexing() throws Exception {
    Directory mainDir = new MockRAMDirectory();
    IndexWriter writer = new IndexWriter(mainDir, new WhitespaceAnalyzer(),
        IndexWriter.MaxFieldLength.LIMITED);
    writer.setUseCompoundFile(false);
    IndexReader reader = writer.getReader(); // start pooling readers
    reader.close();
    writer.setMergeFactor(2);
    writer.setMaxBufferedDocs(10);
    RunThread[] indexThreads = new RunThread[4];
    for (int x=0; x < indexThreads.length; x++) {
      indexThreads[x] = new RunThread(x % 2, writer);
      indexThreads[x].setName("Thread " + x);
      indexThreads[x].start();
    }    
    long startTime = System.currentTimeMillis();
    long duration = 5*1000;
    while ((System.currentTimeMillis() - startTime) < duration) {
      Thread.sleep(100);
    }
    int delCount = 0;
    int addCount = 0;
    for (int x=0; x < indexThreads.length; x++) {
      indexThreads[x].run = false;
      assertTrue(indexThreads[x].ex == null);
      addCount += indexThreads[x].addCount;
      delCount += indexThreads[x].delCount;
    }
    for (int x=0; x < indexThreads.length; x++) {
      indexThreads[x].join();
    }
    //System.out.println("addCount:"+addCount);
    //System.out.println("delCount:"+delCount);
    writer.close();
    mainDir.close();
  }

  public class RunThread extends Thread {
    IndexWriter writer;
    boolean run = true;
    Throwable ex;
    int delCount = 0;
    int addCount = 0;
    int type;

    public RunThread(int type, IndexWriter writer) {
      this.type = type;
      this.writer = writer;
    }

    public void run() {
      try {
        while (run) {
          //int n = random.nextInt(2);
          if (type == 0) {
            int i = seq.addAndGet(1);
            Document doc = TestIndexWriterReader.createDocument(i, "index1", 10);
            writer.addDocument(doc);
            addCount++;
          } else if (type == 1) {
            // we may or may not delete because the term may not exist,
            // however we're opening and closing the reader rapidly
            IndexReader reader = writer.getReader();
            int id = random.nextInt(seq.intValue());
            Term term = new Term("id", Integer.toString(id));
            int count = TestIndexWriterReader.count(term, reader);
            writer.deleteDocuments(term);
            reader.close();
            delCount += count;
          }
        }
      } catch (Throwable ex) {
        ex.printStackTrace(System.out);
        this.ex = ex;
        run = false;
      }
    }
  }
}
