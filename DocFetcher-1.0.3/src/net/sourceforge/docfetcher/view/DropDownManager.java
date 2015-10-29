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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;

/**
 * A helper class for creating and populating the drop-down menu of a ToolItem.
 * For each drop-down ToolItem, a separate instance of this class must be
 * constructed. The <tt>add</tt> method of this class can then be used to add
 * menu items to the drop-down menu.
 * 
 * TODO This class is currently not in use.
 * 
 * @author Tran Nam Quang
 */
public class DropDownManager {
	
	private ToolItem toolItem;
	private Menu menu;
	
	public DropDownManager(ToolItem toolItem) {
		this.toolItem = toolItem;
		menu = new Menu(toolItem.getParent().getShell());
		toolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ToolItem item = DropDownManager.this.toolItem;
				Rectangle bounds = item.getBounds();
				Point pt = item.getParent().toDisplay(bounds.x, bounds.y);
				menu.setLocation(pt.x, pt.y + bounds.height);
				menu.setVisible(true);
			}
		});
	}
	
	/**
	 * Adds a menu item to the drop-down menu of the ToolItem with which the
	 * receiver has been initialized. The arguments <tt>text</tt> and
	 * <tt>image</tt> set the icon and label of the menu item, but can both be
	 * null. Clicks on the menu item are handled by the given <tt>listener</tt>.
	 */
	public void add(String text, Image image, SelectionListener listener) {
		MenuItem menuItem = new MenuItem(menu, SWT.NONE);
		if (text != null) menuItem.setText(text);
		if (image != null) menuItem.setImage(image);
		menuItem.addSelectionListener(listener);
	}

}
