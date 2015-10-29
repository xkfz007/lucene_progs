/*******************************************************************************
 * Copyright (c) 2007, 2008 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *    Tonio Rush - interaction with the daemon
 *******************************************************************************/

package net.sourceforge.docfetcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.docfetcher.enumeration.Icon;
import net.sourceforge.docfetcher.enumeration.Key;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.model.HTMLPair;
import net.sourceforge.docfetcher.model.Job;
import net.sourceforge.docfetcher.model.ResultDocument;
import net.sourceforge.docfetcher.model.RootScope;
import net.sourceforge.docfetcher.model.Scope;
import net.sourceforge.docfetcher.model.ScopeRegistry;
import net.sourceforge.docfetcher.model.ScopeRegistry.SearchException;
import net.sourceforge.docfetcher.parse.Parser;
import net.sourceforge.docfetcher.parse.ParserRegistry;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.UtilFile;
import net.sourceforge.docfetcher.util.UtilGUI;
import net.sourceforge.docfetcher.view.FilesizeGroup;
import net.sourceforge.docfetcher.view.FillLayoutFactory;
import net.sourceforge.docfetcher.view.FormDataFactory;
import net.sourceforge.docfetcher.view.HotkeyHandler;
import net.sourceforge.docfetcher.view.IndexingDialog;
import net.sourceforge.docfetcher.view.MainPanel;
import net.sourceforge.docfetcher.view.ParserGroup;
import net.sourceforge.docfetcher.view.PrefDialog;
import net.sourceforge.docfetcher.view.PreviewPanel;
import net.sourceforge.docfetcher.view.ResultPanel;
import net.sourceforge.docfetcher.view.SashWeightHandler;
import net.sourceforge.docfetcher.view.ScopeGroup;
import net.sourceforge.docfetcher.view.SearchPanel;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.eclipse.swt.widgets.Widget;

/**
 * The main application window.
 * 
 * @author Tran Nam Quang
 * @author Tonio Rush
 */
public class DocFetcher extends ApplicationWindow {
	
	private static DocFetcher docFetcher;
	private static String[] startParams;
	private HotkeyHandler hotkeyHandler;
	private ExceptionHandler exceptionHandler;
	public static String appName;
	
	private Composite filterPanel;
	private SashForm sashHorizontal;
	private SashForm sashLeft;
	private MainPanel mainPanel;
	private SearchPanel searchPanel;
	private PreviewPanel previewPanel;
	private ResultPanel resultPanel;
	private IndexingDialog indexingDialog;
	private TrayItem trayItem;
	private Clipboard clipboard; // must be disposed
	
	private ScopeRegistry scopeReg;
	private FolderWatcher folderWatcher;
	private FilesizeGroup filesizeGroup;
	private ParserGroup parserGroup;
	private ScopeGroup scopeGroup;

	public static void main(String[] args) {
		Pref.load(); // Preferences must be loaded before invoking the command line handler
		if (! CommandLineHandler.handle(args))
			return;
		
		startParams = args;
		docFetcher = new DocFetcher();
		docFetcher.setBlockOnOpen(true);
		docFetcher.open();
		Display.getCurrent().dispose();
		
		/*
		 * Launch the daemon on the installed Linux version (because the debian
		 * installer can't do it).
		 */
		if (Const.IS_LINUX && ! Const.IS_PORTABLE) {
			try {
				Runtime.getRuntime().exec("/usr/share/docfetcher/docfetcher-daemon-linux"); //$NON-NLS-1$
			} catch (IOException e) {
				// Ignore
			}
		}
	}
	
	private DocFetcher() {
		super(null);
		addStatusLine();
		appName = Pref.Str.AppName.getValue();
		if (appName.trim().equals("")) //$NON-NLS-1$
			appName = "DocFetcher"; //$NON-NLS-1$
		Display.setAppName(DocFetcher.appName);
	}
	
	public static DocFetcher getInstance() {
		return docFetcher;
	}

