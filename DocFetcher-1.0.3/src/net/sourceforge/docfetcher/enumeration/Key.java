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

package net.sourceforge.docfetcher.enumeration;

import org.eclipse.swt.SWT;

/**
 * An enumeration of available keyboard shortcuts.
 * <p>
 * Note: This is not a complete list of all keys that may work somewhere. Listed
 * are only those which aren't wired into SWT and which might need to be changed
 * in the future.
 * 
 * @author Tran Nam Quang
 */
public enum Key {
	
	Help (SWT.NONE, SWT.F1),
	FocusSearchBox (SWT.ALT, 'f'),
	FocusSearchBox2 (SWT.CTRL, 'f'),
	FocusFilesizeGroup (SWT.ALT, 'm'),
	FocusParserGroup (SWT.ALT, 't'),
	FocusScopeGroup (SWT.ALT, 's'),
	FocusResults (SWT.ALT, 'r'),
	Update (SWT.NONE, SWT.F5),
	Rename (SWT.NONE, SWT.F2),
	Enter (SWT.NONE, SWT.CR),
	
	Up (SWT.ALT, 'i'),
	Down (SWT.ALT, 'k'),
	Left (SWT.ALT, 'j'),
	Right (SWT.ALT, 'l'),
	Arrow_Left (SWT.NONE, SWT.ARROW_LEFT),
	Arrow_Right (SWT.NONE, SWT.ARROW_RIGHT),
	ScrollUp (SWT.CTRL, 'i'),
	ScrollDown (SWT.CTRL, 'k'),
	ScrollLeft (SWT.CTRL, 'j'),
	ScrollRight (SWT.CTRL, 'l'),
	
	SelectAll (SWT.CTRL, 'a'),
	Copy (SWT.CTRL, 'c'),
	Paste (SWT.CTRL, 'v'),
	Insert (SWT.NONE, SWT.INSERT),
	Delete (SWT.NONE, SWT.DEL),
	ShiftInsert (SWT.SHIFT, SWT.INSERT),
	ShiftDelete (SWT.SHIFT, SWT.DEL),
	;
	
	public final int stateMask;
	public final int keyCode;
	
	Key (int stateMask, int keyCode) {
		this.stateMask = stateMask;
		this.keyCode = keyCode;
	}
	
	/**
	 * Returns one of the registered keys for the given SWT stateMask and SWT
	 * keyCode. Returns null if the stateMask and keyCode represent a key that
	 * is not registered in this enumeration.
	 */
	public static Key getKey(int stateMask, int keyCode) {
		for (Key key : Key.values()) {
			if (key.stateMask == stateMask && key.keyCode == keyCode)
				return key;
		}
		return null;
	}
	
	/**
	 * Returns the key combination represented by this object as a string, e.g.
	 * "Ctrl + H".
	 */
	public String toString() {
		return Key.toString(new int[] {stateMask, keyCode});
	}
	
	/**
	 * Returns a JFace accelerator.
	 */
	public int getAccelerator() {
		return stateMask | keyCode;
	}
	
	/**
	 * Returns a string representing the key combination, e.g. "CTRL + H".
	 */
	public static String toString(int[] hotkey) {
		int stateMask = hotkey[0];
		int keyCode = hotkey[1];
		boolean ctrl = (stateMask & ~SWT.CTRL) != stateMask;
		boolean shift = (stateMask & ~SWT.SHIFT) != stateMask;
		boolean alt = (stateMask & ~SWT.ALT) != stateMask;
		String key = ""; //$NON-NLS-1$
		switch (keyCode) {
		case SWT.F1: key = "F1"; break; //$NON-NLS-1$
		case SWT.F2: key = "F2"; break; //$NON-NLS-1$
		case SWT.F3: key = "F3"; break; //$NON-NLS-1$
		case SWT.F4: key = "F4"; break; //$NON-NLS-1$
		case SWT.F5: key = "F5"; break; //$NON-NLS-1$
		case SWT.F6: key = "F6"; break; //$NON-NLS-1$
		case SWT.F7: key = "F7"; break; //$NON-NLS-1$
		case SWT.F8: key = "F8"; break; //$NON-NLS-1$
		case SWT.F9: key = "F9"; break; //$NON-NLS-1$
		case SWT.F10: key = "F10"; break; //$NON-NLS-1$
		case SWT.F11: key = "F11"; break; //$NON-NLS-1$
		case SWT.F12: key = "F12"; break; //$NON-NLS-1$
		case SWT.PAUSE: key = "Pause"; break; //$NON-NLS-1$
		case SWT.PRINT_SCREEN: key = "Print Screen"; break; //$NON-NLS-1$
		case SWT.BS: key = "Backspace"; break; //$NON-NLS-1$
		case SWT.CR: key = "Enter"; break; //$NON-NLS-1$
		case SWT.INSERT: key = "Insert"; break; //$NON-NLS-1$
		case SWT.DEL: key = "Delete"; break; //$NON-NLS-1$
		case SWT.HOME: key = "Home"; break; //$NON-NLS-1$
		case SWT.END: key = "End"; break; //$NON-NLS-1$
		case SWT.PAGE_UP: key = "Page Up"; break; //$NON-NLS-1$
		case SWT.PAGE_DOWN: key = "Page Down"; break; //$NON-NLS-1$
		case SWT.ARROW_UP: key = "Arrow Up"; break; //$NON-NLS-1$
		case SWT.ARROW_DOWN: key = "Arrow Down"; break; //$NON-NLS-1$
		case SWT.ARROW_LEFT: key = "Arrow Left"; break; //$NON-NLS-1$
		case SWT.ARROW_RIGHT: key = "Arrow Right"; break; //$NON-NLS-1$
		default: {
			key = String.valueOf((char) keyCode).toUpperCase();
		}
		}
		if (alt) key = "Alt + " + key; //$NON-NLS-1$
		if (shift) key = "Shift + " + key; //$NON-NLS-1$
		if (ctrl) key = "Ctrl + " + key; //$NON-NLS-1$
		return key;
	}

}
