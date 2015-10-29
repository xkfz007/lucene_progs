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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.model.Job;
import net.sourceforge.docfetcher.model.RootScope;
import net.sourceforge.docfetcher.parse.ParseException;
import net.sourceforge.docfetcher.util.UtilFile;
import net.sourceforge.docfetcher.util.UtilGUI;
import net.sourceforge.docfetcher.util.UtilList;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * One of the tabs on the indexing box.
 * 
 * @author Tran Nam Quang
 */
public class IndexingTab extends Composite {
	
	private Job job;
	private Text textExtBox;
	private Text htmlExtBox;
	private Text exclusionBox;
	private Button checkHTMLPairing;
	private Button checkDeleteOnExit;
	ProgressPanel progressPanel;
	private List<ParseException> errorCache = new ArrayList<ParseException> ();
	private Set<String> extensionsFromDisk; // Caching file extensions so we need to load them only once
	
	public IndexingTab(Composite parent, Job job) {
		super(parent, SWT.NONE);
		this.job = job;
		
		/*
		 * Create progress panel in advance, in case an external caller wants to
		 * put messages into it.
		 */
		progressPanel = new ProgressPanel(this);
		progressPanel.setVisible(false);
		
		if (job.isReadyForIndexing())
			createProgressPage();
		else
			createEditPage();
		
	}
	