	protected void initializeBounds() {
		// Set shell size
		final Shell shell = getShell();
		int shellWidth = Pref.Int.ShellWidth.getValue();
		int shellHeight = Pref.Int.ShellHeight.getValue();
		shell.setSize(shellWidth, shellHeight);
		
		/*
		 * Set shell location. Must be done after setting the shell size,
		 * because the Util.centerShell(..) method depends on a correct shell
		 * size.
		 */
		int shellX = Pref.Int.ShellX.getValue();
		int shellY = Pref.Int.ShellY.getValue();
		if (shellX < 0 || shellY < 0)
			UtilGUI.centerShell(null, shell);
		else
			shell.setLocation(shellX, shellY);
		
		shell.setMaximized(Pref.Bool.ShellMaximized.getValue());

		// Set sash weights
		// Note: This must be done AFTER setting the maximization state of the main shell!
		sashHorizontal.setWeights(Pref.IntArray.SashHorizontalWeights.getValue());
		sashLeft.setWeights(Pref.IntArray.SashLeftWeights.getValue());
		
		// Couple preferences with shell state
		shell.addControlListener(new ControlAdapter() {
			public void controlMoved(ControlEvent e) {
				if (shell.getMaximized() || ! shell.isVisible())
					return;  // Don't store shell position when it's maximized or invisible
				Point pos = shell.getLocation();
				Pref.Int.ShellX.setValue(pos.x);				Pref.Int.ShellY.setValue(pos.y);			}
			public void controlResized(ControlEvent e) {
				if (shell.getMaximized() || ! shell.isVisible())
					return; // Don't store shell size when it's maximized or invisible
				Point size = shell.getSize();
				Pref.Int.ShellWidth.setValue(size.x);				Pref.Int.ShellHeight.setValue(size.y);			}
		});
	}
	
	protected void configureShell(final Shell shell) {
		shell.setText(DocFetcher.appName);
		shell.setImages(new Image[] {
				Icon.DOCFETCHER16.getImage(),
				Icon.DOCFETCHER32.getImage(),
				Icon.DOCFETCHER48.getImage(),
		});
		super.configureShell(shell);
	}
	
