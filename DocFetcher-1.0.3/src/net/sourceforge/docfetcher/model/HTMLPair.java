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
import java.io.FileFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.parse.HTMLParser;
import net.sourceforge.docfetcher.parse.ParseException;
import net.sourceforge.docfetcher.parse.ParserRegistry;
import net.sourceforge.docfetcher.util.UtilFile;
import net.sourceforge.docfetcher.util.UtilList;

/**
 * A pair of an HTML file and its associated folder.
 * 
 * @author Tran Nam Quang
 */
public class HTMLPair extends FileWrapper {
	
	static final long serialVersionUID = 1;
	
	private Modifiable folderWrapper;
	
	/**
	 * @param htmlFile The HTML file. Must not be null.
	 * @param htmlFolder The folder associated with the HTML file. Can be null.
	 */
	public HTMLPair(Scope parent, File htmlFile, File htmlFolder) {
		super(parent, htmlFile);
		setHtmlFolder(htmlFolder);
	}

	/**
	 * Returns the HTML folder of this HTML pair. Returns null if the HTML file
	 * does not have an attached folder.
	 */
	public File getHtmlFolder() {
		if (folderWrapper == null)
			return null;
		return folderWrapper.getFile();
	}

	public void setHtmlFolder(File htmlFolder) {
		if (htmlFolder != null)
			folderWrapper = new Modifiable(parent.getRootScope(), htmlFolder);
	}

	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof HTMLPair))
			return false;
		HTMLPair htmlPair = (HTMLPair) obj;
		File thisFolder = getHtmlFolder();
		File oFile = htmlPair.file;
		File oFolder = htmlPair.getHtmlFolder();
		boolean fileMatch = file == null ? oFile == null : file.equals(oFile);
		boolean folderMatch = thisFolder == null ? oFolder == null : thisFolder.equals(oFolder);
		return fileMatch && folderMatch;
	}
	
	/**
	 * Returns whether the HTML pair has been modified. This method checks
	 * whether the HTML file is missing or has been modified, whether the HTML
	 * folder is missing and whether indexable files inside the HTML folder have
	 * been inserted, modified or deleted. <br>
	 * Note: This method does not check whether a single HTML file got a new
	 * attached folder. This check must be done elsewhere.
	 */
	public boolean isModified() {
		if (super.isModified())
			return true;
		if (folderWrapper != null)
			return folderWrapper.isModified(); // This also checks whether the folder exists
		return false;
	}
	
	/**
	 * Note: This method may throw an OutOfMemoryError if the parsed file is too
	 * big.
	 */
	public Document parse() throws ParseException {
		HTMLParser htmlParser = ParserRegistry.getHTMLParser();
		Document doc = htmlParser.parse(file, getHtmlFolder());
		doc.setParsedBy(htmlParser);
		return doc;
	}

}

//Recursive structure that keeps track of the modified states of all files under it
class Modifiable implements Serializable {
	
	static final long serialVersionUID = 2;
	
	private RootScope root;
	private File file;
	private long lastModified = -1;
	private List<Modifiable> children = null;
	
	Modifiable (RootScope root, File file) {
		this.root = root;
		this.file = UtilFile.getRelativeFile(Const.USER_DIR_FILE, file);
		if (file.isDirectory()) {
			children = new ArrayList<Modifiable> ();
			for (File subFile : UtilFile.listAll(file))
				if (subFile.isDirectory() ||
						ParserRegistry.canParseIn(root, subFile))
					children.add(new Modifiable(root, subFile));
		}
		else this.lastModified = file.lastModified();
	}
	
	public File getFile() {
		return file;
	}
	
	public boolean isModified() {
		// Modified if file/directory doesn't exist anymore
		if (! file.exists())
			return true;
		// File: Modified if the last modified field of the file has changed
		if (children == null)
			return file.lastModified() != lastModified;
		// Directory: Modified if the directory's children have changed
		for (Modifiable child : children)
			if (child.isModified())
				return true;
		// Directory: Modified if new files have been inserted
		File[] newFiles = UtilFile.listAll(file, new FileFilter() {
			public boolean accept(File candidate) {
				return candidate.isDirectory() ||
				ParserRegistry.canParseIn(root, candidate);
			}
		});
		Modifiable[] oldFiles = children.toArray(new Modifiable[children.size()]);
		UtilList.Equality<File, Modifiable> mapper = new UtilList.Equality<File, Modifiable> () {
			public boolean equals(File file, Modifiable modif) {
				return modif.file.equals(file);
			}
		};
		return ! UtilList.isMap(newFiles, oldFiles, mapper);
	}
	
}