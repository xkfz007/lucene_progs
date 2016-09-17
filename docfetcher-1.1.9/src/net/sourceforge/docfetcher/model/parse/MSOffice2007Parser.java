/*******************************************************************************
 * Copyright (c) 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.model.parse;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.apache.poi.POITextExtractor;
import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.opc.PackageProperties;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import com.google.common.io.Closeables;

/**
 * @author Tran Nam Quang
 */
abstract class MSOffice2007Parser extends FileParser {
	
	public static final class MSWord2007Parser extends MSOffice2007Parser {
		public MSWord2007Parser() {
			super(Msg.filetype_docx.get(), "docx", "docm", "dotx");
		}
	}
	
	public static final class MSExcel2007Parser extends MSOffice2007Parser {
		public MSExcel2007Parser() {
			super(Msg.filetype_xlsx.get(), "xlsx", "xlsm", "xltx");
		}
	}
	
	public static final class MSPowerPoint2007Parser extends MSOffice2007Parser {
		public MSPowerPoint2007Parser() {
			super(Msg.filetype_pptx.get(), "pptx", "pptm", "ppsx");
		}
	}
	
	private final Collection<String> types = MediaType.Col.application("zip");
	
	private final String typeLabel;
	private final Collection<String> extensions;
	
	private MSOffice2007Parser(	@NotNull String typeLabel,
								@NotNull String... extensions) {
		this.typeLabel = typeLabel;
		this.extensions = Arrays.asList(extensions);
	}

	@Override
	protected ParseResult parse(File file, ParseContext context)
			throws ParseException {
		OPCPackage pkg = null;
		try {
			pkg = OPCPackage.open(file.getPath(), PackageAccess.READ);
			String contents = extractText(pkg);
			
			// Open properties
			PackageProperties props = pkg.getPackageProperties();
			
			// Get author(s)
			String author = null;
			String defaultAuthor = props.getCreatorProperty().getValue();
			String lastAuthor = props.getLastModifiedByProperty().getValue();
			if (defaultAuthor == null) {
				if (lastAuthor != null)
					author = lastAuthor;
			}
			else if (lastAuthor == null) {
				author = defaultAuthor;
			}
			else {
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
			
			return new ParseResult(contents)
				.setTitle(title)
				.addAuthor(author)
				.addMiscMetadata(description)
				.addMiscMetadata(keywords)
				.addMiscMetadata(subject);
		}
		catch (Exception e) {
			throw new ParseException(e);
		}
		finally {
			Closeables.closeQuietly(pkg);
		}
	}
	
	@Override
	protected final String renderText(File file, String filename)
			throws ParseException {
		OPCPackage pkg = null;
		try {
			pkg = OPCPackage.open(file.getPath(), PackageAccess.READ);
			return extractText(pkg);
		}
		catch (Exception e) {
			throw new ParseException(e);
		}
		finally {
			Closeables.closeQuietly(pkg);
		}
	}
	
	// Caller is responsible for closing the given package
	@NotNull
	private static String extractText(@NotNull OPCPackage pkg) throws Exception {
		POITextExtractor extractor = ExtractorFactory.createExtractor(pkg);
		if (extractor instanceof XSSFExcelExtractor) {
			boolean indexFormulas = ProgramConf.Bool.IndexExcelFormulas.get();
			((XSSFExcelExtractor) extractor).setFormulasNotResults(indexFormulas);
		}
		String text = extractor.getText();
		return text;
	}

	protected final Collection<String> getExtensions() {
		return extensions;
	}

	protected final Collection<String> getTypes() {
		return types;
	}

	public final String getTypeLabel() {
		return typeLabel;
	}

}
