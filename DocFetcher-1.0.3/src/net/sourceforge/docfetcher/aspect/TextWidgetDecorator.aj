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

package net.sourceforge.docfetcher.aspect;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Text;

import net.sourceforge.docfetcher.enumeration.Key;

/**
 * Adds keyboard shortcuts to text widgets.
 * 
 * @author Tran Nam Quang
 */
public privileged aspect TextWidgetDecorator {
	
	/**
	 * A KeyListener that provides the text widgets it is added to (either Text
	 * or StyledText) with a Select-All key.
	 */
	static KeyAdapter stdTextKeyProvider = new KeyAdapter() {
		public void keyReleased(KeyEvent e) {
			Key key = Key.getKey(e.stateMask, e.keyCode);
			if (key == Key.SelectAll) {
				if (e.widget instanceof Text)
					((Text) e.widget).selectAll();
				else if (e.widget instanceof StyledText)
					((StyledText) e.widget).selectAll();
			}
		}
	};
	
	after() returning(Text textWidget): call(Text+.new(..)) {
		textWidget.addKeyListener(stdTextKeyProvider);
	}
	
	after() returning(StyledText textWidget): call(StyledText+.new(..)) {
		textWidget.addKeyListener(stdTextKeyProvider);
	}

}
