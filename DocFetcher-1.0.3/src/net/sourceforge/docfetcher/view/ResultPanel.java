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

package net.sourceforge.docfetcher.view;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.enumeration.Icon;
import net.sourceforge.docfetcher.enumeration.Key;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.model.FileWrapper;
import net.sourceforge.docfetcher.model.HTMLPair;
import net.sourceforge.docfetcher.model.Job;
import net.sourceforge.docfetcher.model.ResultDocument;
import net.sourceforge.docfetcher.model.RootScope;
import net.sourceforge.docfetcher.model.Scope;
import net.sourceforge.docfetcher.model.ScopeRegistry;
import net.sourceforge.docfetcher.parse.Parser;
import net.sourceforge.docfetcher.parse.ParserRegistry;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.UtilFile;
import net.sourceforge.docfetcher.util.UtilGUI;
import net.sourceforge.docfetcher.util.UtilList;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * A tab representing a result page.
 * 
 * @author Tran Nam Quang
 */
public class ResultPanel extends Composite {
	
	public final Event<ResultPanel> evtVisibleItemsChanged = new Event<ResultPanel> ();
	public final Event<ResultPanel> evtSelectionChanged = new Event<ResultPanel> ();
	
	/** The TableViewer internally used. */
	private TableViewer viewer;
	
	/** All results; flat and unfiltered. */
	private ResultDocument[] results;
	
	/** The total number of visible result items. */
	private int visibleResultCount = 0;
	
	/**
	 * An array of arrays of ResultDocuments. Each top level array represents a
	 * "page" on the whole ResultPanel between which the user can switch.
	 */
	private ResultDocument[][] resultPages = new ResultDocument[0][];
	
	/**
	 * The zero-based index of the currently visible result page, or -1 if there
	 * aren't any pages.
	 */
	private int pageIndex = -1;
	
	/**
	 * A list of filters used to filter the raw input before display on
	 * the result pages.
	 */
	private List<ResultFilter> filters = new ArrayList<ResultFilter> (5);
	
	/**
	 * The Comparator used to sort the results.
	 */
	private ResultSorter resultSorter = new ResultSorter(ResultProperty.SCORE, false);

