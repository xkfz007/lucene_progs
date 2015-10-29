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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.enumeration.Font;
import net.sourceforge.docfetcher.enumeration.Icon;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.model.Job;
import net.sourceforge.docfetcher.model.RootScope;
import net.sourceforge.docfetcher.model.ScopeRegistry;
import net.sourceforge.docfetcher.parse.ParseException;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.UtilGUI;
import net.sourceforge.docfetcher.util.UtilList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * A wrapper for the shell on which the user can add and configure indexing jobs
 * and on which he will receive feedback about the running indexing processes.
 * 
 * @author Tran Nam Quang
 */
public class IndexingDialog {
 
	/** Fired after the indexing dialog is closed. */
	public final Event<IndexingDialog> evtClosed = new Event<IndexingDialog> ();
	
	private Shell shell;
	private CTabFolder tabFolder;
	private ToolItem addButton;
	private Event.Listener<ScopeRegistry> regObserver;
	private IndexingTab activeTab;
	private DirectoryDialog directoryDialog;
	
	/**
	 * Whether the indexing box should try to get the attention of the user
	 * after all indexing jobs are completed. This variable will be set to true
	 * when the first user-initiated job is entered into the queue.
	 */
	private boolean forceShellActiveOnClose = false;

	public IndexingDialog(Shell parentShell) {
		// Create and configure shell
		shell = new Shell(parentShell, Const.DIALOG_STYLE);
		shell.setText(Msg.index_management.value());
		shell.setImage(Icon.INDEX_MANAGEMENT.getImage());
		shell.setLayout(FillLayoutFactory.getInst().margin(5).create());

		// Create containers
		Composite container = new Composite(shell, SWT.NONE);
		container.setLayout(new FillLayout());
		tabFolder = new CTabFolder(container, SWT.CLOSE | SWT.BORDER);
		tabFolder.setSimple(false);

		// Create add button in the top right menu
		ToolBar toolBar = new ToolBar(tabFolder, SWT.FLAT);
		addButton = new ToolItem(toolBar, SWT.FLAT);
		addButton.setImage(Icon.ADD.getImage());
		addButton.setToolTipText(Msg.add_to_queue.value());
		tabFolder.setTopRight(toolBar);
		tabFolder.setTabHeight((int) (toolBar.getSize().y * 1.5)); // A factor of 1.0 would crop the add button image.

		// Create new tab when add button pressed
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addJobFromDialog();
			}
		});

		// For some unknown reason, the focus always goes to the ToolBar items
		toolBar.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				tabFolder.forceFocus();
			}
		});

		// Handle closing tab items
		tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
			public void close(CTabFolderEvent event) {
				onTabClosed(event);
			}
		});

		// Handle closing this window
		shell.addShellListener(new ShellAdapter() {
			public void shellClosed(ShellEvent event) {
				onIndexingBoxClosed(event);
			}
		});

		// Synchronize tabs to items in the indexing queue
		ScopeRegistry.getInstance().getEvtQueueChanged().add(
				regObserver = new Event.Listener<ScopeRegistry> () {
					public void update(ScopeRegistry scopeReg) {
						onQueueChanged();
					}
				});

		// Save shell size after changes
		shell.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				if (shell.getMaximized())
					return;
				Point size = shell.getSize();
				Pref.Int.IndexingBoxWidth.setValue(size.x);
				Pref.Int.IndexingBoxHeight.setValue(size.y);
			}
		});
	}

	/**
	 * Adds a job to the queue by letting the user choose from a directory in
	 * the directory dialog. Returns true if job was successfully added. A
	 * return value of false might result from the user aborting the directory
	 * dialog.
	 */
	public boolean addJobFromDialog() {
		directoryDialog = new DirectoryDialog(tabFolder.getShell(), SWT.PRIMARY_MODAL);
		directoryDialog.setText(Msg.scope_folder_title.value());
		directoryDialog.setMessage(Msg.scope_folder_msg.value());
		String lastPath = Pref.Str.LastIndexedFolder.getValue();
		if (! new File(lastPath).exists())
			lastPath = Pref.Str.LastIndexedFolder.defaultValue;
		directoryDialog.setFilterPath(lastPath);
		String path = directoryDialog.open();
		directoryDialog = null;
		if (path == null)
			return false;
		Pref.Str.LastIndexedFolder.setValue(path);
		File dir = new File(path);
		RootScope newScope = new RootScope(dir);

		// Check for intersections, if so, show warning message and open the dialog again
		String msg = ScopeRegistry.getInstance().checkIntersection(newScope);
		if (msg != null) {
			UtilGUI.showWarningMsg(msg);
			return addJobFromDialog();
		}

		addUncheckedJob(new Job(newScope, true, true));

		return true;
	}

	/**
	 * Adds the given job the indexing queue. Does nothing if the given job
	 * intersects with registered entries or entries in the indexing queue.
	 * Returns whether the job has been added successfully or not.
	 * <p>
	 * Note: This does not make the indexing dialog visible. An additional call
	 * to IndexingDialog.open() is needed for that.
	 */
	public boolean addJob(Job newJob) {
		ScopeRegistry scopeReg = ScopeRegistry.getInstance();
		if (newJob.isAddToRegistry()) {
			String msg = scopeReg.checkIntersection(newJob.getScope());
			if (msg != null)
				return false;
		}
		// For update requests only check for queue intersections
		// Allow duplicate in queue if the existing queue item is being processed
		else if (scopeReg.intersectsInactiveQueue(newJob.getScope()))
			return false;
		addUncheckedJob(newJob);
		return true;
	}

	/**
	 * Adds the given job the indexing queue. Does not check whether the given
	 * job intersects with registered entries or entries in the indexing queue.
	 */
	public void addUncheckedJob(final Job newJob) {
		File file = newJob.getScope().getFile();
		String tabTitle = file.getName();
		if (tabTitle.equals("")) //$NON-NLS-1$
			tabTitle = file.getAbsolutePath();
		if (tabFolder == null)
			return; // This can happen sometimes
		
		// Create tab item
		final CTabItem tabItem = new CTabItem(tabFolder, SWT.NONE);
		tabItem.setText(UtilList.truncate(tabTitle));
		if (newJob.isReadyForIndexing())
			tabItem.setImage(Icon.WALK_TREE.getImage());
		else
			tabItem.setImage(Icon.CHECK.getImage());
		
		// Create tab control
		forceShellActiveOnClose |= newJob.isAddToRegistry() || newJob.isDoRebuild();
		IndexingTab tabControl = new IndexingTab(tabFolder, newJob);
		tabItem.setControl(tabControl);
		tabControl.setFocus(); // Move focus away from tab item, or else the tab title will be underlined
		
		ScopeRegistry.getInstance().addJob(newJob); // Must be done after the previous line
		tabFolder.setSelection(tabItem);
		newJob.evtReadyStateChanged.add(new Event.Listener<Job> () {
			public void update(Job job) {
				if (job.isReadyForIndexing())
					tabItem.setImage(Icon.WALK_TREE.getImage());
				switchToNextWaitingTab();
			}
		});
	}

	/**
	 * Switches to next waiting tab after the run button has been clicked.
	 */
	private void switchToNextWaitingTab() {
		for (CTabItem tabItem : tabFolder.getItems()) {
			IndexingTab indexingTab = (IndexingTab) tabItem.getControl();
			Job job = indexingTab.getJob();
			if (!job.isReadyForIndexing()) {
				tabFolder.setSelection(tabItem);
				return;
			}
		}
	}

	/**
	 * Handles changes in the underlying indexing queue.
	 */
	private void onQueueChanged() {
		// Remove tabs which display no parse errors and the queue items of which have been discarded
		Job[] queueJobs = ScopeRegistry.getInstance().getJobs();
		List<Job> remainingTabJobs = new ArrayList<Job>();
		for (CTabItem tabItem : tabFolder.getItems()) {
			IndexingTab indexingTab = (IndexingTab) tabItem.getControl();
			Job job = indexingTab.getJob();
			if (job.isReadyForIndexing()
					&& !UtilList.containsIdentity(queueJobs, job)) {
				if (!job.getScope().isFinishedWithErrors()
						|| (!job.isAddToRegistry() && !job.isDoRebuild())) {
					if (!tabItem.isDisposed() && Pref.Bool.CloseIndexingTabs.getValue())
						tabItem.dispose(); // Finished without parse errors, close it
				} else
					tabItem.setImage(Icon.WARNING_BIG.getImage()); // Finished with parse errors
			} else
				remainingTabJobs.add(job); // Still waiting for processing
		}

		// Add tab for new queue items
		for (Job queueJob : queueJobs) {
			if (!remainingTabJobs.contains(queueJob)) {
				addJob(queueJob);
			}
		}

		// Find tab that corresponds to current job
		activeTab = null;
		Job currentJob = ScopeRegistry.getInstance().getCurrentJob();
		for (CTabItem tabItem : tabFolder.getItems()) {
			IndexingTab indexingTab = (IndexingTab) tabItem.getControl();
			if (indexingTab.getJob() == currentJob) {
				activeTab = indexingTab;
				tabItem.setFont(Font.SYSTEM_BOLD.getFont());
			} else
				tabItem.setFont(Display.getDefault().getSystemFont());
		}

		// Close window if all tabs and the directory dialog are closed
		if (tabFolder.getItemCount() == 0 && directoryDialog == null)
			close();

		/*
		 * Force focus on app if all submitted jobs (i.e. 'submit' button has
		 * been clicked on their tabs) have been finished.
		 */
		if (ScopeRegistry.getInstance().getSubmittedJobs().length == 0
				&& shell.getDisplay().getActiveShell() == null // Only do this if app has lost focus
				&& forceShellActiveOnClose) {
			DocFetcher docFetcher = DocFetcher.getInstance();
			if (docFetcher.isInSystemTray())
				docFetcher.restoreFromSystemTray();
			else {
				docFetcher.getShell().forceActive();
			}
			forceShellActiveOnClose = false;
		}
	}

	/**
	 * Handles closing a tab.
	 */
	private void onTabClosed(CTabFolderEvent event) {
		if (event.item.isDisposed())
			return;
		IndexingTab tab = (IndexingTab) ((CTabItem) event.item).getControl();
		Job job = tab.getJob();
		
		// Ask for confirmation if the indexing job to be stopped is a full index (re)creation.
		if (job.isDoRebuild()
				&& job.equals(ScopeRegistry.getInstance().getCurrentJob())) {
			int ans = UtilGUI.showConfirmMsg(Msg.discard_incomplete_index.value());
			if (ans != SWT.OK) {
				event.doit = false;
				return;
			}
		}
		ScopeRegistry scopeReg = ScopeRegistry.getInstance();
		scopeReg.getEvtQueueChanged().remove(regObserver);
		scopeReg.removeFromQueue(job);
		scopeReg.getEvtQueueChanged().add(regObserver);
	}

	/**
	 * Handles closing the indexing box.
	 */
	private void onIndexingBoxClosed(ShellEvent e) {
		e.doit = false;
		ScopeRegistry scopeReg = ScopeRegistry.getInstance();
		Job job = scopeReg.getCurrentJob();
		if (job != null && job.isDoRebuild()) {
			int ans = UtilGUI.showConfirmMsg(Msg.discard_incomplete_index.value());
			if (ans != SWT.OK)
				return;
		}
		
		scopeReg.getEvtQueueChanged().remove(regObserver); // Suppress observer notification
		for (CTabItem tabItem : tabFolder.getItems())
			tabItem.dispose();
		scopeReg.clearQueue();
		scopeReg.getEvtQueueChanged().add(regObserver);
		
		try {
			scopeReg.save();
		} catch (IOException e1) {
			UtilGUI.showErrorMsg(Msg.write_error.value());
		}
		close();
	}

	/**
	 * Opens the indexing box.
	 */
	public void open() {
		int width = Pref.Int.IndexingBoxWidth.getValue();
		int height = Pref.Int.IndexingBoxHeight.getValue();
		shell.setSize(width, height);
		UtilGUI.centerShell((Shell) shell.getParent(), shell);
		shell.setMinimized(false); // In case the user had minimized the shell previously
		
		// Open the indexing box only if DocFetcher doesn't hide in the system tray
		if (DocFetcher.getInstance().getShell().isVisible()) {
			shell.open();
			tabFolder.setSelectionBackground(UtilGUI.getColor(SWT.COLOR_TITLE_BACKGROUND));
			tabFolder.setSelectionForeground(UtilGUI.getColor(SWT.COLOR_TITLE_FOREGROUND));
		}
	}

	/**
	 * Returns whether the indexing box is open.
	 */
	public boolean isOpen() {
		return shell.isVisible();
	}

	/**
	 * Closes the indexing box.
	 */
	public void close() {
		shell.setVisible(false);
		evtClosed.fireUpdate(this);
	}

	/**
	 * Appends the given message at the end of the currently active feedback
	 * textbox.
	 */
	public void appendInfo(String str) {
		if (activeTab != null)
			activeTab.appendInfo(str);
	}

	/**
	 * Adds a parse exception to the currently active feedback textbox.
	 */
	public void addError(ParseException error) {
		if (activeTab != null)
			activeTab.addError(error);
	}

	/**
	 * Returns the parse exceptions of the currently active tab, or an empty
	 * array if no tab is active.
	 */
	public ParseException[] getErrors() {
		return activeTab == null ? new ParseException[] {} : activeTab.getErrors();
	}

	/**
	 * Returns all tabs.
	 */
	public IndexingTab[] getIndexingTabs() {
		CTabItem[] ctabItems = tabFolder.getItems();
		IndexingTab[] indexingTabs = new IndexingTab[ctabItems.length];
		for (int i = 0; i < ctabItems.length; i++)
			indexingTabs[i] = (IndexingTab) ctabItems[i].getControl();
		return indexingTabs;
	}

	/**
	 * Returns the shell used to display the indexing box controls.
	 */
	public Shell getShell() {
		return shell;
	}

}
