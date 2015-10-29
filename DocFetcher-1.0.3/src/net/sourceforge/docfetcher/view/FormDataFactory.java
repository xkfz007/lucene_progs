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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Control;

/**
 * Helper class for creating SWT <code>FormData</code> objects. Works like the
 * JFace GridDataFactory.
 * 
 * @author Tran Nam Quang
 */
public class FormDataFactory {
	
	/**
	 * Singleton instance.
	 */
	private static FormDataFactory instance = new FormDataFactory();
	
	/**
	 * The FormData object associated with the FormDataFactory instance.
	 */
	private FormData fd = new FormData();
	
	/**
	 * Default value for the margin used by this class.
	 */
	public static final int DEFAULT_MARGIN = 5;
	
	/**
	 * The currently used margin.
	 */
	private int margin = DEFAULT_MARGIN;
	
	private int minWidth = 0;
	
	private int minHeight = 0;
	
	private FormDataFactory() {
		// Singleton
	}
	
	/**
	 * Returns the instance of this class with all fields reset.
	 */
	public static FormDataFactory getInstance() {
		return instance.reset();
	}
	
	/**
	 * Assigns the internal FormData object to this control and creates a new
	 * FormData object, with the same settings as the old one.
	 */
	public void applyTo(Control control) {
		Point defaultSize = null;
		if (minWidth > 0 || minHeight > 0)
			defaultSize = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		if (minWidth > 0)
			fd.width = Math.max(minWidth, defaultSize.x);
		if (minHeight > 0)
			fd.height = Math.max(minHeight, defaultSize.y);
		control.setLayoutData(fd);
		FormData oldFormData = fd;
		fd = new FormData();
		fd.width = oldFormData.width;
		fd.height= oldFormData.height;
		fd.top = oldFormData.top;
		fd.bottom = oldFormData.bottom;
		fd.left = oldFormData.left;
		fd.right = oldFormData.right;
	}
	
	/**
	 * Creates a FormData object for the current state of the factory.
	 */
	public FormData create() {
		FormData ret = new FormData();
		ret.width = fd.width;
		ret.height= fd.height;
		ret.top = fd.top;
		ret.bottom = fd.bottom;
		ret.left = fd.left;
		ret.right = fd.right;
		return ret;
	}
	
	/**
	 * Resets all <tt>FormData</tt> fields and uses the default margin value
	 * of 5.
	 */
	public FormDataFactory reset() {
		fd = new FormData();
		margin = DEFAULT_MARGIN;
		minWidth = 0;
		minHeight = 0;
		return this;
	}
	
	public FormDataFactory set(FormData fd) {
		this.fd = fd;
		margin = DEFAULT_MARGIN;
		minWidth = 0;
		minHeight = 0;
		return this;
	}
	
	public FormDataFactory setMargin(int margin) {
		this.margin = margin;
		return this;
	}
	
	public int getMargin() {
		return margin;
	}
	
	public FormDataFactory top(int numerator, int offset) {
		fd.top = new FormAttachment(numerator, offset);
		return this;
	}
	
	public FormDataFactory top() {
		fd.top = new FormAttachment(0, margin);
		return this;
	} 
	
	public FormDataFactory bottom(int numerator, int offset) {
		fd.bottom = new FormAttachment(numerator, offset);
		return this;
	}
	
	public FormDataFactory bottom() {
		fd.bottom = new FormAttachment(100, -margin);
		return this;
	}
	
	public FormDataFactory left(int numerator, int offset) {
		fd.left = new FormAttachment(numerator, offset);
		return this;
	}

	public FormDataFactory left() {
		fd.left = new FormAttachment(0, margin);
		return this;
	}
	
	public FormDataFactory right(int numerator, int offset) {
		fd.right = new FormAttachment(numerator, offset);
		return this;
	}

	public FormDataFactory right() {
		fd.right = new FormAttachment(100, -margin);
		return this;
	}

	public FormDataFactory top(Control control, int offset) {
		fd.top = new FormAttachment(control, offset);
		return this;
	}
	
	public FormDataFactory top(Control control) {
		fd.top = new FormAttachment(control, margin);
		return this;
	}
	
	public FormDataFactory bottom(Control control, int offset) {
		fd.bottom = new FormAttachment(control, offset);
		return this;
	}

	public FormDataFactory bottom(Control control) {
		fd.bottom = new FormAttachment(control, -margin);
		return this;
	}
	
	public FormDataFactory left(Control control, int offset) {
		fd.left = new FormAttachment(control, offset);
		return this;
	}

	public FormDataFactory left(Control control) {
		fd.left = new FormAttachment(control, margin);
		return this;
	}
	
	public FormDataFactory right(Control control, int offset) {
		fd.right = new FormAttachment(control, offset);
		return this;
	}

	public FormDataFactory right(Control control) {
		fd.right = new FormAttachment(control, -margin);
		return this;
	}
	
	public FormDataFactory width(int width) {
		fd.width = width;
		return this;
	}
	
	public FormDataFactory height(int height) {
		fd.height = height;
		return this;
	}
	
	public FormDataFactory minWidth(int width) {
		minWidth = width;
		return this;
	}
	
	public FormDataFactory minHeight(int height) {
		minHeight = height;
		return this;
	}

}
