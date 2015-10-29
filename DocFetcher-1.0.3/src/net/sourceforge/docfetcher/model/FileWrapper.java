/*******************************************************************************
 * Copyright (c) 2008 Tran Nam Quang.
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

import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.parse.ParseException;
import net.sourceforge.docfetcher.parse.Parser;
import net.sourceforge.docfetcher.parse.ParserRegistry;

/**
 * A represenation of an indexable file object.
 * 
 * @author Tran Nam Quang
 */
public class FileWrapper extends Indexable {
	
	static final long serialVersionUID = 1;
	
	protected long lastModified;
	
	/**
	 * 
	 * @param parent The parent representation object.
	 * @param file The file represented by this object.
	 */
	public FileWrapper(Scope parent, File file) {
		super(parent, file);
		if (parent == null)
			throw new IllegalArgumentException();
		lastModified = file.lastModified();
	}
	
	/**
	 * Returns whether the file represented by this object has been modified
	 * after the creation of the latter.
	 */
	public boolean isModified() {
		return ! file.exists() ||
		lastModified != file.lastModified() ||
		! ParserRegistry.canParseIn(parent.getRootScope(), file);
	}
	
	/**
	 * Parses the file represented by this object and returns the parse result as
	 * a <tt>Document</tt>.
	 * <br>
	 * Note: This method may throw an OutOfMemoryError if the parsed file is too
	 * big.
	 */
	public Document parse() throws ParseException {
		Parser parser = ParserRegistry.getSingleFileParser(file);
		if (parser == null)
			throw new IllegalStateException("Cannot find parser for this file: " + file.getAbsolutePath()); //$NON-NLS-1$
		try {
			Document doc = parser.parse(file);
			doc.setParsedBy(parser);
			return doc;
		} catch (RuntimeException e) {
			throw new ParseException(file, Msg.parser_error.value());
		}
	}

}
