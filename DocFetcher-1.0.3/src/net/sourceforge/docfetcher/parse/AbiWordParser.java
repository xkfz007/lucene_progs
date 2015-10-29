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

package net.sourceforge.docfetcher.parse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.model.Document;
import net.sourceforge.docfetcher.util.UtilFile;

/**
 * @author Tran Nam Quang
 */
public class AbiWordParser extends Parser {

	private String[] extensions = new String[] {"abw", "abw.gz", "zabw"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	public Document parse(File file) throws ParseException {
		Source source = getSource(file);
		String author = getMetaData(source, "dc.creator"); //$NON-NLS-1$
		String title = getMetaData(source, "dc.title"); //$NON-NLS-1$
		String contents = source.getTextExtractor().toString(); // Includes metadata
		return new Document(file, title, contents).addAuthor(author);
	}

	/**
	 * Returns the value of the given metadata key in the given <tt>Source</tt>,
	 * or null if the key-value-pair was not found.
	 */
	private String getMetaData(Source source, String key) {
		Element metaElement = source.getNextElement(0, "key", key, false); //$NON-NLS-1$
		if (metaElement == null) return null;
		return metaElement.getTextExtractor().toString();
	}

	public String renderText(File file) throws ParseException {
		Source source = getSource(file);
		
		// Find all top level elements, excluding the metadata element
		List<Element> topLevelNonMetaElements = new ArrayList<Element> ();
		int pos = source.getNextElement(0, "metadata").getEnd(); //$NON-NLS-1$
		while (pos < source.length()) {
			Element next = source.getNextElement(pos);
			if (next == null) break;
			topLevelNonMetaElements.add(next);
			pos = next.getEnd();
		}
		
		// Invoke renderer on all found elements, save output to stringbuffer
		StringBuffer sb = new StringBuffer();
		for (Element element : topLevelNonMetaElements)
			sb.append(element.getRenderer().toString());
		
		return sb.toString();
	}

	/**
	 * Returns a <tt>Source</tt> for the given AbiWord file.
	 */
	private Source getSource(File file) throws ParseException {
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			String ext = UtilFile.getExtension(file);
			if (ext.equals("zabw") || ext.equals("abw.gz")) //$NON-NLS-1$ //$NON-NLS-2$
				in = new GZIPInputStream(in);
			Source source = new Source(in);
			source.setLogger(null);
			source.fullSequentialParse();
			return source;
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

	public String[] getExtensions() {
		return extensions;
	}

	public String getFileType() {
		return Msg.filetype_abi.value();
	}

}
