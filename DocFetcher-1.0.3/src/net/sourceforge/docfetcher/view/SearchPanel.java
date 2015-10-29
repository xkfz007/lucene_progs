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

import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.enumeration.Icon;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.MemoryList;
import net.sourceforge.docfetcher.util.UtilGUI;
import net.sourceforge.docfetcher.util.UtilList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * A panel consisting of the search bar and the result panel.
 * 
 * @author Tran Nam Quang
 */
public class SearchPanel extends Composite {
	
	public final Event<String> evtSearchRequest = new Event<String> ();
	public final Event<Widget> evtLeftBtClicked = new Event<Widget> ();
	public final Event<Widget> evtRightBtClicked = new Event<Widget> ();
	public final Event<Widget> evtHelpBtClicked = new Event<Widget> ();
	public final Event<Widget> evtPrefBtClicked = new Event<Widget> ();
	
	private Composite searchBar;
	private Combo searchBox;
	private ToolItem leftBt;
	private ToolItem rightBt;
	private ToolItem prefBt;
	private ToolItem toSystrayBt;
	private ResultPanel resultPanel;
	private ToolItem helpBt;
	
	private MemoryList<String> searchHistory;
	
	public SearchPanel(Composite parent) {
		super(parent, SWT.NONE);
		searchBar = UtilGUI.createCompositeWithBorder(this, true);
		searchBox = new Combo(searchBar, SWT.BORDER);
		searchBox.setVisibleItemCount(Pref.Int.SearchHistorySize.getValue());
		UtilGUI.selectAllOnFocus(searchBox);
		
		searchBox.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (UtilGUI.isCRKey(e))
					evtSearchRequest.fireUpdate(searchBox.getText());
			}
		});
		
		// Load search history
		searchHistory = new MemoryList<String> (Pref.Int.SearchHistorySize.getValue());
		String[] stringSearchHistory = Pref.StrArray.SearchHistory.getValue();
		searchHistory.addAll(UtilList.toList(stringSearchHistory));
		searchBox.setItems(stringSearchHistory);
		
		final Composite toolBarContainer = new Composite(searchBar, SWT.NONE);
		toolBarContainer.setLayout(new FormLayout());
		ToolBar toolBar = new ToolBar(toolBarContainer, SWT.FLAT);
		
		leftBt = new ToolItem(toolBar, SWT.FLAT);
		leftBt.setImage(Icon.ARROW_LEFT.getImage());
		leftBt.setToolTipText(Msg.prev_page.value());
		leftBt.setEnabled(false);
		leftBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				evtLeftBtClicked.fireUpdate(leftBt);
			}
		});
		
		rightBt = new ToolItem(toolBar, SWT.FLAT);
		rightBt.setImage(Icon.ARROW_RIGHT.getImage());
		rightBt.setToolTipText(Msg.next_page.value());
		rightBt.setEnabled(false);
		rightBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				evtRightBtClicked.fireUpdate(rightBt);
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		helpBt = new ToolItem(toolBar, SWT.FLAT);
		helpBt.setImage(Icon.HELP.getImage());
		helpBt.setToolTipText(Msg.open_manual.value());
		helpBt.setSelection(Pref.Bool.ShowWelcomePage.getValue());
		helpBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				evtHelpBtClicked.fireUpdate(helpBt);
			}
		});
		
		prefBt = new ToolItem(toolBar, SWT.FLAT);
		prefBt.setImage(Icon.PREFERENCES.getImage());
		prefBt.setToolTipText(Msg.preferences.value());
		prefBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				evtPrefBtClicked.fireUpdate(prefBt);
			}
		});
		
		toSystrayBt = new ToolItem(toolBar, SWT.FLAT);
		toSystrayBt.setImage(Icon.TO_SYSTRAY.getImage());
		toSystrayBt.setToolTipText(Msg.to_systray.value());
		toSystrayBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DocFetcher.getInstance().toSystemTray();
			}
		});
		
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.setMargin(0).top().bottom().right().applyTo(toolBar);
		fdf.reset().setMargin(0).top().bottom().right().applyTo(toolBarContainer);
		fdf.left().right(toolBarContainer).applyTo(searchBox);
		
		// Make the search box smaller when there's not enough space left
		searchBar.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				int spaceLeft = searchBar.getSize().x - toolBarContainer.getSize().x - searchBar.getBorderWidth() * 2;
				if (spaceLeft < Pref.Int.SearchBoxMaxWidth.getValue())
					FormDataFactory.getInstance().setMargin(0)
					.top().bottom()
					.left().right(toolBarContainer)
					.applyTo(searchBox);
			}
		});
		
		// Limit the width of the search box
		searchBox.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				int maxWidth = Pref.Int.SearchBoxMaxWidth.getValue();
				if (searchBox.getSize().x > maxWidth) {
					searchBox.setSize(maxWidth, SWT.DEFAULT); // necessary for avoiding a layout bug
					FormDataFactory.getInstance().setMargin(0)
					.top().bottom()
					.left().width(maxWidth)
					.applyTo(searchBox);
				}
			}
		});
		
		resultPanel = new ResultPanel(this);
		
		setLayout(new FormLayout());
		fdf.reset().setMargin(0).top().left().right().applyTo(searchBar);
		fdf.top(searchBar).bottom().applyTo(resultPanel);
		
		/*
		 * Without this call the position of the search box will be slightly
		 * off by a few pixels to the top.
		 */
		layout();
	}
	
	/**
	 * Returns an error message if the current text in the search box does not
	 * allow performing a search, otherwise returns null.
	 */
	public String checkSearchDisabled() {
		if (searchBox.getText().trim().equals("")) //$NON-NLS-1$
			return Msg.enter_nonempty_string.value();
		return null;
	}
	
	/**
	 * Adds the given term to the search history (i.e. the drop down list) of
	 * the search box.
	 */
	public void addToSearchHistory(String term) {
		searchHistory.add(term);
		String[] stringSearchHistory = searchHistory.toArray(new String[searchHistory.size()]);
		Pref.StrArray.SearchHistory.setValue(stringSearchHistory);
		searchBox.setItems(stringSearchHistory);
		searchBox.setText(term);
	}
	
	public ResultPanel getResultPanel() {
		return resultPanel;
	}
	
	public boolean setFocus() {
		return searchBox.setFocus();
	}
	
	public void setSearchBoxEnabled(boolean enabled) {
		searchBox.setEnabled(enabled);
	}
	
	public boolean isFocusControl() {
		return searchBox.isFocusControl();
	}
	
	public void setLeftBtEnabled(boolean enabled) {
		leftBt.setEnabled(enabled);
	}
	
	public void setRightBtEnabled(boolean enabled) {
		rightBt.setEnabled(enabled);
	}

}
