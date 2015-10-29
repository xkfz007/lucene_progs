/*******************************************************************************
 * Copyright (c) 2009 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.parse;

import net.sourceforge.docfetcher.enumeration.Msg;

/**
 * @author Tran Nam Quang
 */
public class MSPowerPoint2007Parser extends MSOffice2007Parser {

	private static final String[] extensions = new String[] {"pptx", "pptm"}; //$NON-NLS-1$
	
	public String[] getExtensions() {
		return extensions;
	}

	public String getFileType() {
		return Msg.filetype_pptx.value();
	}

}
