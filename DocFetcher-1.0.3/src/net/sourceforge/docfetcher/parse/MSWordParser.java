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

import net.sourceforge.docfetcher.enumeration.Msg;

import org.apache.poi.hwpf.extractor.WordExtractor;

/**
 * @author Tran Nam Quang
 */
public class MSWordParser extends MSOfficeParser {
	
	private static final String[] extensions = new String[] {"doc"}; //$NON-NLS-1$
	
	public String renderText(File file) throws ParseException {
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			WordExtractor extractor = null;
			try {
				extractor = new WordExtractor(in);
			}
			catch (Exception e) {
				// This can happen if the file has the "doc" extension, but is not a Word document
				throw new ParseException(file, Msg.file_corrupted.value());
			}
			finally {
				in.close();
			}
			return extractor.getText();
		}
		catch (FileNotFoundException e) {
			throw new ParseException(file, Msg.file_not_found.value());
		}
		catch (IOException e) {
			throw new ParseException(file, Msg.file_not_readable.value());
		}
	}

	public String[] getExtensions() {
		return extensions;
	}

	public String getFileType() {
		return Msg.filetype_doc.value();
	}

}
