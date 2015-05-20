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

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

/**
 * A {@link TokenFilter} that applies {@link ArabicNormalizer} to normalize the orthography.
 * 
 */

public final class ArabicNormalizationFilter extends TokenFilter {

  protected ArabicNormalizer normalizer = null;
  private TermAttribute termAtt;
  
  public ArabicNormalizationFilter(TokenStream input) {
    super(input);
    normalizer = new ArabicNormalizer();
    termAtt = (TermAttribute) addAttribute(TermAttribute.class);
  }

  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      int newlen = normalizer.normalize(termAtt.termBuffer(), termAtt.termLength());
      termAtt.setTermLength(newlen);
      return true;
    } else {
      return false;
    }
  }
}
