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

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.parse.Parser;
import net.sourceforge.docfetcher.parse.ParserRegistry;
import net.sourceforge.docfetcher.util.UtilFile;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

/**
 * An object representation of a parsed document file.
 * 
 * @author Tran Nam Quang
 */
public class Document {
	
	/**
	 * Lucene field name for the filename of a <tt>Document</tt> file.
	 */
	public static final String filename = "filename"; //$NON-NLS-1$
	
	/**
	 * Lucene field name for the absolute path of a <tt>Document</tt> file.
	 */
	public static final String path = "path"; //$NON-NLS-1$
	
	/**
	 * Lucene field name for the author of a <tt>Document</tt>.
	 */
	public static final String author = "author"; //$NON-NLS-1$
	
	/**
	 * Lucene field name for the title of a <tt>Document</tt>.
	 */
	public static final String title = "title"; //$NON-NLS-1$
	
	/**
	 * Lucene field name for the contents of a <tt>Document</tt>.
	 */
	public static final String contents = "contents"; //$NON-NLS-1$
	
	/**
	 * Lucene field name for the 'last modified' timestamp of the
	 * <tt>Document</tt> file.
	 */
	public static final String lastModified = "lastModified"; //$NON-NLS-1$
	
	public static final String parsedBy = "parsedBy"; //$NON-NLS-1$
	
	/**
	 * The internal Lucene <tt>Document</tt>.
	 */
	protected org.apache.lucene.document.Document luceneDoc;
	
	/**
	 * The document file.
	 */
	protected File file;
	
	protected Document() {
		// This constructor is for subclassers only!
	}
	
	/**
	 * Constructs a new <tt>Document</tt> with the given contents that
	 * represents the given file, which must not be null. The filename without
	 * extension is used as the default title of the <tt>Document</tt>. To
	 * specify another title, use <tt>setTitle(String)</tt>.
	 */
	public Document(File file, String contents) {
		this(file, getDefaultTitle(file), contents);
	}
	
	/**
	 * Constructs a new <tt>Document</tt> with the given title and contents that
	 * represents the given file, which must not be null. If the title parameter
	 * is null or an empty string, the default title (filename without
	 * extension) will be used. To explicitly set the title to an empty string,
	 * use <tt>setTitle(String)</tt>.
	 */
	public Document(File file, String title, String contents) {
		luceneDoc = new org.apache.lucene.document.Document();
		setFile(file);
		if (title == null || title.equals("")) //$NON-NLS-1$
			title = getDefaultTitle(file);
		setTitle(title);
		setContents(contents);
	}
	
	/**
	 * Constructs a new <tt>Document</tt> with the given contents that
	 * represents the given file, which must not be null. The filename without
	 * extension is used as the default title of the <tt>Document</tt>. To
	 * specify another title, use <tt>setTitle(String)</tt>.
	 */
	public Document(File file, StringBuffer contents) {
		this(file, getDefaultTitle(file), contents);
	}
	
	/**
	 * Constructs a new <tt>Document</tt> with the given title and contents that
	 * represents the given file, which must not be null. If the title parameter
	 * is null or an empty string, the default title (filename without
	 * extension) will be used. To explicitly set the title to an empty string,
	 * use <tt>setTitle(String)</tt>.
	 */
	public Document(File file, String title, StringBuffer contents) {
		luceneDoc = new org.apache.lucene.document.Document();
		setFile(file);
		if (title == null || title.equals("")) //$NON-NLS-1$
			title = getDefaultTitle(file);
		setTitle(title);
		setContents(contents);
	}
	
	/**
	 * Returns the file represented by this <tt>Document</tt>.
	 */
	public File getFile() {
		return file;
	}
	
	/**
	 * Sets the file represented by this <tt>Document</tt>. Must not be null.
	 * Returns the receiver for convenience.
	 */
	public Document setFile(File file) {
		if (file == null)
			throw new IllegalArgumentException("The document file must not be null."); //$NON-NLS-1$
		this.file = UtilFile.getRelativeFile(Const.USER_DIR_FILE, file);
		luceneDoc.removeFields(lastModified);
		luceneDoc.removeFields(path);
		luceneDoc.removeFields(filename);
		luceneDoc.add(new Field(lastModified, String.valueOf(file.lastModified()), Store.YES, Index.NO));
		luceneDoc.add(new Field(path, UtilFile.getRelativePath(file), Store.YES, Index.NO));
		
		String basename = UtilFile.getNameNoExt(file);
		luceneDoc.add(new Field(filename, basename, Store.NO, Index.ANALYZED));
		luceneDoc.add(new Field(Document.contents, basename, Store.NO, Index.ANALYZED));
		return this;
	}
	
