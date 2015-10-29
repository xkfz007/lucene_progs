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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;

/**
 * @author Tran Nam Quang
 */
public class TextParser extends Parser {
	
	private String[] extensions = Pref.StrArray.TextExtensions.getValue();
	
	public String renderText(File file) throws ParseException {
		Reader reader = null;
		try {
			StringBuffer sb = new StringBuffer();
			int c;
			reader = new FileReader(file); 
			reader = new BufferedReader(reader);
			while ((c = reader.read()) != -1)
				sb.append((char)c);
			return sb.toString();
		}
		catch (FileNotFoundException e) {
			throw new ParseException(file, Msg.file_not_found.value());
		}
		catch (IOException e) {
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
	
	public String getFileType() {
		return Msg.filetype_txt.value();
	}
	
	public String[] getExtensions() {
		return extensions;
	}
	
	/**
	 * Sets the file extensions used to identify text files.
	 */
	public void setExtensions(String[] extensions) {
		this.extensions = extensions;
	}

}