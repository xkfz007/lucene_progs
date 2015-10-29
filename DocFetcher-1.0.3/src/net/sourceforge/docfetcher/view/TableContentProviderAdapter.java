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

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * A default implementation of JFace's <code>IStructuredContentProvider</code>.
 * 
 * @author Tran Nam Quang
 */
public class TableContentProviderAdapter implements IStructuredContentProvider {

	public Object[] getElements(Object inputElement) {
		return null;
	}
	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
	
	public void dispose() {}
	
}
