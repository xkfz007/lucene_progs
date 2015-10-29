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

package net.sourceforge.docfetcher.util;

/**
 * @author Tran Nam Quang
 */
public class Timer {
	
	private static long firstPrintTime = -1;
	private static long lastPrintTime = -1;
	
	private Timer() {}
	
	public static void reset() {
		firstPrintTime = -1;
		lastPrintTime = -1;
	}
	
	public static void print(String msg) {
		long now = System.currentTimeMillis();
		if (lastPrintTime == -1)
			System.out.println("ABS\tREL\t" + msg); //$NON-NLS-1$
		else {
			long diff0 = now - firstPrintTime;
			long diff1 = now - lastPrintTime;
			System.out.println(diff0 + "\t" + diff1 + "\t" + msg); //$NON-NLS-1$ //$NON-NLS-2$
		}
		lastPrintTime = now;
		if (firstPrintTime == -1)
			firstPrintTime = now;
	}

}
