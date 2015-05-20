package org.apache.lucene.analysis.fr;

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

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A {@link TokenFilter} that stems french words. 
 * <p>
 * It supports a table of words that should
 * not be stemmed at all. The used stemmer can be changed at runtime after the
 * filter object is created (as long as it is a {@link FrenchStemmer}).
 * </p>
 * NOTE: This stemmer does not implement the Snowball algorithm correctly,
 * especially involving case problems. It is recommended that you consider using
 * the "French" stemmer in the snowball package instead. This stemmer will likely
 * be deprecated in a future release.
 */
public final class FrenchStemFilter extends TokenFilter {

	/**
	 * The actual token in the input stream.
	 */
	private FrenchStemmer stemmer = null;
	private Set exclusions = null;
	
	private TermAttribute termAtt;

	public FrenchStemFilter( TokenStream in ) {
          super(in);
		stemmer = new FrenchStemmer();
		termAtt = (TermAttribute) addAttribute(TermAttribute.class);
	}


	public FrenchStemFilter( TokenStream in, Set exclusiontable ) {
		this( in );
		exclusions = exclusiontable;
	}

	/**
	 * @return  Returns true for the next token in the stream, or false at EOS
	 */
	public boolean incrementToken() throws IOException {
	  if (input.incrementToken()) {
	    String term = termAtt.term();

	    // Check the exclusion table
	    if ( exclusions == null || !exclusions.contains( term ) ) {
	      String s = stemmer.stem( term );
	      // If not stemmed, don't waste the time  adjusting the token.
	      if ((s != null) && !s.equals( term ) )
	        termAtt.setTermBuffer(s);
	    }
	    return true;
	  } else {
	    return false;
	  }
	}
	/**
	 * Set a alternative/custom {@link FrenchStemmer} for this filter.
	 */
	public void setStemmer( FrenchStemmer stemmer ) {
		if ( stemmer != null ) {
			this.stemmer = stemmer;
		}
	}
	/**
	 * Set an alternative exclusion list for this filter.
	 */
	public void setExclusionTable( Map exclusiontable ) {
		exclusions = new HashSet(exclusiontable.keySet());
	}
}


