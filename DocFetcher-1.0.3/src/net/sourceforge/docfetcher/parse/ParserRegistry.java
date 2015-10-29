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

import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.model.RootScope;
import net.sourceforge.docfetcher.util.UtilFile;
import net.sourceforge.docfetcher.util.UtilList;

/**
 * A registry for managing registered parsers.
 * 
 * @author Tran Nam Quang
 */
public class ParserRegistry {

	private ParserRegistry() {
		// Static use only
	}

	/**
	 * The registered file parsers (excluding the HTML parser).
	 */
	private static Parser[] fileParsers = new Parser[] {
		textParser = new TextParser(),
			// TextParser must have higher priority since its file extensions can be customized
		new AbiWordParser(),
		new CHMParser(),
		new MSExcelParser(),
		new MSExcel2007Parser(),
		new MSPowerPointParser(),
		new MSPowerPoint2007Parser(),
		new MSVisioParser(),
		new MSWordParser(),
		new MSWord2007Parser(),
		new OOoWriterParser(),
		new OOoCalcParser(),
		new OOoDrawParser(),
		new OOoImpressParser(),
		new PDFParser(),
		new RTFParser(),
		new SVGParser(),
		//new WordPerfectParser() // Not supported on Windows, therefore disabled
	};

	// Additional reference to the text parser for faster access.
	private static TextParser textParser;

	/** The HTML parser. */
	private static HTMLParser htmlParser = new HTMLParser();

	/**
	 * Returns the parser that can handling the given file, taking the current
	 * text file extension setting into account and excluding the HTML parser.
	 * May return null if no appropriate parser is found.
	 */
	public static Parser getSingleFileParser(File file) {
		String ext = UtilFile.getExtension(file);
		for (Parser parser : fileParsers)
			if (UtilList.containsEquality(parser.getExtensions(), ext))
				return parser;
		return null;
	}

	/**
	 * Returns the HTML parser.
	 */
	public static HTMLParser getHTMLParser() {
		return htmlParser;
	}
	
	/**
	 * Returns the Text parser
	 */
	public static TextParser getTextParser() {
		return textParser;
	}

	/**
	 * Returns a parser that can handle the given file, or null if none
	 * is found. The selection of the parser is based on the default
	 * text and HTML file extensions.
	 */
	public static Parser getParser(File file) {
		String ext = UtilFile.getExtension(file);
		if (UtilList.containsEquality(Pref.StrArray.HTMLExtensions.getValue(), ext))
			return htmlParser;
		for (Parser parser : fileParsers)
			/*
			 * For text files, this will give the default text extensions
			 * because it is expected that callers have reset the text
			 * extensions after using the text parser.
			 */
			if (UtilList.containsEquality(parser.getExtensions(), ext))
				return parser;
		return null;
	}

	/**
	 * Returns whether the given file can be parsed using the customized text
	 * and HTML file extensions stored in <tt>scope</tt>.
	 */
	public static boolean canParseIn(RootScope scope, File file) {
		String ext = UtilFile.getExtension(file);
		if (UtilList.containsEquality(scope.getHtmlExtensions(), ext))
			return true;
		for (Parser parser : fileParsers) {
			String[] extensions; 
			if (parser == textParser)
				extensions = scope.getTextExtensions();
			else
				extensions = parser.getExtensions();
			if (UtilList.containsEquality(extensions, ext))
				return true;
		}
		return false;
	}

	/**
	 * Returns all registered parsers, including the HTML parser.
	 */
	public static Parser[] getParsers() {
		Parser[] parsers = new Parser[fileParsers.length + 1];
		System.arraycopy(fileParsers, 0, parsers, 0, fileParsers.length);
		parsers[parsers.length - 1] = htmlParser;
		return parsers;
	}

	/**
	 * Returns whether at least one of the registered parsers is checked.
	 */
	public static boolean hasCheckedParsers() {
		for (Parser parser : getParsers())
			if (parser.isChecked())
				return true;
		return false;
	}

	/**
	 * Returns whether the given file is an HTML file, using the current HTML
	 * file extension setting.
	 */
	public static boolean isHTMLFile(File file) {
		return UtilList.containsEquality(
				htmlParser.getExtensions(),
				UtilFile.getExtension(file)
		);
	}
	
	/**
	 * Returns whether the given file is an HTML file, using the HTML
	 * file extension setting stored in <tt>rootScope</tt>.
	 */
	public static boolean isHTMLFile(RootScope rootScope, File file) {
		return UtilList.containsEquality(
				rootScope.getHtmlExtensions(),
				UtilFile.getExtension(file)
		);
	}

	/**
	 * Sets which text file extensions should be used when selecting parsers for
	 * given files. The caller should call <tt>resetExtensions()</tt> at the end
	 * of the parse process.
	 */
	public static void setTextExtensions(String[] extensions) {
		textParser.setExtensions(extensions);
	}

	/**
	 * Sets which HTML file extensions should be used when determining whether a
	 * given file is an HTML file. The caller should call
	 * <tt>resetExtensions()</tt> at the end of the parse process.
	 */
	public static void setHTMLExtensions(String[] extensions) {
		htmlParser.setExtensions(extensions);
	}

	/**
	 * Resets the text and HTML file extensions to the defaults given by the
	 * corresponding settings in the preferences.
	 */
	public static void resetExtensions() {
		textParser.setExtensions(Pref.StrArray.TextExtensions.getValue());
		htmlParser.setExtensions(Pref.StrArray.HTMLExtensions.getValue());
	}

}
