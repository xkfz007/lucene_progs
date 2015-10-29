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

import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.model.Document;

import org.apache.poi.POITextExtractor;
import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.opc.PackageProperties;

/**
 * @author Tran Nam Quang
 */
public abstract class MSOffice2007Parser extends Parser {
	
	public Document parse(File file) throws ParseException {
		try {
			// Extract contents
			POITextExtractor ef = ExtractorFactory.createExtractor(file);
			StringBuffer contents = new StringBuffer(ef.getText());
			
			// Open up properties
			OPCPackage pkg = OPCPackage.open(file.getAbsolutePath(), PackageAccess.READ);
			PackageProperties props = pkg.getPackageProperties();
			
			// Get author(s)
			String author = null;
			String defaultAuthor = props.getCreatorProperty().getValue();
			String lastAuthor = props.getLastModifiedByProperty().getValue();
			if (defaultAuthor == null) {
				if (lastAuthor != null)
					author = lastAuthor;
			} else if (lastAuthor == null) {
				author = defaultAuthor;
			} else {
				if (defaultAuthor.equals(lastAuthor))
					author = defaultAuthor;
				else
					author = defaultAuthor + ", " + lastAuthor; //$NON-NLS-1$
			}
			
			// Get other metadata
			String description = props.getDescriptionProperty().getValue();
			String keywords = props.getKeywordsProperty().getValue();
			String subject = props.getSubjectProperty().getValue();
			String title = props.getTitleProperty().getValue();
			
			// Append metadata to contents
			String[] metaData = new String[] {
					author, description, keywords, subject, title
			};
			for (String field : metaData)
				if (field != null)
					contents.append(" ").append(field); //$NON-NLS-1$
			return new Document(file, title, contents).addAuthor(author);
		} catch (Exception e) {
			throw new ParseException(file, Msg.file_not_readable.value());
		}
	}

	public String renderText(File file) throws ParseException {
		try {
			return ExtractorFactory.createExtractor(file).getText();
		} catch (Exception e) {
			throw new ParseException(file, Msg.file_not_readable.value());
		}
	}

}
