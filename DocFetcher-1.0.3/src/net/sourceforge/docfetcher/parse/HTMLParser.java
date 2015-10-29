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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.IllegalCharsetNameException;

import net.htmlparser.jericho.CharacterReference;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.model.Document;
import net.sourceforge.docfetcher.util.UtilFile;

/**
 * @author Tran Nam Quang
 */
public class HTMLParser extends Parser {

	private String[] extensions = Pref.StrArray.HTMLExtensions.getValue();

	public Document parse(File file) throws ParseException {
		return parse(file, null);
	}

	/**
	 * Parses a pair of an HTML file and its associated folder. The latter might
	 * be null.
	 * 
	 * @throws ParseException
	 *             if the parse process failed.
	 */
	public Document parse(File htmlFile, File htmlFolder) throws ParseException {
		/*
		 * The path has to be converted to a URI first because some special
		 * characters (like '#') in that path can cause the HTML parser to fail.
		 * The URI conversion will replace those characters with percent
		 * encoding, which the parser seems to like better.
		 */
		String filepath = htmlFile.toURI().toString();

		// Get an HTML source
		Source source = null;
		try {
			source = new Source(new URL(filepath));
		} catch (IllegalCharsetNameException e) {
			throw new ParseException(htmlFile, Msg.unsupported_encoding.value());
		} catch (UnsupportedEncodingException e) {
			throw new ParseException(htmlFile, Msg.unsupported_encoding.value());
		} catch (IOException e) {
			throw new ParseException(htmlFile, Msg.file_not_readable.value());
		} catch (RuntimeException e) {
			throw new ParseException(htmlFile, Msg.file_not_readable.value());
		}
		source.setLogger(null);
		source.fullSequentialParse();

		// Get tags
		Element titleElement = source.getNextElement(0, HTMLElementName.TITLE);
		String[] metaData = new String[] {
				titleElement == null ? null : CharacterReference.decodeCollapseWhiteSpace(titleElement.getContent()),
						getMetaValue(source, "author"), //$NON-NLS-1$
						getMetaValue(source, "description"), //$NON-NLS-1$
						getMetaValue(source, "keywords"), //$NON-NLS-1$
		}; 

		// Get contents and append tags
		Element bodyElement = source.getNextElement(0, HTMLElementName.BODY);
		String contents = bodyElement == null ? "" : bodyElement.getContent().getTextExtractor().toString(); //$NON-NLS-1$
		StringBuffer sb = new StringBuffer(contents);
		for (String field : metaData)
			if (field != null)
				sb.append(" ").append(field); //$NON-NLS-1$

		// Process appended files if any exist, then return document
		if (htmlFolder != null)
			merge(sb, htmlFolder);
		return new Document(htmlFile, metaData[0], sb).addAuthor(metaData[1]);
	}

	/**
	 * Recursively parses the documents in the specified directory and adds
	 * their contents to the given StringBuffer.
	 */
	private void merge(StringBuffer sb, File directory) {
		File[] files = UtilFile.listAll(directory);
		for (File file : files) {
			if (file.isFile()) {
				// Parse file and append its contents to given StringBuffer
				try {
					Document appendedDoc = null;
					if (ParserRegistry.isHTMLFile(file))
						appendedDoc = ParserRegistry.getHTMLParser().parse(file, null);
					else
						appendedDoc = ParserRegistry.getSingleFileParser(file).parse(file);
					sb.append(" ").append(appendedDoc.getContents()); //$NON-NLS-1$
				} catch (Exception ex) {
					// Ignore all exceptions
				}
			}
			else if (file.isDirectory() && ! UtilFile.isSymLink(file))
				merge(sb, file); // recursive call
		}
	}

	/**
	 * Returns the file extensions used to identify HTML files.
	 */
	public String[] getExtensions() {
		return extensions;
	}

	/**
	 * Sets the file extensions used to identify HTML files.
	 */
	public void setExtensions(String[] extensions) {
		this.extensions = extensions;
	}

	public String getFileType() {
		return Msg.filetype_html.value();
	}

	/**
	 * Returns the value of the meta tag with the given name in the specified
	 * HTML source. Returns null if the meta tag does not exist.
	 */
	private String getMetaValue(Source source, String key) {
		int pos = 0;
		while(pos < source.length()) {
			StartTag startTag = source.getNextStartTag(pos, "name", key, false); //$NON-NLS-1$
			if (startTag == null) return null;
			if (startTag.getName() == HTMLElementName.META)
				return startTag.getAttributeValue("content"); //$NON-NLS-1$
			pos = startTag.getEnd();
		}
		return null;
	}

	public String renderText(File file) throws ParseException {
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			Source source = new Source(in);
			source.setLogger(null);
			return source.getRenderer().setIncludeHyperlinkURLs(false).toString();
		}
		catch (FileNotFoundException e) {
			throw new ParseException(file, Msg.file_not_found.value());
		}
		catch (IOException e) {
			throw new ParseException(file, Msg.file_not_readable.value());
		}
		finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

}
