package org.apache.lucene.analysis;

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

import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.Version;

import java.io.StringReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

public class TestStopAnalyzer extends BaseTokenStreamTestCase {
  
  private StopAnalyzer stop = new StopAnalyzer(Version.LUCENE_CURRENT);
  private Set inValidTokens = new HashSet();
  
  public TestStopAnalyzer(String s) {
    super(s);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    Iterator it = StopAnalyzer.ENGLISH_STOP_WORDS_SET.iterator();
    while(it.hasNext()) {
      inValidTokens.add(it.next());
    }
  }

  public void testDefaults() throws IOException {
    assertTrue(stop != null);
    StringReader reader = new StringReader("This is a test of the english stop analyzer");
    TokenStream stream = stop.tokenStream("test", reader);
    assertTrue(stream != null);
    TermAttribute termAtt = stream.getAttribute(TermAttribute.class);
    
    while (stream.incrementToken()) {
      assertFalse(inValidTokens.contains(termAtt.term()));
    }
  }

  public void testStopList() throws IOException {
    Set stopWordsSet = new HashSet();
    stopWordsSet.add("good");
    stopWordsSet.add("test");
    stopWordsSet.add("analyzer");
    StopAnalyzer newStop = new StopAnalyzer(Version.LUCENE_24, stopWordsSet);
    StringReader reader = new StringReader("This is a good test of the english stop analyzer");
    TokenStream stream = newStop.tokenStream("test", reader);
    assertNotNull(stream);
    TermAttribute termAtt = stream.getAttribute(TermAttribute.class);
    PositionIncrementAttribute posIncrAtt = stream.addAttribute(PositionIncrementAttribute.class);
    
    while (stream.incrementToken()) {
      String text = termAtt.term();
      assertFalse(stopWordsSet.contains(text));
      assertEquals(1,posIncrAtt.getPositionIncrement()); // in 2.4 stop tokenizer does not apply increments.
    }
  }

  public void testStopListPositions() throws IOException {
    Set stopWordsSet = new HashSet();
    stopWordsSet.add("good");
    stopWordsSet.add("test");
    stopWordsSet.add("analyzer");
    StopAnalyzer newStop = new StopAnalyzer(Version.LUCENE_CURRENT, stopWordsSet);
    StringReader reader = new StringReader("This is a good test of the english stop analyzer with positions");
    int expectedIncr[] =                  { 1,   1, 1,          3, 1,  1,      1,            2,   1};
    TokenStream stream = newStop.tokenStream("test", reader);
    assertNotNull(stream);
    int i = 0;
    TermAttribute termAtt = stream.getAttribute(TermAttribute.class);
    PositionIncrementAttribute posIncrAtt = stream.addAttribute(PositionIncrementAttribute.class);

    while (stream.incrementToken()) {
      String text = termAtt.term();
      assertFalse(stopWordsSet.contains(text));
      assertEquals(expectedIncr[i++],posIncrAtt.getPositionIncrement());
    }
  }

}