	/**
	 * Adds an author to this <tt>Document</tt>. If the given string is null, no author
	 * will be added. Returns the receiver for convenience.
	 */
	public Document addAuthor(String author) {
		if (author == null) return this;
		luceneDoc.add(new Field(Document.author, author, Store.YES, Index.ANALYZED));
		return this;
	}
	
	/**
	 * Removes all author fields if any exist. Returns the receiver for convenience.
	 */
	public Document removeAuthors() {
		luceneDoc.removeFields(author);
		 return this;
	}
	
	/**
	 * Returns the <i>first</i> author field that was added, or null, if none has been set.
	 */
	public String getAuthor() {
		return luceneDoc.get(author);
	}
	
	/**
	 * Returns the title of this <tt>Document</tt>, or null if none has been set.
	 */
	public String getTitle() {
		return luceneDoc.get(title);
	}
	
	/**
	 * Sets the title of the <tt>Document</tt>. If the title is null, the title will be
	 * set to an empty string. Returns the receiver for convenience.
	 */
	public Document setTitle(String title) {
		if (title == null) title = ""; //$NON-NLS-1$
		luceneDoc.removeFields(Document.title);
		luceneDoc.add(new Field(Document.title, title, Store.YES, Index.ANALYZED));
		return this;
	}
	
	/**
	 * Returns the contents of this <tt>Document</tt>, possibly an empty string.
	 */
	public String getContents() {
		return luceneDoc.get(contents);
	}
	
	/**
	 * Sets the contents of the <tt>Document</tt>. If the given StringBuffer is null, the
	 * contents field will be set to an empty string. Returns the receiver for convenience.
	 */
	public Document setContents(StringBuffer contents) {
		// Don't remove the content field here, because it's used for the filename
//		luceneDoc.removeFields(Document.contents);
		if (contents == null)
			luceneDoc.add(new Field(Document.contents, "", Store.NO, Index.ANALYZED)); //$NON-NLS-1$
		else
			luceneDoc.add(new Field(Document.contents, contents.toString(), Store.NO, Index.ANALYZED));
		return this;
	}

	/**
	 * Sets the contents of this <tt>Document</tt>. If the given String is null, the
	 * contents field will be set to an empty string. Returns the receiver for convenience.
	 */
	public Document setContents(String contents) {
		if (contents == null)
			contents = ""; //$NON-NLS-1$
		// Don't remove the content field here, because it's used for the filename
//		luceneDoc.removeFields(Document.contents);
		luceneDoc.add(new Field(Document.contents, contents, Store.NO, Index.ANALYZED));
		return this;
	}
	
	/**
	 * Returns the default title for the given file, which is the filename
	 * without extension.
	 */
	public static String getDefaultTitle(File file) {
		String title = UtilFile.getNameNoExt(file);
		if (title.equals("")) //$NON-NLS-1$
			title = file.getName();
		return title;
	}
	
	/**
	 * Returns the underlying Lucene Document. For internal use only!
	 */
	org.apache.lucene.document.Document getLuceneDoc() {
		return luceneDoc;
	}
	
	/**
	 * Returns the parser that produced this document object. It will not return
	 * null.
	 */
	public Parser getParser() {
		String parserName = luceneDoc.get(parsedBy);
		for (Parser candidate : ParserRegistry.getParsers())
			if (candidate.getClass().getSimpleName().equals(parserName))
				return candidate;
		throw new IllegalStateException();
	}
	
	/**
	 * Returns the name of the parser that produced this document object. This
	 * method is faster than {@link #getParser()}, because instead of performing
	 * a search, only a stored string is returned. This method will not return
	 * null.
	 */
	public String getParserName() {
		return luceneDoc.get(parsedBy);
	}
	
	/**
	 * Sets the parser that produced this document object. Null is not allowed.
	 */
	void setParsedBy(Parser parser) {
		if (parser == null)
			throw new IllegalArgumentException();
		luceneDoc.add(new Field(parsedBy, parser.getClass().getSimpleName(), Store.YES, Index.NO));
	}
	
	public boolean equals(Object obj) {
		if (obj != null && (obj instanceof Document))
			return ((Document) obj).file.equals(file);
		return false;
	}
	
	public String toString() {
		return file.toString();
	}
	
}
