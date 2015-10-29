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
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.enumeration.Font;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.util.UtilFile;
import net.sourceforge.docfetcher.util.UtilGUI;
import net.sourceforge.docfetcher.util.UtilList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Tran Nam Quang
 */
public class FileExtensionChooser {
	
	private Shell shell;
	private Table table;
	private StackLayout layout;
	private Set<String> output = new TreeSet<String> ();
	private Set<String> exts = new TreeSet<String> ();
	private Set<String> extsFromDisk;
	private Composite comp;
	private Thread thread;
	private Button okBt;
	
	public FileExtensionChooser(Shell parent) {
		shell = new Shell(parent, Const.DIALOG_STYLE);
		
		Label label = new Label(shell, SWT.NONE);
		label.setText(Msg.select_exts.value());
		
		comp = new Composite(shell, SWT.NONE);
		comp.setLayout(layout = new StackLayout());
		table = new Table(comp, SWT.CHECK | SWT.HIDE_SELECTION | SWT.BORDER);
		Composite textContainer = new Composite(comp, SWT.BORDER);
		textContainer.setBackground(UtilGUI.getColor(SWT.COLOR_LIST_BACKGROUND)); // don't use WHITE, it won't work with dark themes
		textContainer.setLayout(FillLayoutFactory.getInst().margin(5).create());
		StyledText loadingMsg = new StyledText(textContainer, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
		loadingMsg.setBackground(UtilGUI.getColor(SWT.COLOR_LIST_BACKGROUND)); // don't use WHITE, it won't work with dark themes
		loadingMsg.setText(Msg.loading.value());
		loadingMsg.setFont(Font.PREVIEW.getFont());
		loadingMsg.getCaret().setVisible(false);
		layout.topControl = textContainer;
		
		okBt = new Button(shell, SWT.PUSH);
		okBt.setEnabled(false);
		Button cancelBt = new Button(shell, SWT.PUSH);
		okBt.setText(Msg.ok.value());
		cancelBt.setText(Msg.cancel.value());
		
		FormLayout layout = new FormLayout();
		layout.marginWidth = layout.marginHeight = 5;
		shell.setLayout(layout);
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.top().left().right().applyTo(label);
		fdf.reset().minWidth(Const.MIN_BT_WIDTH).bottom().right().applyTo(cancelBt);
		fdf.right(cancelBt).applyTo(okBt);
		fdf.reset().left().right().top(label).bottom(okBt).applyTo(comp);
		
		table.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TableItem item = (TableItem) e.item;
				String ext = item.getText();
				if (item.getChecked())
					exts.add(ext);
				else
					exts.remove(ext);
			}
		});
		
		okBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				output = exts;
				shell.close();
			}
		});
		
		cancelBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});
		
		shell.addShellListener(new ShellAdapter() {
			public void shellClosed(ShellEvent e) {
				thread.interrupt();
			}
		});
		
		// Create context menu
		MenuManager contextMenu = new MenuManager();
		table.setMenu(contextMenu.createContextMenu(table));
		contextMenu.add(new CheckAllAction(true));
		contextMenu.add(new CheckAllAction(false));
		contextMenu.add(new Separator());
		contextMenu.add(new CheckInvertAction());
		
		shell.setSize(shell.computeSize(SWT.DEFAULT, 400));
		UtilGUI.centerShell(parent, shell);
	}
	
	/**
	 * Open the dialog.
	 * 
	 * @param checkedExts
	 *            The extensions that should be checked on this dialog if
	 *            found.
	 * @param extsFromDisk
	 *            The extensions to display. If null, the extensions
	 *            will be loaded recursively from <tt>rootDir</tt>.
	 */
	public Set<String> open(final File rootDir, final String[] checkedExts, final Set<String> extsFromDisk) {
		shell.open();
		for (String ext : checkedExts) {
			exts.add(ext);
			output.add(ext);
		}
		thread = new Thread() {
			public void run() {
				if (extsFromDisk == null)
					// This line will eat lots of cpu time
					FileExtensionChooser.this.extsFromDisk = UtilFile.listExtensions(rootDir);
				else
					FileExtensionChooser.this.extsFromDisk = extsFromDisk;
				if (Thread.currentThread().isInterrupted()) return;
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						for (String ext : FileExtensionChooser.this.extsFromDisk) {
							TableItem item = new TableItem(table, SWT.NONE);
							item.setText(ext);
							if (UtilList.containsEquality(checkedExts, ext))
								item.setChecked(true);
						}
						layout.topControl = table;
						comp.layout();
						okBt.setEnabled(true);
					}
				});
			}
		};
		thread.start();
		
		while (! shell.isDisposed()) {
			if (! shell.getDisplay().readAndDispatch())
				shell.getDisplay().sleep();
		}
		return output;
	}
	
	/**
	 * Returns the cached file extensions or null if they haven't been loaded
	 * yet.
	 */
	public Set<String> getExtensionsFromDisk() {
		return extsFromDisk;
	}
	
	private class CheckAllAction extends Action {
		private boolean value;
		public CheckAllAction(boolean value) {
			this.value = value;
			setText(value ? Msg.check_all.value() : Msg.uncheck_all.value());
		}
		public void run() {
			for (TableItem item : table.getItems())
				item.setChecked(value);
		}
	}
	
	private class CheckInvertAction extends Action {
		public CheckInvertAction() {
			setText(Msg.invert_selection.value());
		}
		public void run() {
			for (TableItem item : table.getItems())
				item.setChecked(! item.getChecked());
		}
	}

}
