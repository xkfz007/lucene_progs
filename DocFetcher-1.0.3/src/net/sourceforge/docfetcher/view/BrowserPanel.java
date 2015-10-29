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

package net.sourceforge.docfetcher.view;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;

import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.enumeration.Icon;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.util.UtilFile;
import net.sourceforge.docfetcher.util.UtilGUI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * A wrapper for an <tt>org.eclipse.swt.browser.Browser</tt> that adds some
 * navigation buttons and an address bar.
 * 
 * @author Tran Nam Quang
 */
public class BrowserPanel extends Composite {
	
	private ToolBar navBar;
	private ToolItem backwBt;
	private ToolItem forwBt;
	private ToolItem stopBt;
	private ToolItem refreshBt;
	private ToolItem launchBt;
	private Text locationBar;
	private Browser browser;
	
	/**
	 * Warning: This constructor throws an SWTError if the browser could not be
	 * created. This can actually happen on some machines, so the caller should
	 * deal with that exception.
	 */
	public BrowserPanel(Composite parent, Composite controlPanel) {
		super(parent, SWT.NONE);
		
		// Try this first, so as to save resources if it fails
		try {
			browser = new Browser(this, SWT.MOZILLA); // Try mozilla browser first
		} catch (SWTError e) {
			try {
				browser = new Browser(this, SWT.NONE);
			} catch (SWTError e2) {
				dispose();
				throw e2;
			}
		}
		
		navBar = new ToolBar(controlPanel, SWT.FLAT);
		backwBt = new ToolItem(navBar, SWT.PUSH);
		forwBt = new ToolItem(navBar, SWT.PUSH);
		stopBt = new ToolItem(navBar, SWT.PUSH);
		refreshBt = new ToolItem(navBar, SWT.PUSH);
		launchBt = new ToolItem(navBar, SWT.PUSH);
		locationBar = new Text(controlPanel, SWT.SINGLE | SWT.BORDER);
		UtilGUI.selectAllOnFocus(locationBar);
		
		backwBt.setEnabled(false);
		forwBt.setEnabled(false);
		
		backwBt.setImage(Icon.ARROW_LEFT.getImage());
		forwBt.setImage(Icon.ARROW_RIGHT.getImage());
		stopBt.setImage(Icon.STOP.getImage());
		refreshBt.setImage(Icon.REFRESH.getImage());
		launchBt.setImage(Icon.PROGRAM.getImage());
		
		backwBt.setToolTipText(Msg.prev_page.value());
		forwBt.setToolTipText(Msg.next_page.value());
		stopBt.setToolTipText(Msg.browser_stop.value());
		refreshBt.setToolTipText(Msg.browser_refresh.value());
		launchBt.setToolTipText(Msg.browser_launch_external.value());
		
		setLayout(new FillLayout());
		controlPanel.setLayout(new FormLayout());
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.setMargin(0).top().bottom().left().applyTo(navBar);
		fdf.left(navBar).right().applyTo(locationBar);
		
		backwBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				browser.back();
			}
		});
		
		forwBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				browser.forward();
			}
		});
		
		stopBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				browser.stop();
			}
		});
		
		refreshBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				browser.refresh();
			}
		});
		
		launchBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String url = browser.getUrl();
				if (url.equals("")) return; //$NON-NLS-1$
				UtilFile.launch(url);
				if (Pref.Bool.HideOnOpen.getValue())
					DocFetcher.getInstance().toSystemTray();
			}
		});
		
		locationBar.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (UtilGUI.isCRKey(e))
					browser.setUrl(locationBar.getText());
			}
		});
		
		browser.addLocationListener(new LocationAdapter() {
			public void changing(LocationEvent event) {
				locationBar.setBackground(UtilGUI.getColor(SWT.COLOR_WIDGET_BACKGROUND));
			}
			public void changed(LocationEvent event) {
	            backwBt.setEnabled(browser.isBackEnabled());
	            forwBt.setEnabled(browser.isForwardEnabled());
	            String path = browser.getUrl();
	            if (path.startsWith("file:///")) { //$NON-NLS-1$
	            	try {
						path = new File(new URI(path)).getAbsolutePath();
					} catch (Exception e) {
						/*
						 * Ignoring URISyntaxException and
						 * IllegalArgumentException. The latter can happen if
						 * the URI contains a "fragment component", e.g.
						 * "myfile.htm#Section_1".
						 */
					}
	            }
	            locationBar.setText(path);
	            
	            /*
				 * The appropriate color is 'LIST_BACKGROUND', not 'WHITE',
				 * because the user might have chosen a dark theme.
				 */
	            locationBar.setBackground(UtilGUI.getColor(SWT.COLOR_LIST_BACKGROUND));
			}
		});
	}
	
	/**
	 * @see org.eclipse.swt.browser.Browser#setText(java.lang.String)
	 */
	public boolean setText(String html) {
		return browser.setText(html);
	}

	/**
	 * Sets the file that should be opened with this browser.
	 */
	public void setFile(File file) {
		String path = file.getAbsolutePath();
		try {
			String url = file.toURI().toURL().toString();
			browser.setUrl(url);
		} catch (MalformedURLException e) {
			browser.setUrl(path);
		}
		locationBar.setText(path);
	}
	
	/**
	 * @see org.eclipse.swt.browser.Browser#getUrl()
	 */
	public String getUrl() {
		return browser.getUrl();
	}

	/**
	 * @see org.eclipse.swt.browser.Browser#addProgressListener(ProgressListener)
	 */
	public void addProgressListener(ProgressListener listener) {
		browser.addProgressListener(listener);
	}

	/**
	 * @see org.eclipse.swt.browser.Browser#removeProgressListener(ProgressListener)
	 */
	public void removeProgressListener(ProgressListener listener) {
		browser.removeProgressListener(listener);
	}

}
