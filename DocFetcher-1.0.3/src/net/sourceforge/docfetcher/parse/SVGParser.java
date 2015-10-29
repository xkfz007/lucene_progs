/*******************************************************************************
 * Copyright (c) 2009 Tran Nam Quang.
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

import net.htmlparser.jericho.CharacterReference;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.model.Document;

/**
 * @author Tran Nam Quang
 */
public class SVGParser extends Parser {

	private String[] extensions = new String[] {"svg"}; //$NON-NLS-1$
	
	public String[] getExtensions() {
		return extensions;
	}

	public String getFileType() {
		return Msg.filetype_svg.value();
	}
	
	public Document parse(File file) throws ParseException {
		try {
			InputStream in = new FileInputStream(file);
			Source source = new Source(in);
			in.close();
			source.setLogger(null);
			String title = getElementContent(source, "dc:title"); //$NON-NLS-1$
			String author = getElementContent(source, "dc:creator"); //$NON-NLS-1$
			return new Document(file, title, source.getTextExtractor().toString()).addAuthor(author);
		} catch (FileNotFoundException e) {
			throw new ParseException(file, Msg.file_not_found.value());
		} catch (IOException e) {
			throw new ParseException(file, Msg.file_not_readable.value());
		}
	}

	public String renderText(File file) throws ParseException {
		try {
			InputStream in = new FileInputStream(file);
			Source source = new Source(in);
			in.close();
			source.setLogger(null);
			return source.getTextExtractor().toString();
		}
		catch (FileNotFoundException e) {
			throw new ParseException(file, Msg.file_not_found.value());
		}
		catch (IOException e) {
			throw new ParseException(file, Msg.file_not_readable.value());
		}
	}
	
	/**
	 * Returns the textual content inside the given HTML element from the given
	 * HTML source. Returns null if the HTML element is not found.
	 */
	private String getElementContent(Source source, String elementName) {
		Element el = source.getNextElement(0, elementName);
		return el == null ? null : CharacterReference.decode(el.getTextExtractor().toString());
	}

}
