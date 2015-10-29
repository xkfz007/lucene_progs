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

package net.sourceforge.docfetcher.dev;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.model.Document;
import net.sourceforge.docfetcher.parse.Parser;
import net.sourceforge.docfetcher.parse.ParseException;
import net.sourceforge.docfetcher.parse.ParserRegistry;
import net.sourceforge.docfetcher.util.UtilGUI;
import net.sourceforge.docfetcher.view.FormDataFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Provides for the parser testbox.
 * 
 * @author Tran Nam Quang
 */
public aspect ParserTestboxInjector {
	
	/*
	 * Widgets
	 */
	private Shell testBox;
	private Rectangle testBoxBounds;
	private String content;
	private Text pathField;
	private Button chooseBt;
	private StyledText contentBox;
	private Button originalBt;
	private Text infoField;

	/**
	 * Create testbox
	 */
	after(): execution(* DocFetcher+.createContents(Composite)) {
		Display.getDefault().addFilter(SWT.KeyDown, new Listener() {
			public void handleEvent(Event event) {
				if (event.keyCode != SWT.F11) return;
				event.type = SWT.None;
				if (testBox != null && ! testBox.isDisposed()) {
					testBoxBounds = testBox.getBounds();
					testBox.dispose();
					return;
				}
				
				// Create new testbox
				testBox = new Shell(Display.getDefault(), SWT.BORDER | SWT.CLOSE | SWT.MIN | SWT.MAX | SWT.RESIZE);
				if (testBoxBounds != null)
					testBox.setBounds(testBoxBounds);
				else {
					testBox.setSize(500, 500);
					UtilGUI.centerShell(null, testBox);
				}
				testBox.setText(Msg.parser_testbox.value());
				testBox.setLayout(new FormLayout());

				// Populate testbox
				chooseBt = new Button(testBox, SWT.PUSH);
				chooseBt.setText(Msg.choose_file.value());
				pathField = new Text(testBox, SWT.BORDER | SWT.SINGLE);
				pathField.setText(Msg.enter_path_msg.value());
				contentBox = new StyledText(testBox, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
				originalBt = new Button(testBox, SWT.CHECK);
				originalBt.setText(Msg.original_parser_output.value());
				infoField = new Text(testBox, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
				infoField.setText(Msg.parser_testbox_info.value());

				// Layout
				FormDataFactory fdf = FormDataFactory.getInstance();
				fdf.top().right().applyTo(chooseBt);
				fdf.right(chooseBt).left().bottom(contentBox).applyTo(pathField);
				fdf.reset().left().bottom().right(originalBt).top(100, -50).applyTo(infoField);
				fdf.reset().top(chooseBt).bottom(infoField).left().right().applyTo(contentBox);
				fdf.reset().top(contentBox).bottom().right().applyTo(originalBt);

				// Show file dialog when choose button is pressed and parse the selected file
				chooseBt.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						FileDialog dg = new FileDialog(testBox, SWT.OPEN | SWT.PRIMARY_MODAL);
						showResults(dg.open());
					}
				});

				// Switch between original and beautified parser output
				originalBt.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						if (originalBt.getSelection())
							contentBox.setText(content);
						else
							contentBox.setText(content.replaceAll("\\s+", " ").trim()); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});

				// Parse file denoted by the path entered in the path field when ENTER is pressed
				pathField.addKeyListener(new KeyAdapter() {
					public void keyReleased(KeyEvent e) {
						if (UtilGUI.isCRKey(e)) {
							String path = pathField.getText();
							File file = new File(path);
							if (file.exists())
								showResults(file.getAbsolutePath());
							else {
								try {
									file = new File(new URL(path).toURI());
									if (file.exists())
										showResults(file.getAbsolutePath());
								} catch (MalformedURLException e1) {
									contentBox.setText(Msg.parser_testbox_invalid_input.value());
								} catch (URISyntaxException e2) {
									contentBox.setText(Msg.parser_testbox_invalid_input.value());
								}
							}
						}
					}
				});

				testBox.open();
			}
		});
	}
	
	/**
	 * Tries to parse the file denoted by the given path and show the results in
	 * the testbox.
	 */
	private void showResults(String path) {
		new ResultsThread(path).start();
	}
	
	public class ResultsThread extends Thread {
		
		private String path;
		
		public ResultsThread(String path) {
			this.path = path;
		}
		
		public void run() {
			try {
				if (path == null) return;
				File file = new File(path);
				final Parser parser = ParserRegistry.getParser(file);
				
				// Abort if no parser is available
				if (parser == null) {
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							contentBox.setText(Msg.unknown_document_format.value());
						}
					});
					return;
				}
				
				// Display some text before parsing
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						pathField.setText(path);
						contentBox.setText(Msg.parsing.value());
					}
				});
				
				// Do the parsing
				final long t_start = System.currentTimeMillis();
				Document _doc = parser.parse(file);
				final Document doc = _doc;
				final long t_end = System.currentTimeMillis();
				
				// Display the results
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						if (doc == null) {
							contentBox.setText(Msg.parser_not_supported.format(parser.getClass().getSimpleName()));
							return;
						}
						content = doc.getContents();
						if (originalBt.getSelection())
							contentBox.setText(content);
						else
							contentBox.setText(content.replaceAll("\\s+", " ").trim()); //$NON-NLS-1$ //$NON-NLS-2$
						infoField.setText(
								Msg.parsed_by.format(
										parser.getClass().getSimpleName(),
										(t_end - t_start),
										doc.getTitle(),
										doc.getAuthor()
								)
						);
					}
				});
			} catch (final ParseException e) {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						contentBox.setText(Msg.parse_exception.format(e.getMessage()));
					}
				});
			} catch (Exception e) {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						contentBox.setText(Msg.parser_testbox_unknown_error.value());
					}
				});
			} catch (OutOfMemoryError e) {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						contentBox.setText(Msg.out_of_jvm_memory.value());
					}
				});
			}
		}
		
	}
	
}