	protected Control createContents(Composite parent) {
		clipboard = new Clipboard(getShell().getDisplay());
		
		// Create widgets
		Composite topContainer = new Composite(parent, SWT.NONE);
		topContainer.setLayout(FillLayoutFactory.getInst().margin(5).create());
		sashHorizontal = new SashForm(topContainer, SWT.HORIZONTAL);
		filterPanel = new Composite(sashHorizontal, SWT.NONE);
		mainPanel = new MainPanel(sashHorizontal);
		searchPanel = mainPanel.getSearchPanel();
		previewPanel = mainPanel.getPreviewPanel();
		resultPanel = mainPanel.getResultPanel();
		filesizeGroup = new FilesizeGroup(filterPanel);
		sashLeft = new SashForm(filterPanel, SWT.VERTICAL | SWT.SMOOTH);
		parserGroup = new ParserGroup(sashLeft);
		scopeGroup = new ScopeGroup(sashLeft);
		
		// Layout
		filterPanel.setLayout(new FormLayout());
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.top(0, 0).left(0, 0).right(100, -5).applyTo(filesizeGroup);
		fdf.top(filesizeGroup).bottom(100, 0).applyTo(sashLeft);
		
		// Load settings
		filterPanel.setVisible(Pref.Bool.ShowFilterPanel.getValue());
		
		// Create indexing box
		indexingDialog = new IndexingDialog(getShell());
		
		// Try to show help page
		boolean internalBrowserAvailable = false;
		if (Pref.Bool.ShowWelcomePage.getValue() && Pref.Bool.ShowPreview.getValue())
			internalBrowserAvailable = mainPanel.showHelpPage();
		else
			DocFetcher.getInstance().setStatus(Msg.press_help_button.format(Key.Help.toString()));
		Pref.Bool.ShowWelcomePage.setValue(internalBrowserAvailable);		
		// Move text cursor to search box
		mainPanel.focusSearchBox();
		
		// Toggle filter panel when its preferences value changes
		Pref.Bool.ShowFilterPanel.evtChanged.add(new Event.Listener<Boolean> () {
			public void update(Boolean eventData) {
				filterPanel.setVisible(eventData);
				sashHorizontal.layout();
			}
		});
		
		/*
		 * Update the result panel on changes to the values in the filesize group.
		 */
		class FilesizeResultFilter extends Event.Listener<long[]> implements ResultPanel.ResultFilter {
			private long minBytes = 0;
			private long maxBytes = -1;
			public List<ResultDocument> select(Collection<ResultDocument> docs) {
				/*
				 * If no filesize filters are set, we can simply return the
				 * input. This is especially important if the number of result
				 * documents is huge, because bypassing the rest of this filter
				 * can save a lot of processing time.
				 */
				if (minBytes == 0 && maxBytes == -1)
					return new ArrayList<ResultDocument> (docs);
				
				List<ResultDocument> selected = new ArrayList<ResultDocument>(docs.size());
				for (ResultDocument doc : docs) {
					long targetSize = doc.getFile().length();
					boolean minPassed = minBytes <= targetSize;
					boolean maxPassed = maxBytes == -1 ? true : targetSize <= maxBytes;
					if (minPassed && maxPassed)
						selected.add(doc);
				}
				return selected;
			}
			public void update(long[] eventData) {
				minBytes = eventData[0];
				maxBytes = eventData[1];
				resultPanel.refresh();
			}
		}
		FilesizeResultFilter filesizeResultFilter = new FilesizeResultFilter();
		resultPanel.addFilter(filesizeResultFilter);
		filesizeGroup.evtValuesChanged.add(filesizeResultFilter);
		
		// Update result panel when parsers are checked/unchecked
		Event.Listener<Parser> parserListener = new Event.Listener<Parser> () {
			public void update(Parser parser) {
				resultPanel.refresh();
			}
		};
		for (Parser parser : ParserRegistry.getParsers())
			parser.evtCheckStateChanged.add(parserListener);
		
		// Update result panel when scopes are checked/unchecked
		Scope.checkStateChanged.add(new Event.Listener<Scope> () {
			public void update(Scope scope) {
				resultPanel.refresh();
			}
		});
		
		// Update result panel after removal of a RootScope
		ScopeRegistry.getInstance().getEvtRegistryRootChanged().add(new Event.Listener<ScopeRegistry> () {
			public void update(ScopeRegistry eventData) {
				resultPanel.refresh();
			}
		});
		
		// Display manual when the help button is clicked
		searchPanel.evtHelpBtClicked.add(new Event.Listener<Widget> () {
			public void update(Widget eventData) {
				if (previewPanel.showHelpPage())
					Pref.Bool.ShowPreview.setValue(true);
			}
		});
		
		// Open up preferences dialog when the preferences button is clicked
		searchPanel.evtPrefBtClicked.add(new Event.Listener<Widget> () {
			public void update(Widget eventData) {
				new PrefDialog(getShell());
			}
		});
		
		// Previous page button
		searchPanel.evtLeftBtClicked.add(new Event.Listener<Widget> () {
			public void update(Widget eventData) {
				resultPanel.previousPage();
			}
		});
		
		// Next page button
		searchPanel.evtRightBtClicked.add(new Event.Listener<Widget> () {
			public void update(Widget eventData) {
				resultPanel.nextPage();
			}
		});
		
		// Handle content changes on the result panel
		resultPanel.evtVisibleItemsChanged.add(new Event.Listener<ResultPanel> () {
			public void update(ResultPanel resultPanel) {
				searchPanel.setLeftBtEnabled(resultPanel.getPageIndex() > 0);
				searchPanel.setRightBtEnabled(resultPanel.getPageIndex() < resultPanel.getPageCount() - 1);
				showResultStatus();
			}
		});
		
		// Selection changes in result panel change content of preview panel
		resultPanel.evtSelectionChanged.add(new Event.Listener<ResultPanel> () {
			public void update(ResultPanel resultPanel) {
				ResultDocument doc = (ResultDocument) resultPanel.getSelection().getFirstElement();
				if (doc == null) return;
				previewPanel.setFile(doc.getFile(), doc.getParser(), doc.getQuery());
				showResultStatus();
			}
		});
		
		// Handle request to list documents
		scopeGroup.evtListDocuments.add(new Event.Listener<ResultDocument[]> () {
			public void update(ResultDocument[] docs) {
				resultPanel.setResults(docs);
			}
		});
		
		// Handle search requests
		searchPanel.evtSearchRequest.add(new Event.Listener<String> () {
			public void update(String searchString) {
				doSearch(searchString);
			}
		});
		
		/*
		 * Global keys
		 */
		Display.getDefault().addFilter(SWT.KeyDown, new Listener() {
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				/*
				 * FIXME This line fixes a bug in SWT 3.2.2 / Windows: If the
				 * current tab displays an SWT browser widget (e.g. this app's
				 * manual) and the user hits a key, two key events are
				 * triggered, one from the Browser class and one from a class
				 * named Website. This means, for example, that if the key for
				 * hiding/showing the search bar is pressed, its visibility
				 * state will not change after all, because it will be
				 * hidden/shown right after it was shown/hidden. Thus we're
				 * intercepting key events from the Website class here. This is
				 * done using a name check instead of 'instanceof' since this
				 * class is not visible.
				 */
				if (event.widget.getClass().getSimpleName().equals("WebSite")) return; //$NON-NLS-1$
				
				Key key = Key.getKey(event.stateMask, event.keyCode);
				if (key == null) return;
				
				// Disable global keys when the main shell is inactive
				if (Display.getCurrent().getActiveShell() != DocFetcher.getInstance().getShell()) return;
				event.doit = false;
				
				switch (key) {
				case Help: mainPanel.showHelpPage(); break;
				case FocusSearchBox:
				case FocusSearchBox2: searchPanel.setFocus(); break;
				case FocusFilesizeGroup: filesizeGroup.setFocus(); break;
				case FocusParserGroup: parserGroup.setFocus(); break;
				case FocusScopeGroup: scopeGroup.setFocus(); break;
				case FocusResults: resultPanel.setFocus(); break;
				default: event.doit = true;
				}
			}
		});

