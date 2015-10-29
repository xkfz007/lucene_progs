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

package net.sourceforge.docfetcher.view;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.enumeration.Font;
import net.sourceforge.docfetcher.enumeration.Icon;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.model.Document;
import net.sourceforge.docfetcher.model.RootScope;
import net.sourceforge.docfetcher.parse.HTMLParser;
import net.sourceforge.docfetcher.parse.ParseException;
import net.sourceforge.docfetcher.parse.Parser;
import net.sourceforge.docfetcher.parse.ParserRegistry;
import net.sourceforge.docfetcher.parse.TextParser;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.UtilFile;
import net.sourceforge.docfetcher.util.UtilGUI;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.NullFragmenter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.TokenGroup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * @author Tran Nam Quang
 */
public class PreviewPanel extends Composite {

	private Composite previewPanel;
	private StackLayout layout;
	private Control lastTopControl;
	private Composite textViewerContainer;
	private StyledText textViewer;
	private BrowserProvider browserProvider = new BrowserProvider();
	private ToolBar generalToolBar;
	private ToolBar textToolBar;
	private Composite browserToolBar;
	private Text occurrenceCounter;
	private ToolItem upBt;
	private ToolItem downBt;
	private Color highlightColor;
	
	/** Whether this preview panel is enabled or not. */
	private boolean isActive;
	
	/** The currently displayed file. */
	private File file;
	
	/** The parser that has been used to parse the currently displayed file. */
	private Parser parser;
	
	/** The query used to obtain the results. */
	private Query query;
	
	/**
	 * A list of pairs of start indices and lengths that indicate which
	 * characters to highlight in the text-only preview.
	 */
	private int[] ranges = new int[0];
	private Composite previewBar;

