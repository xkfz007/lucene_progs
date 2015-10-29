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

package net.sourceforge.docfetcher.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Delegate for handling serialization
 * 
 * @author Tran Nam Quang
 */
public class Serializer {

	/**
	 * Loads and returns the serialized instance of the given class if one was
	 * found in the directory <tt>path</tt>, otherwise returns null.
	 * 
	 * @throws IOException
	 *             if the load process failed
	 * @throws ClassNotFoundException
	 *             if the load process failed
	 */
	public static Object load(Class<?> clazz, File path) throws IOException, ClassNotFoundException {
		File file = new File(path, clazz.getSimpleName() + ".ser"); //$NON-NLS-1$
		if (! file.exists()) {
			file.createNewFile();
			return null;
		}
		ObjectInputStream stream = new ObjectInputStream(
				new BufferedInputStream(new FileInputStream(file)));
		Object obj = stream.readObject();
		stream.close();
		return obj;
	}
	
	/**
	 * Saves the given object to disk, in the <tt>path</tt> directory. The
	 * parameter <tt>path</tt> should not be null.
	 * 
	 * @throws IOException
	 *             if the write process failed.
	 */
	public static synchronized void save(Object obj, File path) throws IOException {
		File file = new File(path, obj.getClass().getSimpleName() + ".ser"); //$NON-NLS-1$
		if (! path.exists())
			path.mkdirs();
		file.createNewFile();
		ObjectOutputStream stream = new ObjectOutputStream(
				new BufferedOutputStream(new FileOutputStream(file)));
		stream.writeObject(obj);
		stream.close();
	}
	
}
