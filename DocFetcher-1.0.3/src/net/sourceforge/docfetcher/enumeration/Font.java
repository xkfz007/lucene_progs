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

package net.sourceforge.docfetcher.enumeration;

import net.sourceforge.docfetcher.Const;

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

/**
 * An enumeration of fonts used in the application.
 * 
 * @author Tran Nam Quang
 */
public enum Font {
	
	SYSTEM_BOLD,
	PREVIEW,
	PREVIEW_MONO,
	;
	
	private static boolean boldFontInitialized = false;
	private static FontRegistry fontRegistry = new FontRegistry();
	
	static {
		fontRegistry.put(PREVIEW.name(), new FontData[] {
				new FontData(
						Const.IS_WINDOWS ? Pref.Str.PreviewFontWin.getValue() : Pref.Str.PreviewFontLinux.getValue(),
						Pref.Int.PreviewFontHeight.getValue(),
						SWT.NORMAL
				)
		});
		fontRegistry.put(PREVIEW_MONO.name(), new FontData[] {
				new FontData(
						Const.IS_WINDOWS ? Pref.Str.PreviewFontMonoWin.getValue() : Pref.Str.PreviewFontMonoLinux.getValue(),
						Pref.Int.PreviewFontHeightMono.getValue(),
						SWT.NORMAL
				)
		});
	}
	
	/**
	 * Returns the <tt>Font</tt> object corresponding to this enumeration
	 * entity. It does not need to be disposed.
	 */
	public org.eclipse.swt.graphics.Font getFont() {
		if (this == SYSTEM_BOLD && ! boldFontInitialized) {
			/*
			 * We have to wait for the display to be created because we want to use
			 * a modified version of the SWT system font, which is only accessible
			 * after the creation of the display.
			 */
			FontData sysFD = Display.getDefault().getSystemFont().getFontData()[0];
			String sysFDName = sysFD.getName();
			int sysFDHeight = sysFD.getHeight();
			fontRegistry.put(SYSTEM_BOLD.name(), new FontData[] {new FontData(sysFDName, sysFDHeight, SWT.BOLD)});
			boldFontInitialized = true;
		}
		return fontRegistry.get(this.name());
	}

}
