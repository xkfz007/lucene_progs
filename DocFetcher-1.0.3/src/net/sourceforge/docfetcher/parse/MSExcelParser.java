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

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import net.sourceforge.docfetcher.enumeration.Msg;

import org.apache.poi.hssf.OldExcelFormatException;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/**
 * @author Tran Nam Quang
 */
public class MSExcelParser extends MSOfficeParser {

	private static final String[] extensions = new String[] {"xls"}; //$NON-NLS-1$
	
	public String renderText(File file) throws ParseException {
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			ExcelExtractor extractor = null;
			try {
				POIFSFileSystem fs = new POIFSFileSystem(in);
				extractor = new ExcelExtractor(fs);
			}
			catch (OldExcelFormatException e) {
				/*
				 * POI doesn't support the old Excel 5.0/7.0 (BIFF5) format,
				 * only the BIFF8 format from Excel 97/2000/XP/2003. Thus, we
				 * fall back to another Excel library.
				 */
				in.close();
				return extractWithJexcelAPI(file);
			}
			catch (Exception e) {
				// This can happen if the file has the "xls" extension, but is not an Excel document
				throw new ParseException(file, Msg.file_corrupted.value());
			}
			finally {
				in.close();
			}
			extractor.setFormulasNotResults(true);
			return extractor.getText();
		}
		catch (FileNotFoundException e) {
			throw new ParseException(file, Msg.file_not_found.value());
		}
		catch (IOException e) {
			throw new ParseException(file, Msg.file_not_readable.value());
		}
	}
	
	private String extractWithJexcelAPI(File file) throws ParseException {
		StringBuilder sb = new StringBuilder();
		try {
			Workbook workbook = Workbook.getWorkbook(file);
			for (int sIndex = 0; sIndex < workbook.getNumberOfSheets(); sIndex++) {
				Sheet sheet = workbook.getSheet(sIndex);
				sb.append(sheet.getName()).append("\n\n"); //$NON-NLS-1$
				for (int i = 0; i < sheet.getRows(); i++) {
					Cell[] row = sheet.getRow(i);
					for (int j = 0; j < row.length; j++)
						sb.append(row[j].getContents()).append(" "); //$NON-NLS-1$
					sb.append("\n");
				}
				sb.append("\n\n\n"); //$NON-NLS-1$
			}
		}
		catch (Exception e) {
			throw new ParseException(file, Msg.file_not_readable.value());
		}
		return sb.toString();
	}

	public String[] getExtensions() {
		return extensions;
	}

	public String getFileType() {
		return Msg.filetype_xls.value();
	}

}
