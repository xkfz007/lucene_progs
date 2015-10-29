/*******************************************************************************
 * Copyright (c) 2007, 2008 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.model;

import java.io.File;

import net.sourceforge.docfetcher.util.UtilFile;

import org.apache.lucene.search.Query;

/**
 * A <code>net.sourceforge.docfetcher.model.Document</code> with an additional
 * score field.
 * 
 * @author Tran Nam Quang
 */
public class ResultDocument extends Document implements Comparable<ResultDocument> {
	
	/**
	 * The score achieved in the search.
	 */
	private float score;
	
	/**
	 * The title of the document, or null if none has been set. This field is
	 * used as a cache so we don't have to call get(String) on the Lucene
	 * document each time.
	 */
	private String title;
	
	/** The query used to obtain this result object. */
	private Query query;
	
	/**
	 * @param doc
	 *            The Lucene document returned by a Lucene search.
	 * @param score
	 *            The score achieved in the search.
	 */
	public ResultDocument(org.apache.lucene.document.Document doc, float score, Query query) {
		luceneDoc = doc;
		this.score = score;
		this.query = query;
		
		/*
		 * The call to UtilFile.normPathSep(..) is needed here because the
		 * stored path could have been generated in a different environment
		 * (Windows/Linux).
		 */
		file = new File(UtilFile.normPathSep(doc.get(path)));
		
		title = doc.get(Document.title);
	}
	
	/**
	 * Returns the score achieved in a Lucene search.
	 */
	public float getScore() {
		return score;
	}
	
	public Query getQuery() {
		return query;
	}
	
	public String getTitle() {
		return title;
	}
	
	/**
	 * Returns 0 if this ResultDocument has the same score as the given
	 * ResultDocument. Returns -1 if this ResultDocument has a <b>higher</b>
	 * score, and 1 if it has a <b>lower</b> score. This inversion is used to
	 * achieve descending instead of ascending sorting order.
	 */
	public int compareTo(ResultDocument o) {
		return score == o.score ? 0 : (score < o.score ? 1 : -1);
	}
	
}
