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

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.enumeration.Key;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.UtilFile;
import net.sourceforge.docfetcher.util.UtilGUI;
import net.sourceforge.docfetcher.util.UtilList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 * @author Tran Nam Quang
 */
public class ProgressPanel extends Composite {
	
	SashForm sash;
	private Table errorTable;
	private TableColumn errorTypeCol;
	private TableColumn pathCol;
	private Text progressBox;
	private Menu menu;
	private boolean isMenuEnabled = false;
	
	public ProgressPanel(Composite parent) {
		super(parent, SWT.NONE);
		setLayout(new FillLayout());
		sash = new SashForm(this, SWT.VERTICAL | SWT.SMOOTH);
		
		// Create group widgets
		Group topGroup = new Group(sash, SWT.SHADOW_OUT);
		Group bottomGroup = new Group(sash, SWT.SHADOW_OUT);
		sash.setWeights(Pref.IntArray.SashProgressPanelWeights.getValue());
		topGroup.setText(Msg.progress.value());
		topGroup.setLayout(FillLayoutFactory.getInst().margin(1).create());
		bottomGroup.setText(Msg.errors.value());
		bottomGroup.setLayout(FillLayoutFactory.getInst().margin(1).create());
		
		// Save sash weights on change
		ControlAdapter sashWeightSaver = new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Pref.IntArray.SashProgressPanelWeights.setValue(sash.getWeights());
			}
		};
		topGroup.addControlListener(sashWeightSaver);
		bottomGroup.addControlListener(sashWeightSaver);
		
		// Create progress textbox
		progressBox = new Text(topGroup, SWT.READ_ONLY | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		progressBox.setBackground(UtilGUI.getColor(SWT.COLOR_LIST_BACKGROUND)); // don't use WHITE, it won't work with dark themes
		
		// Create error table
		errorTable = new Table(bottomGroup, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		errorTable.setLinesVisible(true);
		errorTable.setHeaderVisible(true);
		errorTable.setMenu(menu = new Menu(getShell(), SWT.POP_UP));
		
		// Create error table columns
		errorTypeCol = new TableColumn(errorTable, SWT.NONE);
		pathCol = new TableColumn(errorTable, SWT.NONE);
		errorTypeCol.setWidth(Pref.Int.ErrorTypeColWidth.getValue());
		errorTypeCol.setText(Msg.error_type.value());
		pathCol.setWidth(Pref.Int.ErrorPathColWidth.getValue());
		pathCol.setText(Msg.property_path.value());
		
		// Open file associated with error item on doubleclick
		errorTable.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				TableItem item = errorTable.getItem(new Point(e.x, e.y));
				UtilFile.launch(item.getText(1));
			}
		});
		
		// Some keyboard shortcuts
		errorTable.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				Key key = Key.getKey(e.stateMask, e.keyCode);
				if (key == null) return;
				switch (key) {
				case SelectAll: errorTable.selectAll(); break;
				case Copy: copyErrorSelectionToClipboard(); break;
				}
			}
		});
		
		// Save column widths on change
		errorTypeCol.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Pref.Int.ErrorTypeColWidth.setValue(errorTypeCol.getWidth());
			}
		});
		pathCol.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Pref.Int.ErrorPathColWidth.setValue(pathCol.getWidth());
			}
		});
		
		/*
		 * When sash weight of one indexing tab changes, synchronize sash
		 * weights of other indexing tabs with it.
		 */
		Pref.IntArray.SashProgressPanelWeights.evtChanged.add(new Event.Listener<int[]> () {
			public void update(int[] eventData) {
				for (IndexingTab tab : DocFetcher.getInstance().getIndexingDialog().getIndexingTabs()) {
					if (! tab.getJob().isReadyForIndexing()) continue;
					int[] w = tab.progressPanel.sash.getWeights();
					int[] pref_w = eventData;
					if (w[0] == pref_w[0] && w[1] == pref_w[1])
						continue;
					tab.progressPanel.sash.setWeights(pref_w);
				}
			}
		});
		
		/*
		 * Synchronize error type column width across indexing tabs.
		 */
		Pref.Int.ErrorTypeColWidth.evtChanged.add(new Event.Listener<Integer> () {
			public void update(Integer eventData) {
				for (IndexingTab tab : DocFetcher.getInstance().getIndexingDialog().getIndexingTabs()) {
					if (! tab.getJob().isReadyForIndexing()) continue;
					if (tab.progressPanel.errorTypeCol.getWidth() == eventData)
						continue;
					tab.progressPanel.errorTypeCol.setWidth(eventData);
				}
			}
		});
		
		/*
		 * Synchronize path column width across indexing tabs.
		 */
		Pref.Int.ErrorPathColWidth.evtChanged.add(new Event.Listener<Integer> () {
			public void update(Integer eventData) {
				for (IndexingTab tab : DocFetcher.getInstance().getIndexingDialog().getIndexingTabs()) {
					if (! tab.getJob().isReadyForIndexing()) continue;
					if (tab.progressPanel.pathCol.getWidth() == eventData)
						continue;
					tab.progressPanel.pathCol.setWidth(eventData);
				}
			}
		});
	}
	
	/**
	 * Copies the errors selected in the error panel to the clipboard.
	 */
	private void copyErrorSelectionToClipboard() {
		TableItem[] items = errorTable.getSelection();
		String[] filePaths = new String[items.length];
		for (int i = 0; i < items.length; i++)					
			filePaths[i] = items[i].getText(1);
		Transfer[] types = new Transfer[] {
				FileTransfer.getInstance(),
				TextTransfer.getInstance()
		};
		
		/*
		 * Bug #2904322: When this context menu action is activated, but no
		 * items in the table are selected, the arrays 'items' and 'filePaths'
		 * are empty, which leads to a crash.
		 */
		if (filePaths.length == 0) return;
		
		DocFetcher.getInstance().getClipboard().setContents(
				new Object[] {
						filePaths,
						UtilList.toString(Const.LS, filePaths)
				},
				types
		);
	}

	/**
	 * Appends the given message at the end of the feedback textbox. Calls to
	 * this method do not need to be encapsulated with
	 * <tt>Display.syncExec(Runnable)</tt> or similar constructs.
	 */
	public void appendInfo(final String msg) {
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					appendInfo(msg);
				}
			});
			return;
		}
		if (progressBox.isDisposed())
			return;
		if (progressBox.getCharCount() != 0)
			progressBox.append(Const.LS);
		int endPos = progressBox.getCharCount(); // this may return a different value than the previous call
		progressBox.append(msg);
		progressBox.setSelection(endPos); // Avoids horizontal scrolling when the message string is too long
		progressBox.setTopIndex(Integer.MAX_VALUE);
		
		// Limit number of lines in order to avoid OutOfMemoryExceptions
		if (progressBox.getLineCount() > Pref.Int.MaxLinesInProgressPanel.getValue()) {
			String text = progressBox.getText();
			int breakCount = 0;
			int i = 0;
			while (breakCount < 100) {
				i = text.indexOf("\n", i + 1); //$NON-NLS-1$
				breakCount++;
			}
			text = text.substring(i + 1);
			progressBox.setText("...\n"); //$NON-NLS-1$
			progressBox.append(text);
			progressBox.setSelection(text.lastIndexOf("\n") + 4); //$NON-NLS-1$
			progressBox.setTopIndex(Integer.MAX_VALUE);
		}
	}
	
	
	
	/**
	 * Adds the given error to the list of the displayed errors.
	 */
	public void addError(final String errorType, final String filePath) {
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					addError(errorType, filePath);
				}
			});
			return;
		}
		TableItem item = new TableItem(errorTable, SWT.NONE);
		item.setText(new String[] {errorType, filePath});
		if (! isMenuEnabled) {
			isMenuEnabled = true;
			for (MenuItem menuItem : errorTable.getMenu().getItems())
				menuItem.setEnabled(true);
		}
	}
	
	public MenuItem addErrorMenuItemOpen() {
		return addErrorMenuItem(Msg.open.value(), true, new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TableItem[] items = errorTable.getSelection();
				for (TableItem item : items)
					UtilFile.launch(item.getText(1));
			}
		});
	}
	
	public MenuItem addErrorMenuItemOpenParent() {
		return addErrorMenuItem(Msg.open_parent.value(), false, new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TableItem[] items = errorTable.getSelection();
				for (TableItem item : items) {
					File file = new File(item.getText(1));
					UtilFile.launch(UtilFile.getParent(file));
				}
			}
		});
	}
	
	public MenuItem addErrorMenuItemSeparator() {
		return new MenuItem(menu, SWT.SEPARATOR);
	}
	
	public MenuItem addErrorMenuItemCopy() {
		return addErrorMenuItem(Msg.copy.value(), false, new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				copyErrorSelectionToClipboard();
			}
		});
	}
	
	/**
	 * Adds a context menu item to the context menu of the error table.
	 */
	public MenuItem addErrorMenuItem(String text, boolean isDefault, SelectionListener listener) {
		MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText(text);
		item.setEnabled(false);
		item.addSelectionListener(listener);
		if (isDefault)
			menu.setDefaultItem(item);
		return item;
	}

}
