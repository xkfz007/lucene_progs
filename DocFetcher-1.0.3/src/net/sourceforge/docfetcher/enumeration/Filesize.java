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

package net.sourceforge.docfetcher.enumeration;

/**
 * An enumeration of possible filesize units (byte, kilobyte, etc.) with the
 * capability of converting values from one unit to another.
 * 
 * @author Tran Nam Quang
 */
public enum Filesize {
	
	Byte (1),
	KB (1024),
	MB (1024 * 1024),
	GB (1024 * 1024 * 1024);
	
	private int multiplier;
	
	Filesize(int multiplier) {
		this.multiplier = multiplier;
	}
	
	/**
	 * Converts the given filesize in the given unit into a filesize in the unit
	 * of the receiver.
	 */
	public long convert(long size, Filesize unit) {
		if (unit.multiplier / multiplier == 0 && unit.multiplier != 0)
			return (size / multiplier) * unit.multiplier;
		return (unit.multiplier / multiplier) * size;
	}
	
	/**
	 * Returns all enumerated filesize units as strings.
	 */
	public static String[] valuesAsStrings() {
		Filesize[] values = values();
		String[] strings = new String[values.length];
		for (int i = 0; i < values.length; i++)
			strings[i] = values[i].toString();
		return strings;
	}
	
	/**
	 * Returns whether the given string matches the string representation of one
	 * of the allowed filesize units, i.e. byte, KB, MB or GB (ignoring case).
	 */
	public static boolean contains(String value) {
		Filesize[] values = values();
		for (int i = 0; i < values.length; i++) {
			if (value.equalsIgnoreCase(values[i].toString()))
				return true;
		}
		return false;
	}

}
