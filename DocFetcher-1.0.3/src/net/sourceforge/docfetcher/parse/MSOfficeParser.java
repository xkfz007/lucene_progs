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
import net.sourceforge.docfetcher.model.Document;

import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;

/**
 * @author Tran Nam Quang
 */
public abstract class MSOfficeParser extends Parser {
	
	public Document parse(File file) throws ParseException {
		StringBuffer contents = new StringBuffer(renderText(file));
		
		POIFSReader reader = new POIFSReader();
		MyReaderListener listener = new MyReaderListener();
		reader.registerListener(listener, "\005SummaryInformation"); //$NON-NLS-1$
		
		try {
			InputStream in = new FileInputStream(file);
			reader.read(in);
			in.close();
		}
		catch (FileNotFoundException e) {
			throw new ParseException(file, Msg.file_not_found.value());
		}
		catch (IOException e) {
			throw new ParseException(file, Msg.file_not_readable.value());
		}
		
		String[] metaData = new String[] {
				listener.author,
				listener.title,
				listener.subject,
				listener.keywords,
				listener.comments
		};
		for (String field : metaData)
			if (field != null)
				contents.append(" ").append(field); //$NON-NLS-1$
		
		return new Document(file, listener.title, contents).addAuthor(listener.author);
	}

}

class MyReaderListener implements POIFSReaderListener {
	
	public String author;
	public String title;
	public String subject;
	public String keywords;
	public String comments;
	
	public void processPOIFSReaderEvent(POIFSReaderEvent event) {
		try {
			SummaryInformation si = (SummaryInformation) PropertySetFactory.create(event.getStream());
			
			// Combine 'author' and 'last author' field if they're identical
			String author;
			String defaultAuthor = si.getAuthor();
			String lastAuthor = si.getLastAuthor();
			if (defaultAuthor.equals(lastAuthor))
				author = defaultAuthor;
			else
				author = defaultAuthor + ", " + lastAuthor; //$NON-NLS-1$
			
			this.author = author;
			title = si.getTitle();
			subject = si.getSubject();
			keywords = si.getKeywords();
			comments = si.getComments();
		} catch (Exception e) {
			// Ignore, we can live without meta data
		}
	}
	
}