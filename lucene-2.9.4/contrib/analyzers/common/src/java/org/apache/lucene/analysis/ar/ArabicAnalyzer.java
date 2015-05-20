package org.apache.lucene.analysis.ar;

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.util.Version;

/**
 * {@link Analyzer} for Arabic. 
 * <p>
 * This analyzer implements light-stemming as specified by:
 * <i>
 * Light Stemming for Arabic Information Retrieval
 * </i>    
 * http://www.mtholyoke.edu/~lballest/Pubs/arab_stem05.pdf
 * <p>
 * The analysis package contains three primary components:
 * <ul>
 *  <li>{@link ArabicNormalizationFilter}: Arabic orthographic normalization.
 *  <li>{@link ArabicStemFilter}: Arabic light stemming
 *  <li>Arabic stop words file: a set of default Arabic stop words.
 * </ul>
 * 
 */
public final class ArabicAnalyzer extends Analyzer {

  /**
   * File containing default Arabic stopwords.
   * 
   * Default stopword list is from http://members.unine.ch/jacques.savoy/clef/index.html
   * The stopword list is BSD-Licensed.
   */
  public final static String DEFAULT_STOPWORD_FILE = "stopwords.txt";

  /**
   * Contains the stopwords used with the StopFilter.
   */
  private Set stoptable = new HashSet();
  /**
   * The comment character in the stopwords file.  All lines prefixed with this will be ignored  
   */
  public static final String STOPWORDS_COMMENT = "#";

  private final Version matchVersion;

  /**
   * Builds an analyzer with the default stop words: {@link #DEFAULT_STOPWORD_FILE}.
   *
   * @deprecated Use {@link #ArabicAnalyzer(Version)} instead
   */
  public ArabicAnalyzer() {
    this(Version.LUCENE_24);
  }

  /**
   * Builds an analyzer with the default stop words: {@link #DEFAULT_STOPWORD_FILE}.
   */
  public ArabicAnalyzer(Version matchVersion) {
    this.matchVersion = matchVersion;
    try {
      InputStream stream = ArabicAnalyzer.class.getResourceAsStream(DEFAULT_STOPWORD_FILE);
      InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
      stoptable = WordlistLoader.getWordSet(reader, STOPWORDS_COMMENT);
      reader.close();
      stream.close();
    } catch (IOException e) {
      // TODO: throw IOException
      throw new RuntimeException(e);
    }
  }

  /**
   * Builds an analyzer with the given stop words.
   *
   * @deprecated Use {@link #ArabicAnalyzer(Version, String[])} instead
   */
  public ArabicAnalyzer( String[] stopwords ) {
    this(Version.LUCENE_24, stopwords);
  }

  /**
   * Builds an analyzer with the given stop words.
   */
  public ArabicAnalyzer( Version matchVersion, String[] stopwords ) {
    stoptable = StopFilter.makeStopSet( stopwords );
    this.matchVersion = matchVersion;
  }

  /**
   * Builds an analyzer with the given stop words.
   *
   * @deprecated Use {@link #ArabicAnalyzer(Version, Hashtable)} instead
   */
  public ArabicAnalyzer( Hashtable stopwords ) {
    this(Version.LUCENE_24, stopwords);
  }

  /**
   * Builds an analyzer with the given stop words.
   */
  public ArabicAnalyzer( Version matchVersion, Hashtable stopwords ) {
    stoptable = new HashSet(stopwords.keySet());
    this.matchVersion = matchVersion;
  }

  /**
   * Builds an analyzer with the given stop words.  Lines can be commented out using {@link #STOPWORDS_COMMENT}
   *
   * @deprecated Use {@link #ArabicAnalyzer(Version, File)} instead
   */
  public ArabicAnalyzer( File stopwords ) throws IOException {
    this(Version.LUCENE_24, stopwords);
  }

  /**
   * Builds an analyzer with the given stop words.  Lines can be commented out using {@link #STOPWORDS_COMMENT}
   */
  public ArabicAnalyzer( Version matchVersion, File stopwords ) throws IOException {
    stoptable = WordlistLoader.getWordSet( stopwords, STOPWORDS_COMMENT);
    this.matchVersion = matchVersion;
  }


  /**
   * Creates a {@link TokenStream} which tokenizes all the text in the provided {@link Reader}.
   *
   * @return  A {@link TokenStream} built from an {@link ArabicLetterTokenizer} filtered with
   * 			{@link LowerCaseFilter}, {@link StopFilter}, {@link ArabicNormalizationFilter}
   *            and {@link ArabicStemFilter}.
   */
  public final TokenStream tokenStream(String fieldName, Reader reader) {
    TokenStream result = new ArabicLetterTokenizer( reader );
    result = new LowerCaseFilter(result);
    result = new StopFilter( StopFilter.getEnablePositionIncrementsVersionDefault(matchVersion),
                             result, stoptable );
    result = new ArabicNormalizationFilter( result );
    result = new ArabicStemFilter( result );

    return result;
  }
  
  private class SavedStreams {
    Tokenizer source;
    TokenStream result;
  };
  
  /**
   * Returns a (possibly reused) {@link TokenStream} which tokenizes all the text 
   * in the provided {@link Reader}.
   *
   * @return  A {@link TokenStream} built from an {@link ArabicLetterTokenizer} filtered with
   *            {@link LowerCaseFilter}, {@link StopFilter}, {@link ArabicNormalizationFilter}
   *            and {@link ArabicStemFilter}.
   */
  public TokenStream reusableTokenStream(String fieldName, Reader reader)
      throws IOException {
    SavedStreams streams = (SavedStreams) getPreviousTokenStream();
    if (streams == null) {
      streams = new SavedStreams();
      streams.source = new ArabicLetterTokenizer(reader);
      streams.result = new LowerCaseFilter(streams.source);
      streams.result = new StopFilter(StopFilter.getEnablePositionIncrementsVersionDefault(matchVersion),
                                      streams.result, stoptable);
      streams.result = new ArabicNormalizationFilter(streams.result);
      streams.result = new ArabicStemFilter(streams.result);
      setPreviousTokenStream(streams);
    } else {
      streams.source.reset(reader);
    }
    return streams.result;
  }
}