		/*
		 * Add Hotkey support.
		 */
		hotkeyHandler = new HotkeyHandler();
		hotkeyHandler.evtHotkeyPressed.add(new Event.Listener<HotkeyHandler> () {
			public void update(HotkeyHandler eventData) {
				if (isInSystemTray()) {
					restoreFromSystemTray();
				} else {
					Shell shell = getShell();
					shell.setMinimized(false);
					shell.setVisible(true);
					shell.forceActive();
				}
			}
		});
		hotkeyHandler.evtHotkeyConflict.add(new Event.Listener<int[]> () {
			public void update(int[] eventData) {
				String key = Key.toString(eventData);
				UtilGUI.showWarningMsg(Msg.hotkey_in_use.format(key));
				
				/*
				 * Don't open preferences dialog when the hotkey conflict occurs
				 * at startup.
				 */
				if (getShell().isVisible())
					new PrefDialog(getShell());
			}
		});
		hotkeyHandler.registerHotkey();
		
		scopeReg = ScopeRegistry.getInstance();
		
		// Remove scope registry entries whose index folders don't exist anymore
		RootScope[] rootScopes = scopeReg.getEntries();
		for (RootScope rootScope : rootScopes)
			if (! rootScope.getIndexDir().exists())
				scopeReg.remove(rootScope);
		
		/*
		 * Wipe out unregistered index folders (possibly from older
		 * installations or program crashes).
		 */
		File[] indexDirs = UtilFile.listFolders(Const.INDEX_PARENT_FILE);
		for (File indexDir : indexDirs)
			if (! scopeReg.containsIndexDir(indexDir) && indexDir.getName().matches(".*_[0-9]+")) //$NON-NLS-1$
				UtilFile.delete(indexDir, true);
		
