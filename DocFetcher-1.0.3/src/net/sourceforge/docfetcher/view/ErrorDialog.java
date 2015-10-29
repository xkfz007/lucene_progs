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
import net.sourceforge.docfetcher.enumeration.Icon;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.util.UtilFile;
import net.sourceforge.docfetcher.util.UtilGUI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author Tran Nam Quang
 */
public class ErrorDialog {
	
	private Text text;

	public ErrorDialog(String shellTitle) {
		// Shell
		int shellStyle = SWT.SYSTEM_MODAL | SWT.DIALOG_TRIM | SWT.MIN | SWT.MAX | SWT.RESIZE;
		Shell shell = new Shell(Display.getDefault(), shellStyle);
		shell.setImage(Icon.WARNING_BIG.getImage());
		shell.setText(shellTitle != null ? shellTitle : Msg.system_error.value());
		shell.setSize(400, 400);
		UtilGUI.centerShell(null, shell);
		
		// Widgets
		Link label = new Link(shell, SWT.NONE);
		label.setText(Msg.report_bug.format(Const.ERROR_FILEPATH));
		label.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				UtilFile.launch(e.text);
			}
		});
		text = new Text(shell, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		text.setBackground(UtilGUI.getColor(SWT.COLOR_WHITE));
		text.setForeground(UtilGUI.getColor(SWT.COLOR_RED));
		text.setFocus();
		
		// Layout
		shell.setLayout(new FormLayout());
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.setMargin(10).left().right().top().applyTo(label);
		fdf.top(label).bottom().applyTo(text);
		
		shell.open();
	}
	
	public void append(String str) {
		text.append(str);
	}

}
