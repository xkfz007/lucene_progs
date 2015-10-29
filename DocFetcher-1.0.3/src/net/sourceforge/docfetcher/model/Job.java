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

package net.sourceforge.docfetcher.model;

import net.sourceforge.docfetcher.util.Event;

/**
 * An indexing task item used for queueing indexing tasks.
 * 
 * @author Tran Nam Quang
 */
public class Job {
	
	private RootScope scope;
	private boolean isReadyForIndexing;
	private boolean addToRegistry;
	private boolean doRebuild;
	public final Event<Job> evtReadyStateChanged = new Event<Job> ();
	
	/**
	 * @param scope
	 *            The <tt>RootScope</tt> the folder of which should be
	 *            indexed.
	 * @param addToRegistry
	 *            Whether the given <tt>RootScope</tt> should be added to the
	 *            scope registry after task completion.
	 * @param doRebuild
	 *            True if the index should be created anew. False for updates of
	 *            existing indexes.
	 */
	public Job(RootScope scope, boolean addToRegistry, boolean doRebuild) {
		this.scope = scope;
		this.addToRegistry = addToRegistry;
		this.doRebuild = doRebuild;
		this.isReadyForIndexing = ! doRebuild;
	}
	
	public boolean equals(Object obj) {
		if (obj == null || ! (obj instanceof Job)) return false;
		return ((Job) obj).scope.equals(scope);
	}
	
	/**
	 * @return The <tt>RootScope</tt> the folder of which should be indexed.
	 */
	public RootScope getScope() {
		return scope;
	}

	/**
	 * @return Whether the job is ready for processing.
	 */
	public boolean isReadyForIndexing() {
		return isReadyForIndexing;
	}

	/**
	 * Sets whether the job is ready for processing.
	 */
	public void setReadyForIndexing(boolean isReadyForIndexing) {
		this.isReadyForIndexing = isReadyForIndexing;
		evtReadyStateChanged.fireUpdate(this);
	}

	/**
	 * @return Whether the underlying <tt>RootScope</tt> should be added to
	 *         the scope registry after processing.
	 */
	public boolean isAddToRegistry() {
		return addToRegistry;
	}

	/**
	 * Sets whether the underlying <tt>RootScope</tt> should be added to the
	 * scope registry after processing.
	 */
	public void setAddToRegistry(boolean addToRegistry) {
		this.addToRegistry = addToRegistry;
	}

	/**
	 * @return Whether the index should be created anew. False for index
	 *         updates.
	 */
	public boolean isDoRebuild() {
		return doRebuild;
	}
	
}
