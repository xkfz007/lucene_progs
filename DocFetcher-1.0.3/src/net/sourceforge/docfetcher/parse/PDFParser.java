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
import java.io.IOException;
import java.io.StringWriter;

import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.model.Document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.encryption.StandardDecryptionMaterial;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 * @author Tran Nam Quang
 */
public class PDFParser extends Parser {
	
	private static String[] extensions = new String[] {"pdf"}; //$NON-NLS-1$
	
	public String renderText(File file) throws ParseException {
		PDDocument pdfDoc = null;
		try {
			pdfDoc = PDDocument.load(file);
			if (pdfDoc.isEncrypted()) {
				try {
					pdfDoc.openProtection(new StandardDecryptionMaterial(""));
				} catch (Exception e) {
					throw new ParseException(file, Msg.no_extraction_permission.value());
				}
			}
			PDFTextStripper stripper = new PDFTextStripper();
			StringWriter writer = new StringWriter();
			stripper.writeText(pdfDoc, writer);
			return writer.toString();
		}
		catch (IOException e) {
			throw new ParseException(file, Msg.file_not_readable.value());
		}
		finally {
			if (pdfDoc != null) {
				try {
					pdfDoc.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public Document parse(File file) throws ParseException {
		PDDocument pdfDoc = null;
		try {
			// Check if PDF file is encrypted
			pdfDoc = PDDocument.load(file);
			if (pdfDoc.isEncrypted()) {
				try {
					pdfDoc.openProtection(new StandardDecryptionMaterial(""));
				} catch (Exception e) {
					throw new ParseException(file, Msg.no_extraction_permission.value());
				}
			}

			// Get tags and contents
			PDFTextStripper stripper = new PDFTextStripper();
			StringWriter writer = new StringWriter();
			stripper.writeText(pdfDoc, writer);
			DocFetcher.getInstance().setExceptionHandlerEnabled(true);
			PDDocumentInformation pdInfo = pdfDoc.getDocumentInformation();
			String[] metaData = new String[] {
					pdInfo.getTitle(),
					pdInfo.getAuthor(),
					pdInfo.getSubject(),
					pdInfo.getKeywords(),
			};
			for (String field : metaData)
				if (field != null)
					writer.append(" ").append(field); //$NON-NLS-1$
			return new Document(file, metaData[0], writer.getBuffer()).addAuthor(metaData[1]);
		}
		catch (IOException e) {
			throw new ParseException(file, Msg.file_not_readable.value());
		}
		finally {
			if (pdfDoc != null) {
				try {
					pdfDoc.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public String getFileType() {
		return Msg.filetype_pdf.value();
	}
	
	public String[] getExtensions() {
		return extensions;
	}

}
