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

package net.sourceforge.docfetcher.parse;

import net.sourceforge.docfetcher.enumeration.Msg;

/**
 * @author Tran Nam Quang
 */
public class OOoWriterParser extends OOoParser {
	
	private static String[] extensions = new String[] {"odt", "ott"}; //$NON-NLS-1$ //$NON-NLS-2$
	
	public String[] getExtensions() {
		return extensions;
	}

	public String getFileType() {
		return Msg.filetype_odt.value();
	}

}
