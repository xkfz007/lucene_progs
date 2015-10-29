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

package net.sourceforge.docfetcher.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.docfetcher.enumeration.Key;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;

/**
 * A convenience method for easily adding entries to the context menu of a
 * <tt>TreeViewer</tt>.
 * 
 * @author Tran Nam Quang
 */
public class ViewerMenuManager {
	
	public interface RootChecker {
		/**
		 * Returns whether the given object is a root element.
		 */
		public boolean isRoot(Object obj);
	}
	
	private MenuManager contextMenu;
	private RootChecker rootChecker;
	private List<Action> rootActions = new ArrayList<Action> ();
	private List<Action> nonEmptyActions = new ArrayList<Action> ();
	private List<Action> singleElementActions = new ArrayList<Action> ();
	private Map<Key, Action> keyActionMap = new HashMap<Key, Action> ();
	
	public ViewerMenuManager(Viewer viewer) {
		contextMenu = new MenuManager();
		viewer.getControl().setMenu(contextMenu.createContextMenu(viewer.getControl()));
		
		// Update enabled state of actions according to current selection
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				StructuredSelection selection = (StructuredSelection) event.getSelection();
				
				setEnabled(nonEmptyActions, ! selection.isEmpty());
				setEnabled(singleElementActions, selection.size() == 1);
				
				if (rootChecker != null) {
					List<Object> rootSelection = new ArrayList<Object> (selection.size());
					Iterator<?> it = selection.iterator();
					while (it.hasNext()) {
						Object item = it.next();
						if (rootChecker.isRoot(item)) // Only enable action for RootScopes (not for other Scopes)
							rootSelection.add(item);
					}
					setEnabled(rootActions, rootSelection.size() > 0);
				}
			}
			private void setEnabled(List<Action> actions, boolean enabled) {
				for (Action action : actions)
					action.setEnabled(enabled);
			}
		});
		
		// Activate context menu entries through keyboard shortcuts
		viewer.getControl().addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				Key key = Key.getKey(e.stateMask, e.keyCode);
				if (key == null) return;
				Action action = keyActionMap.get(key);
				if (action != null && action.isEnabled())
					action.run();
			}
		});
	}
	
	/**
	 * Sets the <tt>RootChecker</tt> to be used to determine whether an element
	 * in the <tt>TreeViewer</tt> is a root element. Can be null.
	 */
	public void setRootChecker(RootChecker rootChecker) {
		this.rootChecker = rootChecker;
	}
	
	/**
	 * Adds an <tt>Action</tt> to the context menu whose enabled state does not
	 * change with the selection on the <tt>TreeViewer</tt>.
	 * <p>
	 * The <tt>Key</tt> parameter specifies a key to be pressed to activate the
	 * given <tt>Action</tt> object. It may be null.
	 */
	public void addUnmanagedAction(Action action, Key key) {
		contextMenu.add(action);
		if (key != null)
			keyActionMap.put(key, action);
	}
	
	/**
	 * Adds an <tt>Action</tt> to the context menu that is enabled only if at
	 * least one root element of the tree viewer is selected.
	 * <p>
	 * The <tt>Key</tt> parameter specifies a key to be pressed to activate the
	 * given <tt>Action</tt> object. It may be null.
	 */
	public void addRootAction(Action action, Key key) {
		rootActions.add(action);
		contextMenu.add(action);
		if (key != null)
			keyActionMap.put(key, action);
	}
	
	/**
	 * Adds an <tt>Action</tt> to the context menu that is enabled only if the
	 * current selection on the <tt>TreeViewer</tt> is not empty.
	 * <p>
	 * The <tt>Key</tt> parameter specifies a key to be pressed to activate the
	 * given <tt>Action</tt> object. It may be null.
	 */
	public void addNonEmptyAction(Action action, Key key) {
		nonEmptyActions.add(action);
		contextMenu.add(action);
		if (key != null)
			keyActionMap.put(key, action);
	}
	
	/**
	 * Adds an <tt>Action</tt> to the context menu that is enabled only if
	 * exactly one element on the <tt>TreeViewer</tt> is selected.
	 * <p>
	 * The <tt>Key</tt> parameter specifies a key to be pressed to activate the
	 * given <tt>Action</tt> object. It may be null.
	 */
	public void addSingleElementAction(Action action, Key key) {
		singleElementActions.add(action);
		contextMenu.add(action);
		if (key != null)
			keyActionMap.put(key, action);
	}
	
	/**
	 * Adds a separator to the context menu.
	 */
	public void addSeparator() {
		contextMenu.add(new Separator());
	}
	
	/**
	 * Sets the enabled state of all <tt>Action</tt> objects whose enabled
	 * states change according to the current selection on the
	 * <tt>TreeViewer</tt>.
	 */
	public void setManagedActionsEnabled(boolean enabled) {
		for (Action action : rootActions)
			action.setEnabled(enabled);
		for (Action action : nonEmptyActions)
			action.setEnabled(enabled);
		for (Action action : singleElementActions)
			action.setEnabled(enabled);
	}

}
