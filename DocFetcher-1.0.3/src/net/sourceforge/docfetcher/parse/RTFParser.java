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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.rtf.RTFEditorKit;

import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.model.Document;

/**
 * @author Tran Nam Quang
 */
public class RTFParser extends Parser {
	
	private static final String[] extensions = new String[] {"rtf"}; //$NON-NLS-1$

	public Document parse(File file) throws ParseException {
		return new Document(file, renderText(file));
	}
	
	public String renderText(File file) throws ParseException {
		FileReader reader = null;
		try {
			reader = new FileReader(file);
			DefaultStyledDocument doc = new DefaultStyledDocument();
			new RTFEditorKit().read(reader, doc, 0);
			return doc.getText(0, doc.getLength());
		}
		catch (FileNotFoundException e) {
			throw new ParseException(file, Msg.file_not_found.value());
		}
		catch (IOException e) {
			throw new ParseException(file, Msg.file_not_readable.value());
		}
		catch (BadLocationException e) {
			throw new ParseException(file, Msg.file_not_readable.value());
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public String[] getExtensions() {
		return extensions;
	}

	public String getFileType() {
		return Msg.filetype_rtf.value();
	}

}
