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

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.enumeration.Icon;
import net.sourceforge.docfetcher.enumeration.Key;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.util.UtilGUI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * A separate shell where the user can enter a new key combination.
 * 
 * @author Tran Nam Quang
 */
public class HotkeyDialog {
	
	private Shell parentShell;
	private Shell shell;
	private Text hotkeyBox;
	private int[] hotkey = Pref.IntArray.HotKeyToFront.getValue();
	private boolean hotkeyEnabled = Pref.Bool.HotkeyEnabled.getValue();
	
	public HotkeyDialog(Shell parentShell, int[] initialHotkey) {
		this.parentShell = parentShell;
		shell = new Shell(parentShell, SWT.RESIZE | SWT.CLOSE | SWT.TITLE);
		shell.setLayout(new FormLayout());
		shell.setImage(Icon.LETTERS.getImage());
		shell.setText(Msg.keybox_title.value());
		
		Label keyLabel = new Label(shell, SWT.NONE);
		keyLabel.setText(Msg.keybox_msg.value());
		hotkeyBox = new Text(shell, SWT.BORDER | SWT.READ_ONLY);
		hotkeyBox.setText(Key.toString(initialHotkey));
		hotkeyBox.setFocus();
		
		Label vSpacer = new Label(shell, SWT.NONE);
		Label sep = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		Label hSpacer = new Label(shell, SWT.NONE);
		
		Button okBt = new Button(shell, SWT.PUSH);
		okBt.setText(Msg.ok.value());
		Button cancelBt = new Button(shell, SWT.PUSH);
		cancelBt.setText(Msg.cancel.value());
		Button restoreBt = new Button(shell, SWT.PUSH);
		restoreBt.setText(Msg.restore_defaults.value());
		
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.top(0, 10).left(0, 10).right(100, -10).applyTo(keyLabel);
		fdf.top(keyLabel, 10).left(0, 10).right(100, -10).applyTo(hotkeyBox);
		fdf.reset().minWidth(Const.MIN_BT_WIDTH).bottom().right().applyTo(cancelBt);
		fdf.right(cancelBt).applyTo(okBt);
		fdf.right(okBt).applyTo(restoreBt);
		fdf.minWidth(0).right(restoreBt).left().applyTo(hSpacer);
		fdf.reset().left().right().bottom(cancelBt).applyTo(sep);
		fdf.bottom(sep).top(hotkeyBox).applyTo(vSpacer);
		
		/*
		 * We don't want the current hotkey to interfere with the hotkey dialog.
		 */
		Pref.Bool.HotkeyEnabled.setValue(false);
		shell.addDisposeListener(new DisposeListener () {
			public void widgetDisposed(DisposeEvent e) {
				Pref.Bool.HotkeyEnabled.setValue(hotkeyEnabled);
			}
		});
		
		hotkeyBox.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				hotkey = HotkeyDialog.this.acceptSWTHotkey(e.stateMask, e.keyCode);
				if (hotkey != null)
					hotkeyBox.setText(Key.toString(hotkey));
			}
		});
		
		shell.addShellListener(new ShellAdapter() {
			public void shellClosed(ShellEvent e) {
				hotkey = Pref.IntArray.HotKeyToFront.getValue();
				shell.dispose();
				shell = null;
				hotkeyBox = null;
			}
		});
		
		restoreBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				hotkey = Pref.IntArray.HotKeyToFront.defaultValue;
				hotkeyBox.setText(Key.toString(hotkey));
			}
		});
		
		okBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				hotkeyBox.setText(Key.toString(hotkey));
				shell.dispose();
				shell = null;
				hotkeyBox = null;
			}
		});
		
		cancelBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				hotkey = Pref.IntArray.HotKeyToFront.getValue();
				hotkeyBox.setText(Key.toString(hotkey));
				shell.dispose();
				shell = null;
				hotkeyBox = null;
			}
		});
	}
	
	public void open() {
		shell.pack();
		UtilGUI.centerShell(parentShell, shell);
		shell.open();
	}
	
	public int[] getHotkey() {
		return hotkey;
	}
	
	public void setHotkey(int[] hotkey) {
		this.hotkey = hotkey;
	}

	public void addDisposeListener(DisposeListener listener) {
		shell.addDisposeListener(listener);
	}

	public void removeDisposeListener(DisposeListener listener) {
		shell.removeDisposeListener(listener);
	}
	
	/**
	 * Returns the hotkey if the given SWT state mask and keycode represent
	 * valid user input, otherwise returns null. Some user inputs are invalid
	 * because they wouldn't work in the way they're supposed to work.
	 */
	private int[] acceptSWTHotkey(int stateMask, int keyCode) {
		int[] hotkey = new int[] {stateMask, keyCode};
		
		// State mask must be SWT.NONE or a combination of SWT.CTRL, SWT.ALT and SWT.SHIFT
		stateMask &= ~SWT.CTRL;
		stateMask &= ~SWT.ALT;
		stateMask &= ~SWT.SHIFT;
		if (stateMask != SWT.NONE) return null;
		
		// Accept special keys
		switch (keyCode) {
		case SWT.F1:
		case SWT.F2:
		case SWT.F3:
		case SWT.F4:
		case SWT.F5:
		case SWT.F6:
		case SWT.F7:
		case SWT.F8:
		case SWT.F9:
		case SWT.F10:
		case SWT.F11:
		case SWT.F12:
		case SWT.PAUSE:
		case SWT.PRINT_SCREEN:
		case SWT.BS:
		case SWT.CR:
		case SWT.INSERT:
		case SWT.DEL:
		case SWT.HOME:
		case SWT.END:
		case SWT.PAGE_UP:
		case SWT.PAGE_DOWN:
		case SWT.ARROW_UP:
		case SWT.ARROW_DOWN:
		case SWT.ARROW_LEFT:
		case SWT.ARROW_RIGHT: return hotkey;
		default: break;
		}
		
		// Shift and Alt as keycode is not allowed
		if (keyCode == SWT.SHIFT) return null;
		if (keyCode == SWT.ALT) return null;
		
		// Accept letters and digits
		if (Character.isLetterOrDigit(keyCode)) {
			if (Character.isLetter(keyCode)) // return uppercase letters
				hotkey[1] = Character.toUpperCase(hotkey[1]);
			return hotkey;
		}
		
		return null; // user input rejected
	}

}
