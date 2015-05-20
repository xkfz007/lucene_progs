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
package org.apache.lucene.spatial.tier;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.OpenBitSet;

/**
 * <p><font color="red"><b>NOTE:</b> This API is still in
 * flux and might change in incompatible ways in the next
 * release.</font>
 */
public class CartesianShapeFilter extends Filter {
 
  private final Shape shape;
  private final String fieldName;
  
  CartesianShapeFilter(final Shape shape, final String fieldName){
    this.shape = shape;
    this.fieldName = fieldName;
  }
  
  @Override
  public DocIdSet getDocIdSet(final IndexReader reader) throws IOException {
    final OpenBitSet bits = new OpenBitSet(reader.maxDoc());
    final TermDocs termDocs = reader.termDocs();
    final List<Double> area = shape.getArea();
    int sz = area.size();
    
    final Term term = new Term(fieldName);
    // iterate through each boxid
    for (int i =0; i< sz; i++) {
      double boxId = area.get(i).doubleValue();
      termDocs.seek(term.createTerm(NumericUtils.doubleToPrefixCoded(boxId)));
      // iterate through all documents
      // which have this boxId
      while (termDocs.next()) {
        bits.fastSet(termDocs.doc());
      }
    }
    return bits;
  }
}