	/**
	 * Creates the page on which the user can set indexing settings, among other
	 * things.
	 */
	private void createEditPage() {
		RootScope scope = job.getScope();
		
		FormLayout formLayout = new FormLayout();
		formLayout.marginWidth = formLayout.marginHeight = 5;
		setLayout(formLayout);
		Composite textBoxComp = new Composite(this, SWT.NONE);
		textBoxComp.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
		
		// Create text box controls
		new Label(textBoxComp, SWT.NONE).setText(Msg.target_folder.value());
		final Text pathBox = new Text(textBoxComp, SWT.BORDER | SWT.READ_ONLY);
		pathBox.setText(scope.getFile().getAbsolutePath());
		new Label(textBoxComp, SWT.NONE).setText(Msg.ipref_text_ext.value());
		textExtBox = createExtensionBox(textBoxComp, true);
		new Label(textBoxComp, SWT.NONE).setText(Msg.ipref_html_ext.value());
		htmlExtBox = createExtensionBox(textBoxComp, false);
		new Label(textBoxComp, SWT.NONE).setText(Msg.ipref_skip_regex.value());
		exclusionBox = new Text(textBoxComp, SWT.BORDER);
		
		// Text box control layout
		GridData fillGD = new GridData(SWT.FILL, SWT.FILL, true, false);
		pathBox.setLayoutData(GridDataFactory.copyData(fillGD));
		exclusionBox.setLayoutData(fillGD);
		
		// Check buttons
		checkHTMLPairing = new Button(this, SWT.CHECK);
		checkHTMLPairing.setText(Msg.ipref_detect_html_pairs.value());
		checkDeleteOnExit = new Button(this, SWT.CHECK);
		checkDeleteOnExit.setText(Msg.ipref_delete_on_exit.value());
		
		Label spacing = new Label(this, SWT.NONE);
		Label separator = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
		
		// Create 'help', 'reset' and 'run' buttons
		Button helpButton = new Button(this, SWT.PUSH);
		Button defaultButton = new Button(this, SWT.PUSH);
		Button runButton = new Button(this, SWT.PUSH);
		helpButton.setText(Msg.help.value());
		defaultButton.setText(Msg.restore_defaults.value());
		runButton.setText(Msg.run.value());
		
		// Create regex checker
		final Label regexMatchLabel = new Label(this, SWT.NONE);
		regexMatchLabel.setText(Msg.regex_matches_file_no.value());
		final Text regexFile = new Text(this, SWT.SINGLE | SWT.BORDER);
		Button regexFileChooser = new Button(this, SWT.PUSH);
		regexFileChooser.setText("..."); //$NON-NLS-1$

		// Layout for the upper controls
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.top().left().right().applyTo(textBoxComp);
		fdf.top(textBoxComp, 10).applyTo(checkHTMLPairing);
		fdf.top(checkHTMLPairing).applyTo(checkDeleteOnExit);
		fdf.top(checkDeleteOnExit).applyTo(spacing);
		fdf.top(spacing).applyTo(separator);
		fdf.reset().minWidth(Const.MIN_BT_WIDTH).top(separator).left().applyTo(helpButton);
		fdf.reset().minWidth(Const.MIN_BT_WIDTH).top(separator).left(helpButton).applyTo(defaultButton);
		fdf.reset().minWidth(Const.MIN_BT_WIDTH).top(separator).right().applyTo(runButton);
		
		// Layout for the lower controls
		fdf.reset().height(pathBox.computeSize(SWT.DEFAULT, SWT.DEFAULT).y).bottom().right().applyTo(regexFileChooser);
		fdf.reset().left().right(regexFileChooser).bottom().applyTo(regexFile);
		fdf.bottom(regexFile).right().applyTo(regexMatchLabel);
		
		/*
		 * Set default values. This must be done after the layout work,
		 * otherwise the widths of the text widgets might become too big.
		 */
		new Thread() {
			public void run() {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						resetJobConfig();
					}
				});
			}
		}.start();
		
		// Launch help file when help button is pressed
		helpButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				UtilFile.launch(Const.HELP_FILE_INDEXING);
			}
		});
		
		// Reset configuration when the reset button is clicked on
		defaultButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				resetJobConfig();
			}
		});
		
		/*
		 * Check if the skip file regex pattern matches the selected filename
		 * whenever the regex pattern or the filename are modified
		 */
		ModifyListener regexChecker = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String[] exclFilters = UtilList.parseExclusionString(exclusionBox.getText());
				for (String filter : exclFilters) {
					try {
						Pattern.compile(filter);
					} catch (PatternSyntaxException ex) {
						regexMatchLabel.setText(Msg.regex_matches_file_no.value());
						return;
					}
				}
				for (String filter : exclFilters) {
					if (regexFile.getText().matches(filter)) {
						regexMatchLabel.setText(Msg.regex_matches_file_yes.value());
						return;
					}	
				}
				regexMatchLabel.setText(Msg.regex_matches_file_no.value());
			}
		};
		exclusionBox.addModifyListener(regexChecker);
		regexFile.addModifyListener(regexChecker);
		
		// Open file dialog when the button on the regex check panel is pressed
		regexFileChooser.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell(), SWT.PRIMARY_MODAL);
				dialog.setFilterPath(pathBox.getText());
				dialog.setText(Msg.choose_regex_testfile_title.value());
				String filepath = dialog.open();
				if (filepath == null) return;
				regexFile.setText(new File(filepath).getName());
			}
		});
		
		/*
		 * Run indexing job when the run button is pressed or when the user hits
		 * the enter key on one of the text widgets.
		 */
		class RunListener extends KeyAdapter implements SelectionListener {
			public void widgetSelected(SelectionEvent e) {
				runJob();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void keyReleased(KeyEvent e) {
				if (UtilGUI.isCRKey(e))
					runJob();
			}
		}
		RunListener runListener = new RunListener();
		runButton.addSelectionListener(runListener);
		textExtBox.addKeyListener(runListener);
		htmlExtBox.addKeyListener(runListener);
		exclusionBox.addKeyListener(runListener);
	}
	
	private Text createExtensionBox(Composite parent, final boolean useTextExtensions) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new FormLayout());
		
		// Widgets
		Text text = new Text(comp, SWT.BORDER);
		Button bt = new Button(comp, SWT.PUSH);
		bt.setText("..."); //$NON-NLS-1$
		
		// Button Listener
		bt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// We won't use the values from the text widgets because they might be invalid
				String[] checkedExts = useTextExtensions ?
						job.getScope().getTextExtensions() :
							job.getScope().getHtmlExtensions();
				
				FileExtensionChooser chooser = new FileExtensionChooser(getShell());
				Set<String> out = chooser.open(job.getScope().getFile(), checkedExts, extensionsFromDisk);
				extensionsFromDisk = chooser.getExtensionsFromDisk();
				
				if (useTextExtensions)
					textExtBox.setText(UtilList.toString(" ", out)); //$NON-NLS-1$
				else
					htmlExtBox.setText(UtilList.toString(" ", out)); //$NON-NLS-1$
			}
		});
		
		// Layout
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.setMargin(0).top().bottom().right().applyTo(bt);
		fdf.right(bt, -5).left().applyTo(text);
		
		// Without this height hint, the button would make the whole composite bigger in height
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData.heightHint = text.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		comp.setLayoutData(gridData);
		
		return text;
	}
	
	/**
	 * Tries to transfer the indexing settings in the GUI to the underlying job
	 * object. Returns true if successful. If not (due to invalid settings), it
	 * aborts with warning messages and returns false.
	 */
	private boolean saveJobConfig() {
		RootScope scope = job.getScope();
		
		// Check if target directory still exists
		if (! scope.getFile().exists()) {
			UtilGUI.showErrorMsg(Msg.target_folder_deleted.value());
			return false;
		}
		
		// Check if specified exclusions pattern is valid
		String[] exclFilters = UtilList.parseExclusionString(exclusionBox.getText());
		for (String filter : exclFilters) {
			try {
				Pattern.compile(filter);
			} catch (PatternSyntaxException e) {
				UtilGUI.showWarningMsg(Msg.not_a_regex.format(filter));
				return false;
			}
		}
		String[] textExts = textExtBox.getText().trim().split("[^\\p{Alnum}]+"); //$NON-NLS-1$
		String[] htmlExts = htmlExtBox.getText().trim().split("[^\\p{Alnum}]+"); //$NON-NLS-1$
		
		scope.setTextExtensions(textExts);
		scope.setHtmlExtensions(htmlExts);
		scope.setExclusionFilters(exclFilters);
		scope.setDetectHTMLPairs(checkHTMLPairing.getSelection());
		scope.setDeleteOnExit(checkDeleteOnExit.getSelection());
		return true;
	}
	
	/**
	 * Discards the indexing settings in the GUI and replaces them with the
	 * settings found in the underlying job object.
	 */
	private void resetJobConfig() {
		RootScope scope = job.getScope();
		textExtBox.setText(UtilList.toString(" ", scope.getTextExtensions())); //$NON-NLS-1$
		htmlExtBox.setText(UtilList.toString(" ", scope.getHtmlExtensions())); //$NON-NLS-1$
		exclusionBox.setText(UtilList.toString(" $ ", scope.getExclusionFilters())); //$NON-NLS-1$
		checkHTMLPairing.setSelection(scope.isDetectHTMLPairs());
		checkDeleteOnExit.setSelection(scope.isDeleteOnExit());
	}
	
	/**
	 * Runs the job using the indexing settings from the GUI.
	 */
	private void runJob() {
		if (! saveJobConfig())
			return;
		for (Control child : getChildren())
			if (child != progressPanel)
				child.dispose();
		createProgressPage();
		job.setReadyForIndexing(true);
	}
	
	/**
	 * Creates the page that displays the indexing progress and indexing errors.
	 */
	private void createProgressPage() {
		// Make progress panel visible
		progressPanel.setVisible(true);
		progressPanel.setLayoutData(null);
		setLayout(FillLayoutFactory.getInst().margin(5).create());
		layout();
		
		// Initial info messages
		RootScope scope = job.getScope();
		progressPanel.appendInfo(Msg.target_folder.value() + " " + scope.toString()); //$NON-NLS-1$
		progressPanel.appendInfo(Msg.ipref_text_ext.value() + " " + UtilList.toString(" ", scope.getTextExtensions())); //$NON-NLS-1$ //$NON-NLS-2$
		progressPanel.appendInfo(Msg.ipref_html_ext.value() + " " + UtilList.toString(" ", scope.getHtmlExtensions())); //$NON-NLS-1$ //$NON-NLS-2$
		progressPanel.appendInfo(Msg.ipref_skip_regex.value() + " " + UtilList.toString(" $ ", scope.getExclusionFilters())); //$NON-NLS-1$ //$NON-NLS-2$
		progressPanel.appendInfo(Msg.html_pairing.value() + " " + (scope.isDetectHTMLPairs() ? Msg.yes.value() : Msg.no.value())); //$NON-NLS-1$
		progressPanel.appendInfo(Msg.ipref_delete_on_exit.value() + ": " + (scope.isDeleteOnExit() ? Msg.yes.value() : Msg.no.value())); //$NON-NLS-1$
		progressPanel.appendInfo(Msg.waiting_in_queue.value());
		
		// Context menu of error table
		progressPanel.addErrorMenuItemOpen();
		progressPanel.addErrorMenuItemOpenParent();
		progressPanel.addErrorMenuItemSeparator();
		progressPanel.addErrorMenuItemCopy();
	}
	
	/**
	 * Returns the job represented by this tab.
	 */
	public Job getJob() {
		return job;
	}
	
	/**
	 * Appends the given message at the end of the feedback textbox.
	 */
	public void appendInfo(String str) {
		progressPanel.appendInfo(str);
	}
	
	/**
	 * Adds an error to be displayed in the error box.
	 */
	public void addError(ParseException error) {
		errorCache.add(error);
		progressPanel.addError(error.getMessage(), error.getFile().getAbsolutePath());
	}
	
	/**
	 * Returns all errors displayed in the error box.
	 */
	public ParseException[] getErrors() {
		return errorCache.toArray(new ParseException[errorCache.size()]);
	}

}
