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

package org.apache.lucene.analysis.cn.smart;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.cn.smart.SentenceTokenizer;
import org.apache.lucene.analysis.cn.smart.WordTokenFilter;
import org.apache.lucene.util.Version;

/**
 * <p>
 * SmartChineseAnalyzer is an analyzer for Chinese or mixed Chinese-English text.
 * The analyzer uses probabilistic knowledge to find the optimal word segmentation for Simplified Chinese text.
 * The text is first broken into sentences, then each sentence is segmented into words.
 * </p>
 * <p>
 * Segmentation is based upon the <a href="http://en.wikipedia.org/wiki/Hidden_Markov_Model">Hidden Markov Model</a>. 
 * A large training corpus was used to calculate Chinese word frequency probability.
 * </p>
 * <p>
 * This analyzer requires a dictionary to provide statistical data. 
 * SmartChineseAnalyzer has an included dictionary out-of-box.
 * </p>
 * <p>
 * The included dictionary data is from <a href="http://www.ictclas.org">ICTCLAS1.0</a>.
 * Thanks to ICTCLAS for their hard work, and for contributing the data under the Apache 2 License!
 * </p>
 * <p><font color="#FF0000">
 * WARNING: The status of the analyzers/smartcn <b>analysis.cn.smart</b> package is experimental. 
 * The APIs and file formats introduced here might change in the future and will not be 
 * supported anymore in such a case.</font>
 * </p>
 */
public class SmartChineseAnalyzer extends Analyzer {

  private final Set stopWords;
  private final Version matchVersion;

  /**
   * Create a new SmartChineseAnalyzer, using the default stopword list.
   *
   * @deprecated Use {@link #SmartChineseAnalyzer(Version)} instead
   */
  public SmartChineseAnalyzer() {
    this(Version.LUCENE_24, true);
  }

  /**
   * Create a new SmartChineseAnalyzer, using the default stopword list.
   */
  public SmartChineseAnalyzer(Version matchVersion) {
    this(matchVersion, true);
  }

  /**
   * <p>
   * Create a new SmartChineseAnalyzer, optionally using the default stopword list.
   * </p>
   * <p>
   * The included default stopword list is simply a list of punctuation.
   * If you do not use this list, punctuation will not be removed from the text!
   * </p>
   * 
   * @param useDefaultStopWords true to use the default stopword list.
   *
   * @deprecated Use {@link #SmartChineseAnalyzer(Version, boolean)} instead
   */
  public SmartChineseAnalyzer(boolean useDefaultStopWords) {
    this(Version.LUCENE_24, useDefaultStopWords);
  }

  /**
   * <p>
   * Create a new SmartChineseAnalyzer, optionally using the default stopword list.
   * </p>
   * <p>
   * The included default stopword list is simply a list of punctuation.
   * If you do not use this list, punctuation will not be removed from the text!
   * </p>
   * 
   * @param useDefaultStopWords true to use the default stopword list.
   */
  public SmartChineseAnalyzer(Version matchVersion, boolean useDefaultStopWords) {
    this.matchVersion = matchVersion;
    if (useDefaultStopWords) {
      try {
      InputStream stream = this.getClass().getResourceAsStream("stopwords.txt");
      InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
      stopWords = WordlistLoader.getWordSet(reader, "//");
      } catch (IOException e) {
        // TODO: throw IOException
        throw new RuntimeException(e);
      }
    }else{
      stopWords = null;
    }
  }

  /**
   * <p>
   * Create a new SmartChineseAnalyzer, using the provided {@link Set} of stopwords.
   * </p>
   * <p>
   * Note: the set should include punctuation, unless you want to index punctuation!
   * </p>
   * @param stopWords {@link Set} of stopwords to use.
   *
   * @deprecated Use {@link #SmartChineseAnalyzer(Version, Set)} instead
   */
  public SmartChineseAnalyzer(Set stopWords) {
    this(Version.LUCENE_24, stopWords);
  }

  /**
   * <p>
   * Create a new SmartChineseAnalyzer, using the provided {@link Set} of stopwords.
   * </p>
   * <p>
   * Note: the set should include punctuation, unless you want to index punctuation!
   * </p>
   * @param stopWords {@link Set} of stopwords to use.
   */
  public SmartChineseAnalyzer(Version matchVersion, Set stopWords) {
    this.stopWords = stopWords;
    this.matchVersion = matchVersion;
  }

  public TokenStream tokenStream(String fieldName, Reader reader) {
    TokenStream result = new SentenceTokenizer(reader);
    result = new WordTokenFilter(result);
    // result = new LowerCaseFilter(result);
    // LowerCaseFilter is not needed, as SegTokenFilter lowercases Basic Latin text.
    // The porter stemming is too strict, this is not a bug, this is a feature:)
    result = new PorterStemFilter(result);
    if (stopWords != null) {
      result = new StopFilter(StopFilter.getEnablePositionIncrementsVersionDefault(matchVersion),
                              result, stopWords, false);
    }
    return result;
  }
  
  private static final class SavedStreams {
    Tokenizer tokenStream;
    TokenStream filteredTokenStream;
  }
  
  public TokenStream reusableTokenStream(String fieldName, Reader reader)
      throws IOException {
    SavedStreams streams = (SavedStreams) getPreviousTokenStream();
    if (streams == null) {
      streams = new SavedStreams();
      setPreviousTokenStream(streams);
      streams.tokenStream = new SentenceTokenizer(reader);
      streams.filteredTokenStream = new WordTokenFilter(streams.tokenStream);
      streams.filteredTokenStream = new PorterStemFilter(streams.filteredTokenStream);
      if (stopWords != null) {
        streams.filteredTokenStream = new StopFilter(StopFilter.getEnablePositionIncrementsVersionDefault(matchVersion),
                                                     streams.filteredTokenStream, stopWords, false);
      }
    } else {
      streams.tokenStream.reset(reader);
      streams.filteredTokenStream.reset(); // reset WordTokenFilter's state
    }

    return streams.filteredTokenStream;
  }
}
