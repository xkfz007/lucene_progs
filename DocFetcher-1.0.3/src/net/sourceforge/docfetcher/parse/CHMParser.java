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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import net.htmlparser.jericho.Source;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.model.Document;

import org.chm4j.ChmEntry;
import org.chm4j.ChmFile;

/**
 * @author Tran Nam Quang
 */
public class CHMParser extends Parser {

	private static final String[] extensions = new String[] {"chm"}; //$NON-NLS-1$

	public Document parse(File file) throws ParseException {
		StringBuffer contents = new StringBuffer();
		try {
			ChmFile chmFile = new ChmFile(file);
			ChmEntry[] entries = chmFile.entries(ChmEntry.Attribute.ALL);
			for (ChmEntry entry : entries)
				append(contents, entry, false);
		}
		catch (FileNotFoundException e) {
			throw new ParseException(file, Msg.file_not_found.value());
		}
		catch (IOException e) {
			throw new ParseException(file, Msg.file_not_readable.value());
		}
		return new Document(file, contents);
	}

	public String renderText(File file) throws ParseException {
		StringBuffer contents = new StringBuffer();
		try {
			ChmFile chmFile = new ChmFile(file);
			ChmEntry[] entries = chmFile.entries(ChmEntry.Attribute.ALL);
			for (ChmEntry entry : entries)
				append(contents, entry, true);
		}
		catch (FileNotFoundException e) {
			throw new ParseException(file, Msg.file_not_found.value());
		}
		catch (IOException e) {
			throw new ParseException(file, Msg.file_not_readable.value());
		}
		return contents.toString();
	}

	/**
	 * Converts all <tt>ChmEntry</tt>s under <tt>entry</tt> to strings and
	 * puts them into the given <tt>StringBuffer</tt>.
	 * 
	 * @param renderText
	 *            Whether the textual contents of the <tt>ChmEntry</tt>s
	 *            should be extracted in a readable format (true) or as raw
	 *            strings (false).
	 */
	private void append(StringBuffer sb, ChmEntry entry, boolean renderText) throws IOException {
		if (entry.hasAttribute(ChmEntry.Attribute.DIRECTORY)) {
			for (ChmEntry child : entry.entries(ChmEntry.Attribute.ALL))
				append(sb, child, renderText);
		}
		else {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(
						new InputStreamReader(
								entry.getInputStream(),
								"utf8" // Just guessing... //$NON-NLS-1$
						)
				);
				StringBuilder entryBuffer = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null)
					entryBuffer.append(line).append("\n\n"); //$NON-NLS-1$

				/*
				 * The current version of chm4j doesn't allow differentiating
				 * between binary files (such as images) and HTML files. Therefore
				 * we use regex matching to select the HTML files.
				 */
				if (isHTML(entryBuffer)) {
					Source source = new Source(entryBuffer);
					source.setLogger(null);
					if (renderText)
						sb.append(source.getRenderer().setIncludeHyperlinkURLs(false).toString());
					else
						sb.append(source.getTextExtractor().toString());
				}
			}
			catch (RuntimeException e) {
				// The HTML lib can do this to us; do nothing
			}
			finally {
				if (reader != null)
					reader.close();
			}
		}
	}
	
	/**
	 * Returns true if the given StringBuilder appears to contain HTML. This is
	 * determined by parsing the input with a simple finite state machine that
	 * checks whether the input contains an html start tag, followed by an html
	 * end tag.
	 */
	private boolean isHTML(StringBuilder input) {
		final int OUTSIDE = 0;
		final int INSIDE = 1;
		int state = OUTSIDE;
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (state == OUTSIDE) {
				/*
				 * Note that we're checking for the occurrence of <html, not
				 * <html>, since in some HTML documents the html start tag
				 * contains additional attributes, e.g. <html attr="value">.
				 */
				if (c == 'l' || c == 'L') { // last char in 'html'
					if (i >= 4) {
						String substring = input.substring(i - 4, i + 1);
						if (substring.toLowerCase().equals("<html"))
							state = INSIDE;
					}
				}
			}
			else if (state == INSIDE) {
				if (c == '>') {
					String substring = input.substring(i - 6, i + 1);
					if (substring.toLowerCase().equals("</html>"))
						return true;
				}
			}
		}
		return false;
	}

	public String[] getExtensions() {
		return extensions;
	}

	public String getFileType() {
		return Msg.filetype_chm.value();
	}
	
}
