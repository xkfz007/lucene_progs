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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * A generic group composite.
 * 
 * @author Tran Nam Quang
 */
public class GroupWrapper extends Composite {
	// TODO Not sure if that tiny class here makes sense.
	
	protected Group group;
	
	public GroupWrapper(Composite parent) {
		super(parent, SWT.NONE);
		this.setLayout(new FillLayout());
		group = new Group(this, SWT.NONE);
	}

}