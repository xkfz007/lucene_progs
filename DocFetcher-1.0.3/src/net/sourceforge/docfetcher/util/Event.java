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

package net.sourceforge.docfetcher.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Display;

/**
 * An event class that provides a much simpler alternative to the Observer
 * pattern: Instead of adding methods like addListener, removeListener, etc. to
 * each class one wants to listen to, this class can simply be added as a field.
 * <p>
 * Moreover, the entire event system can be put into a 'caching mode' using the
 * static {@link #hold()} and {@link #flush()} methods, which is useful for
 * avoiding mass notification when a lot of changes are made to an observed
 * object. In caching mode, the event system may discard duplicate events,
 * depending on the return value that is choosen for
 * {@link Listener#getEventDataPolicy()}.
 * <p>
 * The additional type parameter T of this class specifies the type of the event
 * data object that is transmitted on notifications. If none is needed, the
 * observed object may return itself.
 * 
 * @author Tran Nam Quang
 */
public class Event<T> {
	
	/**
	 * This constant indicates that when the event system leaves the caching
	 * mode, the listener should only be notified of the last cached event.
	 */
	public static final int SINGLE = 0;
	
	/**
	 * This constant indicates that when the event system leaves the caching
	 * mode, the listener should not receive multiple copies of the same cached
	 * event data object.
	 */
	public static final int UNIQUE = 1;
	
	/**
	 * This constant indicates that when the event system leaves the caching
	 * mode, the listener should receive every cached event data object, even if
	 * there are duplicates.
	 */
	public static final int DUPLICATE = 2;
	
	public static abstract class Listener<T> {
		private List<T> cachedEventData = new ArrayList<T> ();
		
		public abstract void update(T eventData);
		
		/**
		 * Subclassers may override this method to return one of the constants
		 * defined in {@link Event}, that is, {@link Event#SINGLE}, Event.UNIQUE or
		 * Event.DUPLICATE
		 */
		protected int getEventDataPolicy() {
			return SINGLE; // Default value, can be overridden
		}
		
		private void updateFromCache() {
			int nEventData = cachedEventData.size();
			switch (getEventDataPolicy()) {
			case SINGLE:
				if (nEventData > 0)
					update(cachedEventData.get(nEventData - 1)); // last entry
				break;
			case UNIQUE:
				List<T> uniqueData = new ArrayList<T> (nEventData);
				for (int i = 0; i < nEventData; i++) {
					if (i < nEventData - 1) {
						T eventData = cachedEventData.get(i);
						List<T> nextEntries = cachedEventData.subList(i + 1, nEventData);
						if (! nextEntries.contains(eventData))
							uniqueData.add(eventData);
					}
				}
				for (T eventData : uniqueData)
					update(eventData);
				break;
			case DUPLICATE:
				for (T eventData : cachedEventData)
					update(eventData);
				break;
			}
			cachedEventData.clear();
		}
	}
	
	private boolean enabled = true;
	private Set<Listener<T>> listeners = new LinkedHashSet<Listener<T>> ();
	
	public void add(Listener<T> listener) {
		listeners.add(listener);
	}
	
	public void addAll(Collection<Listener<T>> listeners) {
		this.listeners.addAll(listeners);
	}
	
	
	public void remove(Listener<T> listener) {
		listeners.remove(listener);
	}
	
	public void removeAllListeners() {
		listeners.clear();
	}
	
	public Set<Listener<T>> getListeners() {
		return listeners;
	}
	
	/**
	 * Sents an event to the registered listeners. The event will be cached if
	 * the event system currently operates in caching mode. This method should
	 * only be called by the observed object.
	 */
	public void fireUpdate(final T eventData) {
		if (! enabled) return;
		if (Display.getCurrent() != null || Display.getDefault() == null) {
			doFireUpdate(eventData);
		}
		else {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					doFireUpdate(eventData);
				}
			});
		}
	}
	
	private void doFireUpdate(T eventData) {
		if (hold == 0) {
			for (Listener<T> listener : new ArrayList<Listener<T>> (listeners))
				listener.update(eventData);
		}
		else {
			for (Listener<T> listener : new ArrayList<Listener<T>> (listeners)) {
				cachedListeners.add(listener);
				listener.cachedEventData.add(eventData);
			}
		}
	}
	
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	private static int hold = 0;
	private static Set<Listener<?>> cachedListeners = new LinkedHashSet<Listener<?>> ();
	
	/**
	 * Temporarily puts the entire event system into a 'caching mode', meaning
	 * that subsequent notification requests caused by changes on the observed
	 * objects will be delayed until <tt>flush</tt> is called. Each
	 * <tt>hold</tt> must be followed by a <tt>flush</tt> some time later.
	 * <p>
	 * In caching mode, the event system may discard duplicate events, depending
	 * on the return value of {@link Listener#getEventDataPolicy()}.
	 * <p>
	 * Calls to <tt>hold</tt> and <tt>flush</tt> can be nested, so you could,
	 * for example, call <tt>hold</tt> three times, and then <tt>flush</tt>
	 * three times.
	 */
	public static void hold() {
		hold++;
	}
	
	/**
	 * @see #hold()
	 */
	public static void flush() {
		hold = Math.max(0, hold - 1);
		if (hold > 0) return;
		if (Display.getCurrent() != null || Display.getDefault() == null) {
			for (Listener<?> listener : cachedListeners)
				listener.updateFromCache();
		}
		else {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					for (Listener<?> listener : cachedListeners)
						listener.updateFromCache();
				}
			});
		}
		cachedListeners.clear();
	}

}
