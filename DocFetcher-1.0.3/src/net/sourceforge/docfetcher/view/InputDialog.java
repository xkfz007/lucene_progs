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

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.util.UtilGUI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author Tran Nam Quang
 */
public class InputDialog {
	
	private Shell shell;
	private String answer;
	private Text text;
	
	public InputDialog(Shell parent, String title, String msg, String defaultValue) {
		shell = new Shell(parent, Const.DIALOG_STYLE);
		shell.setText(title);
		
		Label label = new Label(shell, SWT.NONE);
		text = new Text(shell, SWT.BORDER | SWT.SINGLE);
		UtilGUI.selectAllOnFocus(text);
		Label separator = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		Button okBt = new Button(shell, SWT.PUSH);
		Button cancelBt = new Button(shell, SWT.PUSH);
		
		label.setText(msg);
		text.setText(defaultValue);
		text.selectAll();
		okBt.setText(Msg.ok.value());
		cancelBt.setText(Msg.cancel.value());
		
		FormLayout layout = new FormLayout();
		layout.marginWidth = layout.marginHeight = 5;
		shell.setLayout(layout);
		
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.top().left().right().applyTo(label);
		fdf.top(label).applyTo(text);
		fdf.reset().minWidth(Const.MIN_BT_WIDTH).bottom().right().applyTo(cancelBt);
		fdf.reset().minWidth(Const.MIN_BT_WIDTH).bottom().right(cancelBt).applyTo(okBt);
		fdf.reset().left().right().bottom(okBt).applyTo(separator);
		
		Point shellSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		shell.setSize(UtilGUI.minimum(shellSize, new Point(300, 150)));
		
		okBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				answer = text.getText();
				shell.close();
			}
		});
		
		cancelBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});
		
		text.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (UtilGUI.isCRKey(e)) {
					answer = text.getText();
					shell.close();
				}
			}
		});
	}

	/**
	 * If a filename has been set as the default value for this input dialog,
	 * calling this method will set the initial selection to the filename only,
	 * excluding the file extension.
	 */
	public void selectFilenameOnly() {
		String s = text.getText();
		int index = s.lastIndexOf('.');
		if (index == -1) return;
		text.setSelection(0, index);
	}
	
	/**
	 * Opens the dialog. Returns the input string or null if the dialog was
	 * canceled.
	 */
	public String open() {
		UtilGUI.centerShell(DocFetcher.getInstance().getShell(), shell);
		shell.open();
		while (! shell.isDisposed()) {
			if (! shell.getDisplay().readAndDispatch())
				shell.getDisplay().sleep();
		}
		return answer;
	}

}
