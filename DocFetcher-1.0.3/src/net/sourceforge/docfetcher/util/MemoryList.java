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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A special kind of list that shows the typical behavior of a search history.
 * The details are explained in the add method.
 * 
 * @see #add(E item)
 * @author Tran Nam Quang
 */
public class MemoryList <E> implements Collection<E> {
	
	private List<E> items = new ArrayList<E> ();
	private int maxItems;
	
	/**
	 * @param maxItems Capacity limit of the list.
	 * @see #add(E item)
	 */
	public MemoryList(int maxItems) {
		this.maxItems = Math.max(0, maxItems);
	}
	
	/**
	 * Special list behavior:
	 * <ul>
	 * <li>New items are placed at the beginning of the list.</li>
	 * <li>If one attempts to add an item that is already in the list, instead
	 * of duplication the existing entry is moved to the beginning of the list.</li>
	 * <li>The list has a capacity limit. When that limit is reached, adding a
	 * new item leads to removal of the oldest item, which is at the end of the
	 * list.</li>
	 * </ul>
	 */
	public boolean add(E item) {
		if (! items.isEmpty() && items.get(0).equals(item))
			return false;
		items.remove(item);
		items.add(0, item);
		if (items.size() > maxItems)
			items.remove(items.size() - 1);
		return true;
	}
	
	/**
	 * Returns the newest item in the list, which is the item at the beginning
	 * of the list, or <tt>defaultValue</tt> if the list is empty.
	 */
	public E getNewest(E defaultValue) {
		if (items.isEmpty()) return defaultValue;
		return items.get(0);
	}
	
	public boolean isEmpty() {
		return items.isEmpty();
	}
	
	public int size() {
		return items.size();
	}
	
	public <T> T[] toArray(T[] a) {
		return items.toArray(a);
	}

	public boolean addAll(Collection<? extends E> c) {
		Object[] oldItems = items.toArray();
		List<E> reversedList = new ArrayList<E>(c);
		Collections.reverse(reversedList);
		for (E item : reversedList)
			add(item);
		if (oldItems.length != items.size())
			return true;
		Object[] newItems = items.toArray();
		for (int i = 0; i < newItems.length; i++)
			if (! oldItems[i].equals(newItems[i]))
				return true;
		return false;
	}

	public void clear() {
		items.clear();
	}

	public boolean contains(Object o) {
		return items.contains(o);
	}

	public boolean containsAll(Collection<?> c) {
		return items.containsAll(c);
	}

	public Iterator<E> iterator() {
		return items.iterator();
	}

	public boolean remove(Object o) {
		return items.remove(o);
	}

	public boolean removeAll(Collection<?> c) {
		return items.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return items.retainAll(c);
	}

	public Object[] toArray() {
		return items.toArray();
	}

}
