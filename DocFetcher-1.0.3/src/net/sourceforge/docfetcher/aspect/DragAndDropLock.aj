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

package net.sourceforge.docfetcher.aspect;

import net.sourceforge.docfetcher.view.ScopeGroup;

import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.Dialog;

/**
 * This aspect makes sure the user can't add folders to the search scope using
 * drag and drop when an SWT FileDialog or SWT DirectoryDialog is open. We have
 * to do it with an aspect since in pure Java there seems to be no way to find
 * out if somewhere on the GUI a FileDialog or DirectoryDialog is open, other
 * than the messy approach of manually keeping track of all Dialog+.open()
 * actions.
 * 
 * @author Tran Nam Quang
 */
public aspect DragAndDropLock {
	
	/** Number of open dialogs */
	private int nDialogsOpen = 0;
	
	before(): call(* Dialog+.open(..)) {
		nDialogsOpen++;
	}
	
	after(): call(* Dialog+.open(..)) {
		nDialogsOpen--;
	}
	
	/** Disallow drag and drop if at least one dialog is open */
	void around(): execution(* drop(DropTargetEvent)) && within(ScopeGroup) {
		if (nDialogsOpen == 0)
			proceed();
	}

}
