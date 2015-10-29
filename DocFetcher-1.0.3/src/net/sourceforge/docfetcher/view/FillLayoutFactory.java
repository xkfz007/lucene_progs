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

import org.eclipse.swt.layout.FillLayout;

/**
 * Helper class for creating SWT <code>FillLayout</code> objects.
 * 
 * @author Tran Nam Quang
 */
public class FillLayoutFactory {
	
	private static FillLayoutFactory instance = new FillLayoutFactory();
	
	private FillLayout layout = new FillLayout();
	
	private FillLayoutFactory() {
		// Singleton
	}
	
	public static FillLayoutFactory getInst() {
		return instance;
	}
	
	public FillLayoutFactory spacing(int spacing) {
		layout.spacing = spacing;
		return this;
	}
	
	public FillLayoutFactory type(int type) {
		layout.type = type;
		return this;
	}
	
	public FillLayoutFactory margin(int margin) {
		layout.marginWidth = layout.marginHeight = margin;
		return this;
	}
	
	public FillLayoutFactory reset() {
		layout = new FillLayout();
		return this;
	}

	public FillLayout create() {
		FillLayout fillLayout = layout;
		layout = new FillLayout();
		return fillLayout;
	}

}
