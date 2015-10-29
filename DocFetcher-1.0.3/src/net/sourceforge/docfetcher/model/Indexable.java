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
import java.io.Serializable;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.util.UtilFile;

/**
 * A representation of a file from the document repository. It is used as a
 * "snapshot" and stored on disk so that changes in the document repository can
 * be detected later.
 * 
 * @author Tran Nam Quang
 */
public class Indexable implements Serializable, Comparable<Indexable> {
	
	static final long serialVersionUID = 2;
	
	/**
	 * The parent representation object.
	 */
	protected Scope parent;

	/**
	 * The file in the document repository that this object represents. 
	 */
	protected File file;

	/**
	 * @param parent The parent representation object
	 * @param file The file represented by this object
	 */
	public Indexable(Scope parent, File file) {
		this.file = UtilFile.getRelativeFile(Const.USER_DIR_FILE, file);
		this.parent = parent;
		if (file == null)
			throw new IllegalArgumentException("The file must not be null."); //$NON-NLS-1$
	}
	
	/**
	 * @return The parent representation object
	 */
	public Scope getParent() {
		return parent;
	}
	
	/**
	 * @param The parent representation object
	 */
	public void setParent(Scope parent) {
		this.parent = parent;
	}

	/**
	 * @return The file represented by this object
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @param The file represented by this object
	 */
	public void setFile(File file) {
		this.file = UtilFile.getRelativeFile(Const.USER_DIR_FILE, file);
	}

	public int compareTo(Indexable other) {
		return file.compareTo(other.file);
	}

	public boolean equals(Object obj) {
		if (obj != null && (obj instanceof Indexable))
			if (((Indexable) obj).file.equals(file))
				return true;
		return false;
	}

	public String toString() {
		return file.getAbsolutePath();
	}

}