	public ResultPanel(Composite parent) {
		super(parent, SWT.NONE);
		setLayout(new FillLayout());
		viewer = new TableViewer(this, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		viewer.addDoubleClickListener(new SelectionLauncher());
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		
		Pref.Int.MaxResultsPerPage.evtChanged.add(new Event.Listener<Integer> () {
			public void update(Integer eventData) {
				refresh();
			}
		});
		
		// Create column event listener
		class ColumnListener extends SelectionAdapter implements ControlListener {
			// Resort entries when user clicks on a column header,
			// with inverted sorting order when the same header is clicked again
			public void widgetSelected(SelectionEvent e) {
				ResultProperty property = (ResultProperty) e.widget.getData();
				sortItems(property);
			}
			// Store column order in preferences
			public void controlMoved(ControlEvent e) {
				Pref.IntArray.ResultColumnOrder.setValue(viewer.getTable().getColumnOrder());			}
			// Store column widths in preferences
			public void controlResized(ControlEvent e) {
				TableColumn[] columns = viewer.getTable().getColumns();
				if (Pref.IntArray.ResultColumnWidths.getValue().length != columns.length)
					Pref.IntArray.ResultColumnWidths.setValue(new int[columns.length]);				for (int i = 0; i < columns.length; i++)
					Pref.IntArray.ResultColumnWidths.getValue()[i] = columns[i].getWidth();
			}
		}
		ColumnListener columnListener = new ColumnListener();
		
		// Create columns
		ResultProperty[] properties = ResultProperty.values();
		for (ResultProperty property : properties) {
			TableColumn column = new TableColumn(table, SWT.LEFT);
			column.setData(property);
			column.setText(property.getLabel());
			column.setMoveable(true);
			column.addSelectionListener(columnListener);
			column.addControlListener(columnListener);
			
			/*
			 * FIXME Possibly a bug on Windows Vista (SWT 3.2.2): This setting
			 * causes the column headers to display an (inappropriate) icon next
			 * to the column title. No problems on other platforms found.
			 */
			column.setAlignment(property.getAlignment());
		}
		
		// Load alternative column order from preferences if there is one
		int[] resultColumnOrder = Pref.IntArray.ResultColumnOrder.getValue();
		if (properties.length == resultColumnOrder.length)
			viewer.getTable().setColumnOrder(resultColumnOrder);
		
		// Set column widths
		int[] resultColumnWidths = Pref.IntArray.ResultColumnWidths.getValue();
		TableColumn[] columns = viewer.getTable().getColumns();
		if (properties.length == resultColumnWidths.length) {
			for (int i = 0; i < columns.length; i++) {
				columns[i].removeControlListener(columnListener);
				columns[i].setWidth(resultColumnWidths[i]);
				columns[i].addControlListener(columnListener);
			}
		}
		else {
			for (int i = 0; i < columns.length; i++)
				columns[i].setWidth(properties[i].getDefaultColumnWidth());
		}
		
		// Set up filters
		filters.add(new ParserFilter());
		filters.add(new ScopeFilter());
		
		/*
		 * This allows making copies of the files represented by the table items
		 * using drag and drop.
		 * 
		 * TODO Creating a link to the original file using DnD would be nice,
		 * but doesn't work. Since it doesn't work in Eclipse either, there's
		 * probably not much one can do about it.
		 */
		DragSource source = new DragSource(table, DND.DROP_COPY | DND.DROP_LINK);
		source.setTransfer(new Transfer[] {FileTransfer.getInstance()});
		source.addDragListener(new DragSourceAdapter() {
			public void dragSetData(DragSourceEvent event) {
				IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
				String[] paths = new String[sel.size()];
				Iterator<?> it = sel.iterator();
				for (int i = 0; it.hasNext(); i++)
					paths[i] = ((ResultDocument) it.next()).getFile().getAbsolutePath();
				event.data = paths;
			}
		});
		
		// Create context menu
		Menu menu = new Menu(parent.getShell(), SWT.POP_UP);
		table.setMenu(menu);
		
		final MenuItem openItem = new MenuItem(menu, SWT.PUSH);
		openItem.setText(Msg.open.value());
		openItem.setEnabled(false);
		menu.setDefaultItem(openItem);
		openItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				launchSelection();
			}
		});
		
		final MenuItem openParentItem = new MenuItem(menu, SWT.PUSH);
		openParentItem.setEnabled(false);
		openParentItem.setText(Msg.open_parent.value());
		new MenuItem(menu, SWT.SEPARATOR);
		openParentItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				launchSelectionParents();
			}
		});
		
		final MenuItem copyItem = new MenuItem(menu, SWT.PUSH);
		copyItem.setText(Msg.copy.value() + "\t" + Key.Copy.toString()); //$NON-NLS-1$
		copyItem.setEnabled(false);
		copyItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				copySelectionToClipboard();
			}
		});

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent arg0) {
				// Disable context menu items if selection is empty
				boolean hasSelection = ! viewer.getSelection().isEmpty();
				openItem.setEnabled(hasSelection);
				openParentItem.setEnabled(hasSelection);
				copyItem.setEnabled(hasSelection);
				
				// Update result panel and status line
				evtSelectionChanged.fireUpdate(ResultPanel.this);
			}
		});
		
		if (Pref.Bool.AllowRepositoryModification.getValue()) {
			final MenuItem deleteItem = new MenuItem(menu, SWT.PUSH);
			deleteItem.setText(Msg.delete_file.value() + "\t" + Key.Delete.toString()); //$NON-NLS-1$
			deleteItem.setEnabled(false);
			deleteItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					deleteSelection();
				}
			});
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent arg0) {
					deleteItem.setEnabled(! viewer.getSelection().isEmpty());
					evtSelectionChanged.fireUpdate(ResultPanel.this);
				}
			});
		}
		
		viewer.getTable().addKeyListener(new ResultPanelNavigator());
		viewer.getTable().addKeyListener(new SortSelector());
	}
	
	/**
	 * Displays the given results on this ResultPanel, possibly filtering out
	 * some items. Displays a blank page when the given result is null or an
	 * empty array.
	 */
	public void setResults(ResultDocument... results) {
		if (results == null || results.length == 0) {
			pageIndex = -1;
			visibleResultCount = 0;
			this.results = new ResultDocument[0];
			resultPages = new ResultDocument[0][];
			viewer.setInput(new ResultDocument[0]);
			evtVisibleItemsChanged.fireUpdate(this);
			return;
		}
		this.results = results;
		setResultPages(results);
		pageIndex = 0;
		viewer.setInput(resultPages[0]);
		viewer.getTable().setTopIndex(0);
		evtVisibleItemsChanged.fireUpdate(this);
	}
	
	/**
	 * Updates the contents of this ResultPanel.
	 * <p>
	 * Note: Always use this method instead of calling refresh() or update() on
	 * the internal Viewer, because the latter won't make use of filters and
	 * sorters!
	 */
	public void refresh() {
		setResults(results);
	}
	
	/**
	 * Adds a filter to be used to filter the results.
	 */
	public void addFilter(ResultFilter filter) {
		filters.add(filter);
	}
	
	/**
	 * Removes the given filter from the set of filters used.
	 */
	public void removeFilter(ResultFilter filter) {
		filters.remove(filter);
	}
	
	/**
	 * Sets the local <tt>resultPages</tt> field for the given raw input of
	 * <tt>ResultDocument</tt>s, with filtering and sorting applied. The
	 * <tt>visibleResultCount</tt> field is also updated. The argument must
	 * not be null.
	 */
	private void setResultPages(ResultDocument[] input) {
		// Apply filters
		List<ResultDocument> filteredInput = Arrays.asList(input);
		for (ResultFilter filter : filters)
			filteredInput = filter.select(filteredInput);
		visibleResultCount = filteredInput.size();
		if (visibleResultCount == 0) {
			resultPages = new ResultDocument[1][0];
			return;
		}
		
		// Sort
		Object[] sortedInput = filteredInput.toArray();
		Arrays.sort(sortedInput, resultSorter);
		
		// Split flat array into array of arrays
		int maxSize = Pref.Int.MaxResultsPerPage.getValue();
		if (maxSize < 1)
			throw new IllegalStateException("Maximum number of results per page cannot be smaller than 1."); //$NON-NLS-1$
		int nPages = (int) Math.ceil((double) sortedInput.length / (double) maxSize);
		resultPages = new ResultDocument[nPages][];
		for (int i = 0; i < resultPages.length; i++) {
			int length = maxSize;
			if (i == resultPages.length - 1 && sortedInput.length % maxSize != 0)
				length = sortedInput.length % maxSize;
			resultPages[i] = new ResultDocument[length];
			System.arraycopy(sortedInput, i * maxSize, resultPages[i], 0, length);
		}
	}
	
	/**
	 * Navigates to the previous page of this ResultPanel. Does nothing if the
	 * first page is reached.
	 */
	public void previousPage() {
		if (pageIndex == -1) return;
		pageIndex = Math.max(0, pageIndex - 1);
		viewer.setInput(resultPages[pageIndex]);
		viewer.getTable().setTopIndex(0);
		evtVisibleItemsChanged.fireUpdate(this);
	}
	
	/**
	 * Navigates to the next page of this ResultPanel. Does nothing if the last
	 * page is reached.
	 */
	public void nextPage() {
		if (pageIndex == -1) return;
		int maxIndex = resultPages.length - 1;
		pageIndex = Math.min(pageIndex + 1, maxIndex);
		viewer.setInput(resultPages[pageIndex]);
		viewer.getTable().setTopIndex(0);
		evtVisibleItemsChanged.fireUpdate(this);
	}
	
	public void setPage(int newPageIndex) {
		if (pageIndex == -1) return;
		pageIndex = Math.min(Math.max(0, newPageIndex), resultPages.length - 1);
		viewer.setInput(resultPages[pageIndex]);
		viewer.getTable().setTopIndex(0);
		evtVisibleItemsChanged.fireUpdate(this);
	}
	
	/**
	 * Returns the zero-based index of the currently visible page of this
	 * ResultPanel. Returns -1 if there aren't any pages.
	 */
	public int getPageIndex() {
		return pageIndex;
	}
	
	/**
	 * Returns the number of pages of this ResultPanel.
	 */
	public int getPageCount() {
		return resultPages.length;
	}
	
	/**
	 * Launches all the files selected on this ResultPanel, unless the number of
	 * selected files exceeds a certain limit. In that case an appropriate
	 * warning message is shown to the user.
	 */
	private void launchSelection() {
		IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
		if (sel.isEmpty()) return;
		int openLimit = Pref.Int.OpenLimit.getValue();
		if (sel.size() > openLimit) {
			UtilGUI.showInfoMsg(Msg.open_limit.format(openLimit));
			return;
		}
		Iterator<?> it = sel.iterator();
		while(it.hasNext()) {
			ResultDocument doc = (ResultDocument) it.next();
			UtilFile.launch(doc.getFile().getAbsolutePath());
		}
		if (Pref.Bool.HideOnOpen.getValue())
			DocFetcher.getInstance().toSystemTray();
	}
	
	/**
	 * Opens the parent directories of all the files selected on this ResultPanel,
	 * unless their number exceeds a certain limit. In that case an appropriate
	 * warning message is shown to the user.
	 */
	private void launchSelectionParents() {
		IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
		if (sel.isEmpty()) return;
		Set<String> dirsToOpen = new TreeSet<String> (); // Use a set cache to avoid duplicate launch calls
		Iterator<?> it = sel.iterator();
		while(it.hasNext()) {
			ResultDocument doc = (ResultDocument) it.next();
			dirsToOpen.add(UtilFile.getParentFile(doc.getFile()).getAbsolutePath());
		}
		int openLimit = Pref.Int.OpenLimit.getValue();
		if (dirsToOpen.size() > openLimit) {
			UtilGUI.showInfoMsg(Msg.open_limit.format(openLimit));
			return;
		}
		for (String dir : dirsToOpen)
			UtilFile.launch(dir);
		if (Pref.Bool.HideOnOpen.getValue())
			DocFetcher.getInstance().toSystemTray();
	}
	
	/**
	 * Copies the files selected on this ResultPanel to the given Clipboard, both
	 * as files and file paths. HTML pairing is taken into account.
	 */
	private void copySelectionToClipboard() {
		IStructuredSelection sel = getSelection();
		if (sel.isEmpty()) return;
		File[] files = new File[sel.size()];
		Iterator<?> it = sel.iterator();
		for (int i = 0; it.hasNext(); i++)
			files[i] = ((ResultDocument) it.next()).getFile();
		files = UtilFile.completeHTMLPairs(files);
		String[] filePaths = UtilFile.toPaths(files);
		Transfer[] types = new Transfer[] {
				FileTransfer.getInstance(),
				TextTransfer.getInstance()
		};
		DocFetcher.getInstance().getClipboard().setContents(
				new Object[] {
						filePaths,
						UtilList.toString(Const.LS, filePaths)
				},
				types
		);
	}
	
	private void deleteSelection() {
		// Locate files to be deleted and RootScopes to be updated
		IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
		if (sel.isEmpty()) return;
		Iterator<?> it = sel.iterator();
		RootScope[] rootScopes = ScopeRegistry.getInstance().getEntries();
		int deleteCount = 0; // Counts documents, not files
		Set<File> filesToDelete = new HashSet<File> ();
		Set<RootScope> scopesToUpdate = new HashSet<RootScope> ();
		while (it.hasNext()) {
			File file = ((ResultDocument) it.next()).getFile();
			for (RootScope rootScope : rootScopes) {
				FileWrapper wrapper = rootScope.getFileWrapperDeep(file);
				if (wrapper == null) continue;
				filesToDelete.add(wrapper.getFile());
				if (wrapper instanceof HTMLPair) {
					File htmlDir = ((HTMLPair) wrapper).getHtmlFolder();
					if (htmlDir != null) filesToDelete.add(htmlDir);
				}
				scopesToUpdate.add(rootScope);
				deleteCount++;
				break; // Assume only one of the RootScopes contains the file
			}
		}
		
		// Ask user whether to proceed
		if (deleteCount == 0) return;
		int ans = UtilGUI.showConfirmMsg(Msg.confirm_delete_file.format(deleteCount));
		if (ans != SWT.OK) return;
		
		Set<File> emptyParents = new HashSet<File> ();
		DocFetcher.getInstance().setWatchEnabled(false, scopesToUpdate);
		
		// Delete files
		for (File file : filesToDelete) {
			File parent = UtilFile.getParentFile(file);
			UtilFile.delete(file, true);
			File[] neighbors = UtilFile.listAll(parent);
			if (neighbors.length == 0)
				emptyParents.add(parent);
		} 
		
		DocFetcher.getInstance().setWatchEnabled(Pref.Bool.WatchFS.getValue(), scopesToUpdate);
		
		// Update indexes, but silently
		IndexingDialog indexingDialog = DocFetcher.getInstance().getIndexingDialog();
		for (RootScope scope : scopesToUpdate)
			indexingDialog.addJob(new Job(scope, false, false));
		
		// Tell user about empty folders
		if (emptyParents.size() > 0) {
			final Shell msgBox = new Shell(getShell(), SWT.SHELL_TRIM | SWT.PRIMARY_MODAL);
			msgBox.setSize(400, 300);
			UtilGUI.centerShell(getShell(), msgBox);
			msgBox.setText(Msg.empty_folders.value());
			FormLayout formLayout = new FormLayout();
			formLayout.marginWidth = formLayout.marginHeight = 5;
			msgBox.setLayout(formLayout);
			msgBox.setImage(Icon.INFO.getImage());
			final org.eclipse.swt.widgets.List list = new org.eclipse.swt.widgets.List(msgBox,
					SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			for (File emptyParent : emptyParents)
				list.add(emptyParent.getAbsolutePath());
			Label msg = new Label(msgBox, SWT.NONE);
			msg.setText(Msg.empty_folders_msg.value());
			Button okBt = new Button(msgBox, SWT.PUSH);
			okBt.setText(Msg.ok.value());
			okBt.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					msgBox.close();
				}
			});
			list.addMouseListener(new MouseAdapter() {
				public void mouseDoubleClick(MouseEvent e) {
					String[] selItems = list.getSelection();
					if (selItems.length == 0) return;
					UtilFile.launch(selItems[0]);
				}
			});
			FormDataFactory fdf = FormDataFactory.getInstance();
			fdf.bottom().right().minWidth(Const.MIN_BT_WIDTH).applyTo(okBt);
			fdf.reset().top().left().right().applyTo(msg);
			fdf.bottom(okBt).top(msg).applyTo(list);
			msgBox.open();
		}
		
		// Create new result list that doesn't contain the deleted elements
		List<ResultDocument> newResults = new ArrayList<ResultDocument> ();
		for (ResultDocument resultDoc : results)
			if (! UtilList.containsEquality(filesToDelete, resultDoc.getFile()))
				newResults.add(resultDoc);
		int previousPageIndex = pageIndex;
		setResults(newResults.toArray(new ResultDocument[newResults.size()]));
		setPage(previousPageIndex);
	}
	
	/**
	 * Returns the set of ResultDocuments that are selected on this ResultPanel.
	 * (The set items can be cast to ResultDocument.)
	 */
	public IStructuredSelection getSelection() {
		return (IStructuredSelection) viewer.getSelection();
	}
	
	/**
	 * Returns the total number of result items visible on all result pages
	 * (including those pages that aren't currently visible).
	 */
	public int getVisibleResultCount() {
		return visibleResultCount;
	}
	
	/**
	 * Sorts the results by the given property. If the given property equals the
	 * previous property, then the sorting order will be inverted.
	 */
	public void sortItems(ResultProperty property) {
		if (resultSorter.getProperty().equals(property))
			resultSorter.setInverted(! resultSorter.isInverted());
		else
			resultSorter.setInverted(false);
		resultSorter.setProperty(property);
		if (results == null)
			return;
		setResultPages(results);
		pageIndex = 0;
		viewer.setInput(resultPages[0]);
	}
	
	static class ContentProvider extends TableContentProviderAdapter {
		
		public Object[] getElements(Object inputElement) {
			return (ResultDocument[]) inputElement;
		}
		
	}
	
	static class LabelProvider extends TableLabelProviderAdapter {
		
		private List<Image> disposables = new ArrayList<Image> ();
		
		public String getColumnText(Object element, int columnIndex) {
			ResultDocument result = (ResultDocument) element;
			File file = result.getFile();
			switch (columnIndex) {
			case 0: return result.getTitle(); // Title
			case 1: return String.valueOf(Math.round(result.getScore() * 100)); // Score
			case 2: return UtilFile.getSizeInKB(file) + " KB"; // Filesize //$NON-NLS-1$
			case 3: return file.getName(); // Filename
			case 4: return UtilFile.getExtension(file); // Type
			case 5: return file.getAbsolutePath(); // Path
			case 6: return result.getAuthor(); // Author
			case 7: return UtilFile.getLastModified(file); // Last Modified
			}
			return null;
		}
		
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0) {
				try {
					ResultDocument result = (ResultDocument) element;
					String extension = UtilFile.getExtension(result.getFile());

					/*
					 * FIXME On Ubuntu 8.10 the SVG icon returned from the OS would
					 * be too big, causing the result panel rows to expand beyond
					 * their normal height.
					 */
					if (Const.IS_LINUX && extension.equals("svg")) //$NON-NLS-1$
						return Icon.FILE.getImage();

					Program program = Program.findProgram(extension);
					if (program == null)
						return Icon.FILE.getImage(); // Program icon not available, return generic file icon
					ImageData imgData = program.getImageData();
					if (imgData == null)
						return Icon.FILE.getImage(); // Program icon not available, return generic file icon
					Image image = new Image(Display.getCurrent(), imgData);
					disposables.add(image);
					return image;
				} catch (Exception e) {
					/**
					 * TODO This can happen when we've reached the limit for the
					 * number of image handles. See bug #2916975 and bug
					 * #2829490. Returning null here is only a temporary
					 * solution.
					 */
					return null;
				}
			}
			return null;
		}
		
		public void dispose() {
			for (Image image : disposables) {
				if (image != null && ! image.isDisposed())
					image.dispose();
			}
		}
		
	}
	
	/**
	 * A filter used to filter results in ResultPanels.
	 * 
	 * @author Tran Nam Quang
	 */
	public static interface ResultFilter {
		
		/**
		 * Takes the given collection of ResultDocuments and returns a new,
		 * filtered list of ResultDocuments.
		 */
		public List<ResultDocument> select(Collection<ResultDocument> docs);
	}
	
	/**
	 * Filters out all files whose file types aren't checked in the parser group
	 * viewer.
	 */
	static class ParserFilter implements ResultFilter {
		
		public List<ResultDocument> select(Collection<ResultDocument> docs) {
			Map<String, Boolean> checkStates = new HashMap<String, Boolean> ();
			for (Parser parser : ParserRegistry.getParsers())
				checkStates.put(parser.getClass().getSimpleName(), parser.isChecked());
			
			List<ResultDocument> selected = new ArrayList<ResultDocument> (docs.size());
			for (ResultDocument doc : docs) {
				Boolean checkState = checkStates.get(doc.getParserName());
				if (checkState != null && checkState)
					selected.add(doc);
			}
			
			return selected;
		}
		
	}
	
	/**
	 * Filters out a file if its parent directory isn't checked in the
	 * search scope viewer.
	 */
	static class ScopeFilter implements ResultFilter {
		
		public List<ResultDocument> select(Collection<ResultDocument> docs) {
			/*
			 * Bug #2807587: In v1.0 and earlier DocFetcher walked down the
			 * Scope hierarchy for each ResultDocument to determine whether it
			 * should be included in the visible results, causing DocFetcher to
			 * freeze for minutes if there were too many indexes and/or result
			 * items. Using this HashMap, the processing time has been reduced
			 * to milliseconds.
			 */
			Map<String, Boolean> checkStates = new HashMap<String, Boolean> ();
			for (RootScope rootScope : ScopeRegistry.getInstance().getEntries()) {
				insertCheckState(checkStates, rootScope);
			}
			
			List<ResultDocument> selected = new ArrayList<ResultDocument> (docs.size());
			for (ResultDocument doc : docs) {
				String path = UtilFile.getParentFile(doc.getFile()).getAbsolutePath();
				Boolean checkState = checkStates.get(path);
				if (checkState != null && checkState)
					selected.add(doc);
			}
			
			return selected;
		}
		
		private void insertCheckState(Map<String, Boolean> checkStates, Scope scope) {
			checkStates.put(scope.getFile().getAbsolutePath(), scope.isChecked());
			for (Scope child : scope.getChildren())
				insertCheckState(checkStates, child);
		}
		
	}
	
	/**
	 * Sorts the results.
	 */
	static class ResultSorter implements Comparator<Object> {
		
		/**
		 * The currently used sort criterion.
		 */
		private ResultProperty property;
		
		/**
		 * Whether the sorting is inverted (1: normal; -1: inverted).
		 */
		private int inverted = 1;
		
		/**
		 * Constructs a new instance of this sorter, with <tt>property</tt> as
		 * the sort criterion. The <tt>inverted</tt> parameter determines
		 * whether the sorting should be inverted.
		 */
		ResultSorter(ResultProperty property, boolean inverted) {
			this.property = property;
			this.inverted = inverted ? -1 : 1;
		}
		
		public int compare(Object o1, Object o2) {
			ResultDocument r1 = (ResultDocument) o1;
			ResultDocument r2 = (ResultDocument) o2;
			File f1 = r1.getFile();
			File f2 = r2.getFile();
			switch (property) {
			case TITLE:
				return UtilList.compareToIgnoreCaseWithNumbers(r1.getTitle(), r2.getTitle()) * inverted;
			case NAME:
				return UtilList.compareToIgnoreCaseWithNumbers(f1.getName(), f2.getName()) * inverted;
			case SIZE:
				long length1 = f1.length();
				long length2 = f2.length();
				if (length1 == length2) return 0;
				return ((length1 > length2) ? -1 : 1) * inverted; // Descending order
			case TYPE:
				String type1 = UtilFile.getExtension(f1);
				String type2 = UtilFile.getExtension(f2);
				return type1.compareToIgnoreCase(type2) * inverted;
			case PATH:
				String path1 = f1.getAbsolutePath();
				String path2 = f2.getAbsolutePath();
				return UtilList.compareToIgnoreCaseWithNumbers(path1, path2) * inverted;
			case AUTHOR:
				String author1 = r1.getAuthor();
				String author2 = r2.getAuthor();
				if (author1 == null) author1 = "";
				if (author2 == null) author2 = "";
				return UtilList.compareToIgnoreCaseWithNumbers(author1, author2) * inverted;
			case LAST_MODIFIED:
				long lmod1 = f1.lastModified();
				long lmod2 = f2.lastModified();
				if (lmod1 == lmod2) return 0;
				return ((lmod1 > lmod2) ? 1 : -1) * inverted;
			default: // sort by score
				return r1.compareTo(r2) * inverted; // Descending order
			}
		}
		
		/**
		 * Returns the currently used sort criterion.
		 */
		public ResultProperty getProperty() {
			return property;
		}
		
		/**
		 * Sets the currently used sort criterion.
		 */
		public void setProperty(ResultProperty property) {
			this.property = property;
		}
		
		/**
		 * Returns whether the sorting is inverted.
		 */
		public boolean isInverted() {
			return inverted < 0;
		}
		
		/**
		 * Sets whether the sorting is inverted.
		 */
		public void setInverted(boolean inverted) {
			this.inverted = inverted ? -1 : 1;
		}
		
	}
	
	/**
	 * Runs the program associated with the file the user double-clicks on.
	 */
	static class SelectionLauncher implements IDoubleClickListener  {
		
		public void doubleClick(DoubleClickEvent event) {
			IStructuredSelection sel = (IStructuredSelection) event.getSelection();
			ResultDocument result = (ResultDocument) sel.getFirstElement();
			File file = result.getFile();
			boolean launched = UtilFile.launch(file.getAbsolutePath());
			if (launched) {
				if (Pref.Bool.HideOnOpen.getValue())
					DocFetcher.getInstance().toSystemTray();
			}
			else {
				DocFetcher.getInstance().setStatus(Msg.open_file_error.format(file.getName()));
			}
		}
		
	}

	/**
	 * Properties of the result items. These will be displayed in the columns of
	 * the result page.
	 */
	public static enum ResultProperty {
		
		/*
		 * FIXME This is bad design because everytime a new result property
		 * (i.e. a new column in the result panel) is added to this list, one
		 * must remember to add another case to the switch-case construct in the
		 * ResultSorter.compare(..) method. See bug #2956392.
		 */
		TITLE (Msg.property_title.value(), 250, SWT.LEFT),
		SCORE (Msg.property_score.value(), 75, SWT.RIGHT),
		SIZE (Msg.property_size.value(), 75, SWT.RIGHT),
		NAME (Msg.property_name.value(), 200, SWT.LEFT),
		TYPE (Msg.property_type.value(), 75, SWT.LEFT),
		PATH (Msg.property_path.value(), 350, SWT.LEFT),
		AUTHOR (Msg.property_author.value(), 100, SWT.LEFT),
		LAST_MODIFIED (Msg.property_lastModified.value(), 100, SWT.LEFT);
		
		private String label;
		private int defaultColumnWidth;
		private int alignment;
		
		ResultProperty(String label, int columnWidth, int alignment) {
			this.label = label;
			this.defaultColumnWidth = columnWidth;
			this.alignment = alignment;
		}

		public int getAlignment() {
			return alignment;
		}

		public int getDefaultColumnWidth() {
			return defaultColumnWidth;
		}

		public String getLabel() {
			return label;
		}
		
		public static ResultProperty get(int index) {
			return ResultProperty.values()[index];
		}
		
	}
	
	/**
	 * Handler for result panel related keys.
	 */
	class ResultPanelNavigator extends KeyAdapter {
		
		public void keyReleased(KeyEvent e) {
			Key key = Key.getKey(e.stateMask, e.keyCode);
			if (key == null) return;
			
			Table table = (Table) e.widget;
			int rowIndex = table.getSelectionIndex();
			
			switch (key) {
			case Up:
				rowIndex = Math.max(0, rowIndex - 1);
				// This won't cause a selection event, therefore we have to call the event handler manually
				table.setSelection(rowIndex);
				evtSelectionChanged.fireUpdate(ResultPanel.this);
				break;
			case Down:
				rowIndex = Math.min(table.getItemCount() - 1, rowIndex + 1);
				// This won't cause a selection event, therefore we have to call the event handler manually
				table.setSelection(rowIndex);
				evtSelectionChanged.fireUpdate(ResultPanel.this);
				break;
			case Arrow_Left:
				e.doit = false; // fall through
			case Left:
				previousPage();
				break;
			case Arrow_Right:
				e.doit = false; // fall through
			case Right:
				nextPage();
				break;
			case Copy:
				copySelectionToClipboard();
				break;
			case Delete:
				deleteSelection();
				break;
			}
		}		
		
	}
	
	/**
	 * This class handles sorting of results by a specific column
	 * using keyboard shortcuts.
	 */
	class SortSelector extends KeyAdapter {
		
		public void keyReleased(KeyEvent e) {
			if (e.stateMask != SWT.ALT) return;
			int key = 0;
			try {
				key = Integer.valueOf(String.valueOf((char) e.keyCode));
			} catch (NumberFormatException e1) {
				return;
			}
			int columnCount = viewer.getTable().getColumnCount();
			if (key == 0) key = 10;
			if (key > columnCount || key < 1 || key > 10) return;
			sortItems(ResultProperty.get(key - 1));
		}
		
	}
	
}
