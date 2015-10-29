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

/**
 * An exception indicating that a file move/copy operation failed because the
 * destination file already exists.
 * 
 * @author Tran Nam Quang
 */
public class FileAlreadyExistsException extends Exception {
	
	static final long serialVersionUID = 1;
	
	/**
	 * The existing destination file that caused the move/copy operation to
	 * fail.
	 */
	private File file;
	
	/**
	 * @param file
	 *            The existing destination file that caused the move/copy
	 *            operation to fail
	 * @param msg
	 *            An optional error message
	 */
	public FileAlreadyExistsException(File file, String msg) {
		super(msg);
		this.file = file;
	}
	
	/**
	 * Returns the existing destination file that caused the move/copy operation
	 * to fail.
	 */
	public File getFile() {
		return file;
	}

}
