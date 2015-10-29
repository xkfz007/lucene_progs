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
import java.util.LinkedHashMap;
import java.util.Vector;

import net.sourceforge.docfetcher.enumeration.Msg;

import org.libwpd.WPDocumentJava;
import org.libwpd.WPXDocumentJavaInterface;

/**
 * @author Tran Nam Quang
 */
public class WordPerfectParser extends Parser {

	private static final String[] extensions = new String[] {"wpd"}; //$NON-NLS-1$
	
	public String[] getExtensions() {
		return extensions;
	}

	public String getFileType() {
		return Msg.filetype_wpd.value();
	}

	public String renderText(File file) throws ParseException {
		try {
			String path = file.getAbsolutePath();
			if (! WPDocumentJava.isFileWordPerfectDocument(path))
				throw new ParseException(file, Msg.wordperfect_expected.value());
			WPListenerImpl listenerImpl = new WPListenerImpl();
			WPDocumentJava.parseFile(path, listenerImpl);
			return listenerImpl.sb.toString();
		}
		catch (UnsatisfiedLinkError e) {
			throw new ParseException(file, Msg.wordperfect_parser_not_installed.value());
		}
		catch (NoClassDefFoundError e) {
			throw new ParseException(file, Msg.wordperfect_parser_not_installed.value());
		}
	}
	
	private class WPListenerImpl implements WPXDocumentJavaInterface {
		
		public StringBuffer sb = new StringBuffer();
		
		public void insertText(String arg0) {
			sb.append(arg0).append(" "); //$NON-NLS-1$
		}
		
		public void insertTab() {
			sb.append("\t"); //$NON-NLS-1$
		}
		
		public void insertLineBreak() {
			sb.append("\n"); //$NON-NLS-1$
		}
		
		public void closeComment() {}
		public void closeEndnote() {}
		public void closeFooter() {}
		public void closeFootnote() {}
		public void closeFrame() {}
		public void closeHeader() {}
		public void closeListElement() {}
		public void closeOrderedListLevel() {}
		public void closePageSpan() {}
		public void closeParagraph() {}
		public void closeSection() {}
		public void closeSpan() {}
		public void closeTable() {}
		public void closeTableCell() {}
		public void closeTableRow() {}
		public void closeTextBox() {}
		public void closeUnorderedListLevel() {}
		public void defineOrderedListLevel(LinkedHashMap<String, String> arg0) {}
		public void defineUnorderedListLevel(LinkedHashMap<String, String> arg0) {}
		public void endDocument() {}
		public void insertBinaryObject(LinkedHashMap<String, String> arg0,
				byte[] arg1) {}
		public void insertCoveredTableCell(LinkedHashMap<String, String> arg0) {}
		public void openComment(LinkedHashMap<String, String> arg0) {}
		public void openEndnote(LinkedHashMap<String, String> arg0) {}
		public void openFooter(LinkedHashMap<String, String> arg0) {}
		public void openFootnote(LinkedHashMap<String, String> arg0) {}
		public void openFrame(LinkedHashMap<String, String> arg0) {}
		public void openHeader(LinkedHashMap<String, String> arg0) {}
		public void openListElement(LinkedHashMap<String, String> arg0,
				Vector<LinkedHashMap<String, String>> arg1) {}
		public void openOrderedListLevel(LinkedHashMap<String, String> arg0) {}
		public void openPageSpan(LinkedHashMap<String, String> arg0) {}
		public void openParagraph(LinkedHashMap<String, String> arg0,
				Vector<LinkedHashMap<String, String>> arg1) {}
		public void openSection(LinkedHashMap<String, String> arg0,
				Vector<LinkedHashMap<String, String>> arg1) {}
		public void openSpan(LinkedHashMap<String, String> arg0) {}
		public void openTable(LinkedHashMap<String, String> arg0,
				Vector<LinkedHashMap<String, String>> arg1) {}
		public void openTableCell(LinkedHashMap<String, String> arg0) {}
		public void openTableRow(LinkedHashMap<String, String> arg0) {}
		public void openTextBox(LinkedHashMap<String, String> arg0) {}
		public void openUnorderedListLevel(LinkedHashMap<String, String> arg0) {}
		public void setDocumentMetaData(LinkedHashMap<String, String> arg0) {}
		public void startDocument() {}
	}

}

