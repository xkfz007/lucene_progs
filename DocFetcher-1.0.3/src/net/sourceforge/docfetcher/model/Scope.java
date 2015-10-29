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

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.docfetcher.parse.ParserRegistry;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.UtilFile;

/**
 * An object representation of directories in the search scope.
 *
 * @author Tran Nam Quang
 */
public class Scope extends Indexable {
	
	static final long serialVersionUID = 1;
	
	/**
	 * Event: Check state of this object has changed.
	 */
	public static final Event<Scope> checkStateChanged = new Event<Scope> ();

	/**
	 * The set of files in the directory represented by this class. Only
	 * contains files for which a parser is available. Does not contain HTML
	 * files.
	 */
	protected Set<FileWrapper> subFiles = new HashSet<FileWrapper> ();

	/**
	 * The set of pairs of HTML files and their associated folders located in
	 * the directory represented by this class.
	 */
	protected Set<HTMLPair> subHTMLPairs = new HashSet<HTMLPair> ();

	/**
	 * The child Scopes under this scope. Does not include Scopes whose
	 * underlying folders are associated with HTML files.
	 */
	protected Set<Scope> subScopes = new HashSet<Scope> ();

	/**
	 * The check state of this Scope.
	 * <p>
	 * Note: This variable is named 'unchecked' instead of 'checked' because its
	 * value is not stored when this class is serialized (transient!), but we
	 * want its default value to be 'true' instead of 'false', so we make a
	 * logical flip here.
	 */
	private transient boolean unchecked = false;

	/**
	 * @param parent
	 *            The parent Scope
	 * @param file
	 *            The directory represented by this Scope
	 */
	protected Scope(Scope parent, File file) {
		super(parent, file);
		if (file.isFile())
			throw new IllegalStateException("The scope object must not be initialized with a file object"); //$NON-NLS-1$
	}

	/**
	 * Returns the top-level parent of the receiver.
	 */
	public RootScope getRootScope() {
		if (parent == null)
			return (RootScope) this;
		return parent.getRootScope();
	}

	/**
	 * Returns the object under this <tt>Scope</tt> that represents the given
	 * file (recursive). The argument must not be null and must not be a
	 * directory. Returns null if no representation object is found.
	 */
	public FileWrapper getFileWrapperDeep(File file) {
		if (UtilFile.getParentFile(file).equals(this.file)) {
			if (ParserRegistry.isHTMLFile(getRootScope(), file)) {
				for (HTMLPair candidate : subHTMLPairs)
					if (candidate.getFile().equals(file))
						return candidate;
			}
			else for (FileWrapper candidate : subFiles)
				if (candidate.getFile().equals(file))
					return candidate;
		}
		else for (Scope subScope : subScopes)
			if (subScope.contains(file))
				return subScope.getFileWrapperDeep(file);
		return null;
	}

	/**
	 * Returns the checked state of this Scope.
	 */
	public boolean isChecked() {
		return ! unchecked;
	}

	/**
	 * Sets the checked state of this Scope.
	 */
	public void setChecked(boolean checked) {
		this.unchecked = ! checked;
		checkStateChanged.fireUpdate(this);
	}

	/**
	 * Recursively checks or unchecks all Scopes under the receiver,
	 * including the latter.
	 */
	public void setCheckedDeep(boolean checked) {
		setChecked(checked);
		for (Scope child : getChildren())
			child.setCheckedDeep(checked);
	}

	/**
	 * Returns all scopes under this Scope object.
	 */
	public Scope[] getChildren() {
		return subScopes.toArray(new Scope[subScopes.size()]);
	}

	/**
	 * Returns true if the receiver contains the given scope. Example: If the
	 * receiver represents "C:\", then it contains "C:\Windows\System\". This
	 * method does not check if the directory represented by the given Scope
	 * actually exists.
	 */
	public boolean contains(Scope scope) {
		return UtilFile.contains(file.getAbsolutePath(), scope.file.getAbsolutePath());
	}

	/**
	 * Returns true if the receiver contains the given file. Example: If the
	 * receiver represents "C:\", then it contains "C:\Windows\System\foo.dll".
	 * This method does not check if the given file actually exists.
	 */
	public boolean contains(File file) {
		return UtilFile.contains(this.file.getAbsolutePath(), file.getAbsolutePath());
	}
	
	/**
	 * Returns a Scope object for the given directory if it is identical to the
	 * receiver or a child of the receiver (recursive). Otherwise returns null.
	 */
	public Scope getScopeDeep(File directory) {
		if (file.equals(directory))
			return this;
		for (Scope child : subScopes) {
			Scope candidate = child.getScopeDeep(directory);
			if (candidate != null) return candidate;
		}
		return null;
	}

	/**
	 * Returns whether the given non-HTML file has a representation directly
	 * under this <tt>Scope</tt> (non-recursive).
	 */
	public boolean isFileRegistered(File file) {
		for (FileWrapper wrapper : subFiles)
			if (wrapper.file.equals(file))
				return true;
		return false;
	}

	/**
	 * Returns whether the given HTML file has a representation directly under
	 * this <tt>Scope</tt> (non-recursive).
	 */
	public boolean isHTMLPairRegistered(HTMLPair candidate) {
		for (HTMLPair htmlPair : subHTMLPairs)
			if (htmlPair.equals(candidate))
				return true;
		return false;
	}
	
	/**
	 * Returns an HTMLPair object under this Scope for the given HTML directory,
	 * or null if none is found (recursive).
	 */
	public HTMLPair getHTMLPair(File directory) {
		if (directory == null) return null;
		for (HTMLPair htmlPair : subHTMLPairs)
			if (directory.equals(htmlPair.getHtmlFolder()))
				return htmlPair;
		for (Scope subScope : subScopes) {
			HTMLPair htmlPair = subScope.getHTMLPair(directory);
			if (htmlPair != null) return htmlPair;
		}
		return null;
	}

	/**
	 * Returns whether the given HTML pair has a representation directly under
	 * this <tt>Scope</tt> (non-recursive).
	 */
	public boolean isHTMLFileRegistered(File candidate) {
		for (HTMLPair htmlPair : subHTMLPairs)
			if (htmlPair.file.equals(candidate))
				return true;
		return false;
	}

	/**
	 * Returns the registered child of the receiver that represents the given
	 * file. Non-recursive.
	 */
	public Scope getRegisteredScope(File dir) {
		for (Scope scope : subScopes)
			if (scope.file.equals(dir))
				return scope;
		return null;
	}

}