		// Hook onto scope registry and set app name according to number of jobs
		scopeReg.getEvtQueueChanged().add(new Event.Listener<ScopeRegistry> () {
			public void update(ScopeRegistry scopeReg) {
				Shell shell = getShell();
				if (shell == null) return;
				int count = scopeReg.getSubmittedJobs().length;
				String prefix = Msg.jobs.format(count) + " - "; //$NON-NLS-1$
				shell.setText((count == 0 ? "" : prefix) + DocFetcher.appName); //$NON-NLS-1$
			}
		});
		
		folderWatcher = new FolderWatcher(); // must be done after loading the Prefs
		
		parserGroup.setParsers(ParserRegistry.getParsers());
		scopeGroup.setScopes(scopeReg.getEntries());
		createTemporaryIndexes();

		/*
		 * Enabling the sash weight handler must be delayed using a Thread;
		 * otherwise we would get a nasty layout bug that would shrink the width
		 * of the filter panel after each subsequent program launch, provided
		 * the program is terminated in maximized state.
		 */
		new Thread() {
			public void run() {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						new SashWeightHandler(getShell(), sashHorizontal);
					}
				});
			}
		}.start();
		
		/*
		 * Check if daemon has detected changes in the indexed folders.
		 * For each change, launches an update
		 */
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(Const.INDEX_DAEMON_FILE));
			String line;
			while((line = reader.readLine()) != null){
				// comment line
				if(line.length() >= 2 && line.substring(0, 1).equals("//")) //$NON-NLS-1$
					continue;
				
				// changed file
				if(line.length() > 1 && line.charAt(0) == '#'){
					String folder_changed = line.substring(1);
					RootScope rs = scopeReg.getEntry(new File(folder_changed));

					if(rs==null){
						// directory unknown ???
						continue;
					}

					// update index
					indexingDialog.addJob(new Job(rs, false, false));
				}
			}
		} catch (FileNotFoundException e) {
			// Can't print stacktrace here, no GUI available
		} catch (IOException e) {
			// Can't print stacktrace here, no GUI available
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException e) {
				// Can't print stacktrace here, no GUI available
			}
		}

		/*
		 * We do this at the end of this method (instead of at the beginning of
		 * main) so developers can see a stacktrace in the Eclipse console if
		 * they haven't set up the run configuration appropriately.
		 */
		exceptionHandler = new ExceptionHandler();
		exceptionHandler.setEnabled(true);
		
		return topContainer;
	}
	
	/**
	 * Creates temporary indexes from start parameters.
	 */
	private void createTemporaryIndexes() {
		if (startParams == null || startParams.length == 0) return;
		
		// Create RootScope objects from start parameters
		final List<RootScope> newRootScopes = new ArrayList<RootScope> ();
		for (File file : UtilFile.getDissociatedDirectories(startParams))
			newRootScopes.add(new RootScope(file));

		if (newRootScopes.isEmpty()) {
			setStatus(Msg.invalid_start_params.value());
			return;
		}
		
		// Check all given RootScopes/Scopes after indexing, uncheck everything else
		indexingDialog.evtClosed.add(new Event.Listener<IndexingDialog> () {
			public void update(IndexingDialog eventData) {
				for (RootScope rootScope : scopeReg.getEntries())
					rootScope.setCheckedDeep(false);
				for (RootScope newRootScope : newRootScopes) {
					Scope scope = scopeReg.getScopeDeep(newRootScope.getFile());
					if (scope != null)
						scope.setCheckedDeep(true);
				}
				scopeGroup.updateCheckStates();
				indexingDialog.evtClosed.remove(this); // unregister listener
			}
		});

		// Start indexing and open up the indexing dialog
		boolean jobsAdded = false;
		for (RootScope rs : newRootScopes) {
			/*
			 * Note: Through this pathway, the user can create
			 * intersecting RootScopes: For example, the user can create
			 * a permanent index "C:\Docs\Stuff" and then create a
			 * temporary index for "C:\Docs". This case is not
			 * intercepted in the following for-if construct because
			 * allowing it seems to be more convenient for the user.
			 * 
			 * Note 2: The user can also search in HTML folders, even if
			 * there were already indexed.
			 */
			boolean allow = true;
			for (RootScope oldScope : scopeReg.getEntries()) {
				if (oldScope.equals(rs)) {
					allow = false;
					break;
				}
				if (oldScope.contains(rs)) {
					HTMLPair htmlPair = scopeReg.getHTMLPair(rs.getFile());
					allow = htmlPair != null;
					break;
				}
			}
			if (allow) {
				rs.setDeleteOnExit(true);
				indexingDialog.addUncheckedJob(new Job(rs, true, true));
				jobsAdded = true;
			}
		}
		
		// Only open indexing dialog if at least one new folder was indexed
		if (jobsAdded) {
			new Thread() {
				public void run() {
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							/*
							 * This wouldn't work outside the thread,
							 * because at this point the main shell
							 * hasn't been created yet.
							 */
							indexingDialog.open();
						}
					});
				}
			}.start();
		}
		else // Call the evtClosed handler anyway
			indexingDialog.evtClosed.fireUpdate(indexingDialog);
	}
	
	/**
	 * Returns the application's clipboard.
	 */
	public Clipboard getClipboard() {
		return clipboard;
	}

	public boolean close() {
		/*
		 * If an indexing process is running in the background, ask the user
		 * before terminating it and exiting.
		 */
		if (scopeReg.getCurrentJob() != null) {
			int ans = UtilGUI.showConfirmMsg(Msg.force_quit.value());
			if (ans != SWT.OK) return false;
			scopeReg.clearQueue();
		}
		
		// Delete the RootScopes that were marked for deletion
		for (RootScope rootScope : scopeReg.getTemporaryEntries())
			scopeReg.remove(rootScope);
		
		if (clipboard != null && ! clipboard.isDisposed())
			clipboard.dispose();
		
		Pref.Bool.FirstLaunch.setValue(false);		Pref.Bool.ShellMaximized.setValue(getShell().getMaximized());		
		// Store sash weights
		Pref.IntArray.SashHorizontalWeights.setValue(sashHorizontal.getWeights());		Pref.IntArray.SashLeftWeights.setValue(sashLeft.getWeights());		mainPanel.saveWeights();
		
		// Clear search history if the user wants it that way
		if (Pref.Bool.ClearSearchHistoryOnExit.getValue())
			Pref.StrArray.SearchHistory.setValue();
		
		// Save preferences and registries
		try {
			Pref.save();
			scopeReg.save();
		} catch (IOException e) {
			UtilGUI.showErrorMsg(Msg.write_error.value());
		}
		
		if (hotkeyHandler != null)
			hotkeyHandler.shutdown();
		exceptionHandler.closeErrorFile();
		folderWatcher.shutdown(); // On Windows, this may cause a crash, so we do this last
		
		return super.close();
	}
	
	/**
	 * Sets an error message on the status line.
	 */
	public void setErrorStatus(String msg) {
		if (msg == null || msg.equals("")) //$NON-NLS-1$
			getStatusLineManager().setErrorMessage(null);
		else
			getStatusLineManager().setErrorMessage(Icon.WARNING.getImage(), msg);
	}
	
	public void setStatus(String msg) {
		if (msg == null || msg.equals("")) //$NON-NLS-1$
			getStatusLineManager().setMessage(null, null);
		else
			getStatusLineManager().setMessage(Icon.INFO.getImage(), msg);
	}
	
	private void doSearch(final String searchString) {
		// Disallow empty search strings
		String errorMsg = searchPanel.checkSearchDisabled();
		
		// Disallow search when there are no indexes to search in
		if (ScopeRegistry.getInstance().getEntries().length == 0)
			errorMsg = Msg.search_scope_empty.value();
		
		// Check for correct filesizes
		String filesizeGroupMsg = filesizeGroup.checkSearchDisabled();
		if (filesizeGroupMsg != null)
			errorMsg = filesizeGroupMsg;
		
		// At least one item in the filetype table must be checked
		if (! ParserRegistry.hasCheckedParsers())
			errorMsg = Msg.no_filetypes_selected.value();
		
		if (errorMsg != null) {
			UtilGUI.showWarningMsg(errorMsg);
			return;
		}
		
		/*
		 * Get query string and check if it starts with '*' or '?'. If so, warn
		 * the user about performance issues.
		 */
		if (searchString.startsWith("*") || searchString.startsWith("?")) { //$NON-NLS-1$ //$NON-NLS-2$
			boolean msgShown = Pref.Bool.LeadingWildcardMessageShown.getValue();
			if (! msgShown) {
				UtilGUI.showInfoMsg(Msg.leading_wildcard.value());
				Pref.Bool.LeadingWildcardMessageShown.setValue(true);
			}
		}
		
		searchPanel.setSearchBoxEnabled(false);
		new Thread() {
			public void run() {
				try {
					final ResultDocument[] results = scopeReg.search(searchString);
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							resultPanel.setResults(results);
							searchPanel.addToSearchHistory(searchString);
						}
					});
				}
				catch (SearchException e) {
					UtilGUI.showWarningMsg(e.getMessage());
				}
				finally {
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							searchPanel.setSearchBoxEnabled(true);
							resultPanel.setFocus();
						}
					});
				}
			}
		}.start();
	}
	
	/**
	 * Displays a message about the results on the status bar.
	 */
	private void showResultStatus() {
		// Get active result tab, clear status line if no result panel hasn't been created yet
		if (resultPanel == null) {
			setStatus(null);
			return;
		}
		
		// Get total number of visible result items
		int visibleResultCount = resultPanel.getVisibleResultCount();
		
		// Get number of selected results
		IStructuredSelection sel = resultPanel.getSelection();
		int selSize = sel.size();
		
		// Create status message
		String msg = null;
		String space = "     "; //$NON-NLS-1$
		
		// Simple message: "Results: 123"
		if (resultPanel.getPageCount() <= 1)
			msg = Msg.num_results.format(visibleResultCount);
		
		// More complicated message: "Results: 101-200 of 320	Page 2/4"
		else {
			int maxSize = Pref.Int.MaxResultsPerPage.getValue();
			int pageIndex = resultPanel.getPageIndex();
			int pageCount = resultPanel.getPageCount();
			int a = pageIndex * maxSize + 1;
			int b = pageIndex + 1 == pageCount ? visibleResultCount : (pageIndex + 1) * maxSize;
			msg = Msg.num_results_detail.format(new Object[] {a, b, visibleResultCount});
			msg += space + Msg.page_m_n.format(new Object[] {pageIndex + 1, pageCount});
		}
		
		// Append selection info if more than 1 item selected
		if (selSize > 1)
			msg += space + Msg.num_sel_results.format(selSize);
		
		setStatus(msg);
	}
	
	/**
	 * Removes the focus from any widget in the application.
	 */
	public void unfocus() {
		Control statusLine = getStatusLineManager().getControl();
		if (statusLine == null || statusLine.isDisposed()) return;
		statusLine.setFocus();
	}
	
	/**
	 * Sends the application to the system tray.
	 */
	public void toSystemTray() {
		// Get shell; return if we're already in the tray
		Shell shell = getShell();
		if (! shell.isVisible()) return;
		
		// Get tray; abort and display error message if it's not available
		Tray tray = shell.getDisplay().getSystemTray();
		if (tray == null) {
			UtilGUI.showErrorMsg(Msg.systray_not_available.value());
			return;
		}
		
		/*
		 * If DocFetcher is sent to the system tray while being maximized and
		 * showing a big file on the preview panel, one would experience an
		 * annoying delay once the program returns from the system tray. The
		 * workaround is to deactivate the preview panel before going to the
		 * system tray and reactivate it when we come back.
		 */
		previewPanel.setActive(false);
		
		/*
		 * For some reason the shell will have the wrong position without this
		 * when brought back from system tray.
		 */
		Point shellPos = shell.getLocation();
		Pref.Int.ShellX.setValue(shellPos.x);		Pref.Int.ShellY.setValue(shellPos.y);		
		// Create and configure tray item
		trayItem = new TrayItem (tray, SWT.NONE);
		trayItem.setToolTipText(DocFetcher.appName);
		
		// Set system tray icon
		if (Const.IS_LINUX)
			// On Linux, the default 16x16px image would be too small and lack transparency
			trayItem.setImage(Icon.DOCFETCHER_SYSTRAY_LINUX.getImage());
		else
			trayItem.setImage(Icon.DOCFETCHER16.getImage());
		
		final Menu trayMenu = new Menu(shell, SWT.POP_UP);
		MenuItem restoreItem = new MenuItem(trayMenu, SWT.PUSH);
		MenuItem closeItem = new MenuItem(trayMenu, SWT.PUSH);
		restoreItem.setText(Msg.restore_app.value());
		closeItem.setText(Msg.exit.value());
		trayMenu.setDefaultItem(restoreItem);
		
		shell.setVisible(false);
		
		/*
		 * Event handling
		 */
		// Open system tray menu when the user clicks on it
		trayItem.addMenuDetectListener(new MenuDetectListener() {
			public void menuDetected(MenuDetectEvent e) {
				trayMenu.setVisible(true);
			}
		});
		
		// Shut down application when user clicks on the 'close' tray item
		closeItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DocFetcher.this.close();
			}
		});
		
		// Restore application when user clicks on the 'restore' item or doubleclicks on the tray icon.
		Listener appRestorer = new Listener() {
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				restoreFromSystemTray();
			}
		};
		trayItem.addListener(SWT.Selection, appRestorer);
		restoreItem.addListener(SWT.Selection, appRestorer);
	}
	
	/**
	 * Returns whether the application is hidden in the system tray.
	 */
	public boolean isInSystemTray() {
		return trayItem != null;
	}
	
	/**
	 * Restores application from the system tray.
	 */
	public void restoreFromSystemTray() {
		Shell shell = getShell();
		shell.setVisible(true);
		shell.forceActive();
		shell.setMinimized(false);
		previewPanel.setActive(true);
		if (trayItem != null) {
			trayItem.dispose();
			trayItem = null;
		}
		shell.setLocation(Pref.Int.ShellX.getValue(), Pref.Int.ShellY.getValue());
		mainPanel.focusSearchBox();
	}
	
	/**
	 * Returns the indexing dialog. Will not return null.
	 */
	public IndexingDialog getIndexingDialog() {
		return indexingDialog;
	}
	
	/**
	 * Enables or disables the application's custom exception handler. Does
	 * nothing if the exception handler hasn't been created yet.
	 */
	public void setExceptionHandlerEnabled(boolean enabled) {
		if (exceptionHandler != null)
			exceptionHandler.setEnabled(enabled);
	}

	/**
	 * @see net.sourceforge.docfetcher.FolderWatcher#setWatchEnabled(boolean, java.util.Collection)
	 */
	public void setWatchEnabled(boolean enabled, Collection<RootScope> targets) {
		folderWatcher.setWatchEnabled(enabled, targets);
	}

	/**
	 * @see net.sourceforge.docfetcher.FolderWatcher#setWatchEnabled(boolean, net.sourceforge.docfetcher.model.RootScope[])
	 */
	public void setWatchEnabled(boolean enabled, RootScope... targets) {
		folderWatcher.setWatchEnabled(enabled, targets);
	}
	
	/**
	 * Extracts the program version and build date from the filename of the
	 * DocFetcher JAR file. Returns null if the extraction failed.
	 */
	public static String[] getProgramVersion() {
		Pattern pattern = Pattern.compile("net.sourceforge.docfetcher_(.*?)_(.*?).jar"); //$NON-NLS-1$
		for (File libFile : UtilFile.listFiles(new File("lib"))) { //$NON-NLS-1$
			Matcher matcher = pattern.matcher(libFile.getName());
			if (matcher.matches())
				return new String[] {matcher.group(1), matcher.group(2)};
		}
		return null;
	}
	
}
