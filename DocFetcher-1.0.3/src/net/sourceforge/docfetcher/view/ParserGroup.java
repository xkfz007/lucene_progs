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

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.enumeration.Key;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.parse.Parser;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.UtilList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

/**
 * @author Tran Nam Quang
 */
public class ParserGroup extends GroupWrapper {

	private Parser[] parsers;
	private CheckboxTableViewer viewer;
	private MenuManager contextMenu;

	public ParserGroup(Composite parent) {
		super(parent);
		group.setText(Msg.filetype_group_label.value());
		group.setLayout(FillLayoutFactory.getInst().margin(Const.GROUP_MARGIN).create());
		Table table = new Table(group, SWT.CHECK | SWT.MULTI | SWT.BORDER);
		viewer = new CheckboxTableViewer(table);
		
		table.addKeyListener(new ParserGroupNavigator());
		
		// Remove selection when viewer loses focus
		viewer.getTable().addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				((Table) e.widget).deselectAll();
			}
		});

		viewer.setContentProvider(new TableContentProviderAdapter() {
			public Object[] getElements(Object inputElement) {
				return (Object[]) inputElement;
			}
		});

		viewer.setLabelProvider(new TableLabelProviderAdapter() {
			public String getColumnText(Object element, int columnIndex) {
				return ((Parser) element).getFileType();
			}
		});

		viewer.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				return ((Parser) e1).compareTo((Parser) e2) ;
			}
		});

		// Update parser check states on changes
		viewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				Parser parser = (Parser) event.getElement();
				boolean oldState = parser.isChecked();
				boolean newState = event.getChecked();
				if (oldState != newState) // Avoid MVC circularity
					parser.setChecked(newState);
			}
		});

		// Create context menu
		contextMenu = new MenuManager();
		viewer.getTable().setMenu(contextMenu.createContextMenu(viewer.getTable()));
		contextMenu.add(new CheckAllAction(true));
		contextMenu.add(new CheckAllAction(false));
		contextMenu.add(new Separator());
		contextMenu.add(new CheckInvertAction());
	}
	
	public boolean setFocus() {
		return viewer.getControl().setFocus();
	}

	public void setParsers(Parser[] parsers) {
		this.parsers = parsers;
		viewer.setInput(parsers);

		// Set the checked state of each viewer item
		List<Parser> checked = new ArrayList<Parser> (parsers.length);
		for (int i = 0; i < parsers.length; i++) {
			if (parsers[i].isChecked())
				checked.add(parsers[i]);
		}
		viewer.setCheckedElements(checked.toArray());
	}

	private class CheckAllAction extends Action {
		private boolean value;
		public CheckAllAction(boolean value) {
			this.value = value;
			setText(value ? Msg.check_all.value() : Msg.uncheck_all.value());
		}
		public void run() {
			viewer.setAllChecked(value);
			Event.hold();
			for (Parser parser : parsers)
				parser.setChecked(value);
			Event.flush();
		}
	}
	
	private class CheckInvertAction extends Action {
		public CheckInvertAction() {
			setText(Msg.invert_selection.value());
		}
		public void run() {
			Event.hold();
			Object[] checked = viewer.getCheckedElements();
			for (Parser parser : parsers) {
				boolean setChecked = ! UtilList.containsIdentity(checked, parser);
				viewer.setChecked(parser, setChecked);
				parser.setChecked(setChecked);
			}
			Event.flush();
		}
	}
	
	/**
	 * Navigation in the parser group
	 */
	private class ParserGroupNavigator extends KeyAdapter {
		public void keyReleased(KeyEvent e) {
			Key key = Key.getKey(e.stateMask, e.keyCode);
			if (key == null) return;
			Table table = (Table) e.widget;
			int selIndex = table.getSelectionIndex();
			if (selIndex == -1) selIndex++;
			
			switch (key) {
			case Up:
				table.setSelection(Math.max(0, selIndex - 1));
				break;
			case Down:
				table.setSelection(Math.min(table.getItemCount() - 1, selIndex + 1));
				break;
			}
		}
	}

}