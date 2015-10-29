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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.enumeration.Icon;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.model.FileAlreadyExistsException;
import net.sourceforge.docfetcher.util.UtilFile;
import net.sourceforge.docfetcher.util.UtilGUI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Tran Nam Quang
 */
public class FileTransferDialog {
	
	private Shell shell;
	private ProgressPanel progressPanel;
	private ProgressBar progressBar;
	private Button cancelBt;
	private int currentSize;
	private File newParent;
	private Thread thread;
	
	public FileTransferDialog(Shell parent, String title) {
		// Create shell
		shell = new Shell(parent, Const.DIALOG_STYLE);
		shell.setSize(Pref.Int.IndexingBoxWidth.getValue(), Pref.Int.IndexingBoxHeight.getValue());
		UtilGUI.centerShell(parent, shell);
		FormLayout layout = new FormLayout();
		layout.marginWidth = layout.marginHeight = 2;
		shell.setLayout(layout);
		shell.setImage(Icon.INFO.getImage());
		shell.setText(title);
		
		// Create widgets
		progressPanel = new ProgressPanel(shell);
		progressBar = new ProgressBar(shell, SWT.HORIZONTAL);
		cancelBt = new Button(shell, SWT.PUSH);
		cancelBt.setText(Msg.cancel.value());
		
		// Error box context menu
		progressPanel.addErrorMenuItemOpen();
		progressPanel.addErrorMenuItemOpenParent();
		progressPanel.addErrorMenuItem(Msg.open_target_folder.value(), false, new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (newParent != null)
					UtilFile.launch(newParent.getAbsolutePath());
			}
		});
		progressPanel.addErrorMenuItemSeparator();
		progressPanel.addErrorMenuItemCopy();
		
		// Layout
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.minWidth(Const.MIN_BT_WIDTH).bottom().right().applyTo(cancelBt);
		fdf.reset().bottom(cancelBt).left().right().applyTo(progressBar);
		fdf.bottom(progressBar).top().applyTo(progressPanel);
		
		// Send interrupt signal to copy thread when cancel button is clicked
		cancelBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				thread.interrupt();
				shell.close();
			}
		});
	}

	/**
	 * @see org.eclipse.swt.widgets.Shell#open()
	 */
	public void open() {
		shell.open();
	}
	
	/**
	 * Moves the files given in the array to the new folder given by
	 * <tt>newParent</tt>. The file transfer is done in a thread. HTML pairs are
	 * detected automatically.
	 */
	public void transferFiles(File[] files, final File newParent) {
		currentSize = 0;
		this.newParent = newParent;
		final File[] allFiles = UtilFile.completeHTMLPairs(files);
		int totalSize = (int) UtilFile.getSizeInKB(allFiles);
		progressPanel.appendInfo(Msg.moving_files.format(new Object[] {
				allFiles.length, totalSize, newParent.getAbsolutePath()}));
		progressBar.setMinimum(0);
		progressBar.setMaximum(totalSize);
		progressBar.setState(SWT.NORMAL);
		progressBar.setSelection(0);
		
		// File transfer thread
		thread = new Thread() {
			public void run() {
				// Copy files
				List<File> filesToDelete = new ArrayList<File> ();
				boolean errorOccurred = false;
				for (File file : allFiles) {
					// Try to copy a file
					String filePath = file.getAbsolutePath();
					try {
						progressPanel.appendInfo(Msg.copying.format(filePath));
						UtilFile.copy(file, newParent);
						filesToDelete.add(file);
					} catch (FileNotFoundException e) {
						String msg = file.isFile() ? Msg.file_not_found.value() :
							Msg.folder_not_found.value();
						progressPanel.appendInfo("### " + msg); //$NON-NLS-1$
						progressPanel.addError(msg, filePath);
						errorOccurred = true;
					} catch (IOException e) {
						progressPanel.appendInfo("### " + Msg.file_not_readable.value()); //$NON-NLS-1$
						progressPanel.addError(Msg.file_not_readable.value(), filePath);
						errorOccurred = true;
					} catch (FileAlreadyExistsException e) {
						String msg = file.isFile() ? Msg.file_already_exists_dot.value() :
							Msg.folder_already_exists.value();
						progressPanel.appendInfo("### " + msg); //$NON-NLS-1$
						progressPanel.addError(msg, filePath);
						errorOccurred = true;
					}
					currentSize += UtilFile.getSizeInKB(file);
					
					// Update progress bar
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							progressBar.setSelection(currentSize);
						}
					});
					
					if (Thread.currentThread().isInterrupted())
						return;
				}
				
				// Delete original files
				for (File file : filesToDelete) {
					progressPanel.appendInfo(Msg.deleting.format(file.getAbsolutePath()));
					UtilFile.delete(file, true);
				}
				
				// Display some informative messages if there are errors
				final boolean errorOccurredFinal = errorOccurred;
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						if (errorOccurredFinal) {
							progressPanel.appendInfo(Msg.finished_with_errors.value());
							cancelBt.setText(Msg.close.value());
							FormDataFactory.getInstance().minWidth(Const.MIN_BT_WIDTH).bottom().right().applyTo(cancelBt);
						}
						else
							shell.close();
					}
				});
			}
		};
		thread.start();
	}

}