	public PreviewPanel(Composite parent) {
		super(parent, SWT.NONE);
		previewBar = UtilGUI.createCompositeWithBorder(this, true);
		occurrenceCounter = new Text(previewBar, SWT.BORDER | SWT.SINGLE | SWT.CENTER | SWT.READ_ONLY);
		occurrenceCounter.setBackground(UtilGUI.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		occurrenceCounter.setToolTipText(Msg.occurrence_count.value());
		occurrenceCounter.setVisible(false);
		textToolBar = new ToolBar(previewBar, SWT.FLAT);
		textToolBar.setVisible(false);
		upBt = new ToolItem(textToolBar, SWT.FLAT);
		upBt.setImage(Icon.ARROW_UP.getImage());
		upBt.setToolTipText(Msg.prev_occurrence.value());
		upBt.setEnabled(false);
		downBt = new ToolItem(textToolBar, SWT.FLAT);
		downBt.setImage(Icon.ARROW_DOWN.getImage());
		downBt.setToolTipText(Msg.next_occurrence.value());
		downBt.setEnabled(false);
		
		// Create browser toolbar
		browserToolBar = new Composite(previewBar, SWT.NONE);
		browserToolBar.setLayoutData(new FormData(5,5)); // Without this, the toolbar height would be too large
		
		// Create general toolbar
		generalToolBar = new ToolBar(previewBar, SWT.FLAT);
		new ToolItem(generalToolBar, SWT.SEPARATOR);
		final ToolItem htmlPreviewBt = new ToolItem(generalToolBar, SWT.FLAT | SWT.CHECK);
		htmlPreviewBt.setImage(Icon.BROWSER.getImage());
		htmlPreviewBt.setSelection(Pref.Bool.PreviewHTML.getValue());
		htmlPreviewBt.setToolTipText(Msg.use_embedded_html_viewer.value());

		// Toolbar layout
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.setMargin(0).top().bottom().right().applyTo(generalToolBar);
		
		// Change search term highlighting color when the corresponding preferences value has changed
		Pref.IntArray.HighlightColor.evtChanged.add(new Event.Listener<int[]> () {
			public void update(int[] eventData) {
				Color oldColor = highlightColor;
				highlightColor = null; // the following call creates a new color object if this is null
				setHighlighting(Pref.Bool.HighlightSearchTerms.getValue());
				if (oldColor != null) oldColor.dispose();
			}
		});
		
		// Toggle search term highlighting when the corresponding preferences value has changed
		Pref.Bool.HighlightSearchTerms.evtChanged.add(new Event.Listener<Boolean> () {
			public void update(Boolean eventData) {
				setHighlighting(eventData);
			}
		});
		
		// Dispose of search term highlighting color on shutdown (it is created lazily)
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent arg0) {
				if (highlightColor != null) highlightColor.dispose();
			}
		});
		
		upBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Point sel = textViewer.getSelection();
				int searchStart = Math.min(sel.x, sel.y);
				int tokenStart = -1;
				int tokenEnd = -1;
				int tokenIndex = 0;
				for (int i = 0; i < ranges.length - 1; i = i + 2) {
					if (ranges[i] + ranges[i + 1] <= searchStart) {
						tokenStart = ranges[i];
						tokenEnd = ranges[i] + ranges[i + 1];
						tokenIndex = (i / 2) + 1;
					}
					else
						break;
				}
				if (tokenStart == -1) return;
				textViewer.setSelection(tokenStart, tokenEnd);
				occurrenceCounter.setText(
						Integer.toString(tokenIndex) + "/" + //$NON-NLS-1$
						Integer.toString(ranges.length / 2)
				);
				scrollToMiddle((tokenStart + tokenEnd) / 2);
			}
		});
		
		downBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Point sel = textViewer.getSelection();
				int searchStart = Math.max(sel.x, sel.y);
				int tokenStart = -1;
				int tokenEnd = -1;
				int tokenIndex = 0;
				for (int i = 0; i < ranges.length - 1; i = i + 2) {
					if (ranges[i] >= searchStart) {
						tokenStart = ranges[i];
						tokenEnd = tokenStart + ranges[i + 1];
						tokenIndex = (i / 2) + 1;
						break;
					}
				}
				if (tokenStart == -1) return;
				textViewer.setSelection(tokenStart, tokenEnd);
				occurrenceCounter.setText(
						Integer.toString(tokenIndex) + "/" + //$NON-NLS-1$
						Integer.toString(ranges.length / 2)
				);
				scrollToMiddle((tokenStart + tokenEnd) / 2);
			}
		});

		htmlPreviewBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Pref.Bool.PreviewHTML.setValue(htmlPreviewBt.getSelection());
				if (parser instanceof HTMLParser) // Only refresh preview panel for HTML documents
					setFile(file, parser, query, true);
			}
		});
		
		// Create preview panel, set up overall layout
		previewPanel = new Composite(this, SWT.BORDER);
		setLayout(new FormLayout());
		fdf.reset().setMargin(0).top().left().right().applyTo(previewBar);
		fdf.top(previewBar).bottom().applyTo(previewPanel);
		
		previewPanel.setLayout(layout = new StackLayout());
		textViewerContainer = new Composite(previewPanel, SWT.NONE);
		textViewerContainer.setLayout(FillLayoutFactory.getInst().margin(10).create());
		textViewer = new StyledText(textViewerContainer, SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL);
		textViewerContainer.setBackground(textViewer.getBackground());
		layout.topControl = textViewerContainer;
		
		textViewer.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Point sel = textViewer.getSelection();
				if (sel.x == sel.y)
					occurrenceCounter.setText(Integer.toString(ranges.length / 2));
			}
		});
	}
	
	/**
	 * Vertically divides the text viewer into three segments of equal height
	 * and scrolls the given caret offset into view so that it is always
	 * displayed in the middle segment (either at the top or at bottom of it or
	 * somewhere in between).
	 */
	private void scrollToMiddle(int caretOffset) {
		try {
			int lineIndexNow = textViewer.getLineAtOffset(caretOffset);
			int lineIndexTop = textViewer.getTopIndex();
			int lineIndexBottom = textViewer.getLineIndex(textViewer.getClientArea().height);
			double dist = lineIndexBottom - lineIndexTop;
			int dist13 = (int) (dist / 3);
			int dist23 = (int) (2 * dist / 3);
			double lineIndexMiddleTop = lineIndexTop + dist / 3;
			double lineIndexMiddleBottom = lineIndexBottom - dist / 3;
			if (lineIndexNow < lineIndexMiddleTop)
				textViewer.setTopIndex(lineIndexNow - dist13);
			else if (lineIndexNow > lineIndexMiddleBottom)
				textViewer.setTopIndex(lineIndexNow - dist23);
		}
		catch (Exception e) {
			// textViewer.getLineAtOffset(..) can throw an IllegalArgumentException
			// See bug #2778204
		}
	}
	
	/**
	 * Enables or disables the preview panel.
	 */
	public void setActive(boolean active) {
		this.isActive = active;
		if (active)
			setFile(file, parser, query);
		else {
			// Set blank page
			showViewer(textViewerContainer);
			textViewer.setText(""); //$NON-NLS-1$
			ranges = new int[0];
			BrowserPanel browser = browserProvider.getBrowser(previewPanel, browserToolBar, ParserRegistry.getHTMLParser());
			if (browser != null) // Is null on KDE desktops
				browser.setText("<html><head></head><body></body></html>"); //$NON-NLS-1$
		}
	}
	
	/**
	 * Sets the file to be displayed in the preview panel. The <tt>parser</tt>
	 * parameter is the parser instance with which the file was parsed. This
	 * method does nothing if the given file is null.
	 */
	public void setFile(File file, Parser parser, Query query) {
		setFile(file, parser, query, false);
	}

	/**
	 * Sets the file to be displayed, using <tt>parser</tt> to extract the
	 * text from the file on the disk. This method does nothing if the given
	 * file is null. The <tt>force</tt> parameter specifies whether the
	 * preview should be updated even if neither the file nor the search terms
	 * have changed in the meantime.
	 */
	private void setFile(final File file, final Parser parser, final Query query, boolean force) {
		File lastFile = this.file;
		Query lastQuery = this.query;
		this.file = file;
		this.parser = parser;
		this.query = query;
		
		// Check input
		if (file == null) return;
		if (parser == null) // Allowed to be null if file is null, too
			throw new IllegalArgumentException();
		if (! isActive) return;
		if (file.equals(lastFile) && ! force)
			if (query != null && query.equals(lastQuery))
				return;
		
		if (file.isDirectory())
			throw new IllegalStateException("File expected for preview, got directory instead."); //$NON-NLS-1$
		if (! file.exists()) {
			textViewer.setText(Msg.file_not_found.value());
			showViewer(textViewerContainer);
			return;
		}

		// Use the HTML browser
		if (file.getAbsolutePath().equals(Const.HELP_FILE) || Pref.Bool.PreviewHTML.getValue()) {
			final BrowserPanel browser = browserProvider.getBrowser(previewPanel, browserToolBar, parser);
			if (browser != null) {
				browser.addProgressListener(new ProgressAdapter() {
					public void completed(ProgressEvent event) {
						showViewer(browser);
						upBt.setEnabled(false);
						downBt.setEnabled(false);
						occurrenceCounter.setText("0"); //$NON-NLS-1$
					}
				});
				browser.setFile(file);
				return;
			}
			// Browser creation failed, go on to next code block
		}

		// Use text renderers
		showViewer(textViewerContainer);
		
		// Use monospace font for text files
		if (parser instanceof TextParser) {
			org.eclipse.swt.graphics.Font monoFont = Font.PREVIEW_MONO.getFont();
			if (! textViewer.getFont().equals(monoFont))
				textViewer.setFont(monoFont);
		}
		else {
			org.eclipse.swt.graphics.Font previewFont = Font.PREVIEW.getFont();
			if (! textViewer.getFont().equals(previewFont))
				textViewer.setFont(previewFont);
		}
		
		textViewer.setText(Msg.loading.value()); // display loading message
		
		new Thread() { // run in a thread because parsing the file takes some time
			public void run() {
				// Extract the raw text from the file
				String text;
				boolean fileParsed = true;
				try {
					text = parser.renderText(file);
				}
				catch (ParseException e) {
					text = Msg.cant_read_file.format(e.getMessage());
					fileParsed = false;
				}
				catch (OutOfMemoryError e) {
					/*
					 * We can get here if the user sets a high java heap space
					 * value during indexing and then sets a lower value for
					 * search only usage.
					 */
					text = Msg.out_of_jvm_memory.value();
					fileParsed = false;
				}
				
				if (PreviewPanel.this.file != file)
					return; // Another preview request had been started while we were parsing
				
				/*
				 * Create the message that will be displayed if the character limit
				 * is reached. It is appended to the file contents later; if it
				 * was appended here, some words in it might get highlighted.
				 */
				int maxLength = Pref.Int.PreviewLimit.getValue();
				final String msg = "...\n\n\n[" //$NON-NLS-1$
					+ Msg.preview_limit_hint.format(new Object[] {
							maxLength,
							Pref.Int.PreviewLimit.name(),
							Const.USER_PROPERTIES_FILENAME
					}) + "]"; //$NON-NLS-1$
				final boolean exceeded = text.length() > maxLength;
				if (text.length() > maxLength)
					text = text.substring(0, maxLength - msg.length());
				final String fText = text;
				
				/*
				 * Create StyleRange ranges (i.e. start-end integer pairs) for
				 * search term highlighting. Only tokenize preview text if we're
				 * not displaying any info messages and if there are tokens to
				 * highlight.
				 */
				ranges = new int[0];
				if (fileParsed && query != null) {
					final List<Integer> rangesList = new ArrayList<Integer> ();
					Analyzer analyzer = RootScope.analyzer;
					
					/*
					 * A formatter is supposed to return formatted text, but
					 * since we're only interested in the start and end offsets
					 * of the search terms, we return null and store the offsets
					 * in a list.
					 */
					Formatter nullFormatter = new Formatter() {
						public String highlightTerm(String originalText, TokenGroup tokenGroup) {
							for (int i = 0; i < tokenGroup.getNumTokens(); i++) {
								Token token = tokenGroup.getToken(i);
								if (tokenGroup.getScore(i) == 0)
									continue;
								int start = token.startOffset();
								int end = token.endOffset();
								rangesList.add(start);
								rangesList.add(end - start);
							}
							return null;
						}
					};
					
					Highlighter highlighter = new Highlighter(nullFormatter, new QueryScorer(query, Document.contents));
					highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
					highlighter.setTextFragmenter(new NullFragmenter());
					try {
						/*
						 * This has a return value, but we ignore it since we
						 * only want the offsets.
						 */
						highlighter.getBestFragment(analyzer, Document.contents, fText);
					} catch (Exception e) {
						// We can do without the search term highlighting
					}
					
					// List to array (will be used by the method 'setHighlighting(..)')
					ranges = new int[rangesList.size()];
					for (int i = 0; i < ranges.length; i++)
						ranges[i] = rangesList.get(i);
				}
				
				// Parsing and tokenizing done; display the results
				final boolean fFileParsed = fileParsed;
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						// Enable or disable up and down buttons
						upBt.setEnabled(ranges.length != 0);
						downBt.setEnabled(ranges.length != 0);
						
						textViewer.setText(fText);
						setHighlighting(fFileParsed && Pref.Bool.HighlightSearchTerms.getValue());
						occurrenceCounter.setText(Integer.toString(ranges.length / 2));
						if (exceeded)
							textViewer.append(msg); // character limit exceeded, append hint
					}
				});
			}
		}.start();
	}
	
	/**
	 * Update the search term highlighting based on the current highlighting
	 * preferences.
	 */
	private void setHighlighting(boolean highlight) {
		if (ranges.length != 0) {
			Color col = null;
			if (highlight) {
				if (highlightColor == null) { // lazy instantiation
					int[] rgb = Pref.IntArray.HighlightColor.getValue();
					highlightColor = new Color(getDisplay(), rgb[0], rgb[1], rgb[2]);
				}
				col = highlightColor;
			}
			else {
				col = textViewer.getBackground(); // no highlighting
			}
			StyleRange style = new StyleRange(0, 0, null, col);
			StyleRange[] styles = new StyleRange[ranges.length / 2];
			for (int i = 0; i < styles.length; i++)
				styles[i] = style;
			textViewer.setStyleRanges(ranges, styles);
		}
		else {
			textViewer.setStyleRange(null);
		}
	}

	/**
	 * Bring the given viewer (either HTML viewer or text viewer) to the front.
	 */
	private void showViewer(Composite viewer) {
		layout.topControl = viewer;
		if (lastTopControl != layout.topControl) {
			FormDataFactory fdf = FormDataFactory.getInstance();
			fdf.setMargin(0).top().bottom();
			if (viewer == textViewerContainer) {
				browserToolBar.setVisible(false);
				browserToolBar.setLayoutData(new FormData(5, 5));
				textToolBar.setVisible(true);
				fdf.right(generalToolBar).applyTo(textToolBar);
				occurrenceCounter.setVisible(true);
				fdf.right(textToolBar).minWidth(50).applyTo(occurrenceCounter);
			}
			else {
				browserToolBar.setVisible(true);
				fdf.height(5).top().bottom().left().right(generalToolBar).applyTo(browserToolBar);
				textToolBar.setVisible(false);
				textToolBar.setLayoutData(null);
				occurrenceCounter.setVisible(false);
				occurrenceCounter.setLayoutData(null);
			}
			previewBar.layout();
			previewPanel.layout();
			lastTopControl = viewer;
		}
	}

	/**
	 * If the internal HTML viewer is available, this method displays the help
	 * page in it and returns true. If not, it opens the help page in the
	 * external HTML browser and returns false.
	 */
	public boolean showHelpPage() {
		parser = ParserRegistry.getHTMLParser();
		final BrowserPanel browser = browserProvider.getBrowser(previewPanel, browserToolBar, parser);
		if (browser == null) {
			UtilFile.launch(Const.HELP_FILE);
			return false;
		}
		file = new File(Const.HELP_FILE);
		browser.addProgressListener(new ProgressAdapter() {
			public void completed(ProgressEvent event) {
				showViewer(browser);
				upBt.setEnabled(false);
				downBt.setEnabled(false);
				occurrenceCounter.setText("0"); //$NON-NLS-1$
			}
		});
		query = null;
		browser.setFile(file);
		return true;
	}

}

class BrowserProvider {

	private static boolean isBrowserAvailable = true;
	private BrowserPanel browser;

	/**
	 * Returns an SWT browser for the given parent <tt>Composite</tt> and the
	 * given HTML file only if several conditions are met:
	 * <ul>
	 * <li>An embedded browser must be available on the platform</li>
	 * <li>The preferences must allow HTML preview</li>
	 * <li>The given parser (with which the file was parsed) must be the
	 * <tt>HTMLParser</tt>.</li>
	 * </ul>
	 * Returns null otherwise.
	 */ 
	public BrowserPanel getBrowser(Composite parent, Composite toolBar, Parser parser) {
		if (! isBrowserAvailable)
			return null;
		if (! (parser instanceof HTMLParser))
			return null;
		try {
			if (browser == null)
				browser = new BrowserPanel(parent, toolBar);
			return browser;
		}
		catch (SWTError e) {
			isBrowserAvailable = false;
			if (browser != null) {
				browser.dispose();
				browser = null;
			}
			return null;
		}
	}

}
