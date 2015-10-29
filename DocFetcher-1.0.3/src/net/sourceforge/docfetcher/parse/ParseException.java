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

/**
 * An exception occuring while trying to parse files.
 * 
 * @author Tran Nam Quang
 */
public class ParseException extends Exception {
	
	static final long serialVersionUID = 1;
	
	/**
	 * The file the parse exception occured on.
	 */
	private File file;
	
	/**
	 * Constructs a new instance of this class with the file the parse exception
	 * occured on and a short message describing the reason of the failure.
	 */
	public ParseException(File file, String msg) {
		super(msg);
		this.file = file;
	}
	
	/**
	 * Returns the file the parse exception occured on.
	 */
	public File getFile() {
		return file;
	}

}
