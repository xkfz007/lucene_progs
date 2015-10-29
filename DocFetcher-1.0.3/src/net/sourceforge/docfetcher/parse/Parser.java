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

package net.sourceforge.docfetcher.parse;

import java.io.File;

import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.model.Document;
import net.sourceforge.docfetcher.util.Event;

/**
 * A class that parses files and has a check state. 
 * 
 * @author Tran Nam Quang
 */
public abstract class Parser implements Comparable<Parser> {
	
	/**
	 * Event: Changes in the check state of this parser.
	 */
	public final Event<Parser> evtCheckStateChanged = new Event<Parser> ();
	
	/**
	 * Creates a document object from the given file. By default, this method
	 * makes a call to <tt>renderText</tt>. Subclassers may reimplement it for
	 * efficiency reasons. The output does not need be in a humanly readable
	 * format, since all of it will be digested by the indexing engine anyway.
	 * 
	 * @throws ParseException
	 *             if the parse process failed.
	 */
	public Document parse(File file) throws ParseException {
		return new Document(file, renderText(file));
	}
	
	/**
	 * Creates a String object from the given file. The returned string should
	 * be in a humanly readable format (e.g. with line breaks), since it will be
	 * shown in the text preview panel.
	 * 
	 * @throws ParseException
	 *             if the parse process failed.
	 */
	public abstract String renderText(File file) throws ParseException;
	
	/**
	 * Returns a short, descriptive term for the type of files supported by this
	 * parser, e.g. "HTML", "Plain Text" or "PDF". This is what the user will
	 * see in the file type panel.
	 */
	public abstract String getFileType();
	
	/**
	 * Returns an array of file extensions supported by this parser. Subclassers
	 * are advised to return a cached string array if possible, instead of
	 * creating a new one on each call.
	 */
	public abstract String[] getExtensions();
	
	/**
	 * Returns whether this parser is checked in the file type panel.
	 */
	public boolean isChecked() {
		return Pref.isChecked(getClass());
	}
	
	/**
	 * Sets whether this parser should be checked in the file type panel.
	 */
	public void setChecked(boolean checked) {
		Pref.setChecked(getClass(), checked);
		evtCheckStateChanged.fireUpdate(this);
	}
	
	public int compareTo(Parser o) {
		return getFileType().compareToIgnoreCase(o.getFileType());
	}
	
}
