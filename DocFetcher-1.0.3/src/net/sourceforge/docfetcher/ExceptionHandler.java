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

package net.sourceforge.docfetcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.docfetcher.view.ErrorDialog;

import org.eclipse.swt.widgets.Display;

/**
 * This class makes the system error output show up not only on the console, but
 * also in an error window and in a text file.
 * 
 * @author Tran Nam Quang
 */
public class ExceptionHandler {

	/**
	 * A convenience variable to turn this class off.
	 */
	private boolean forceDisabled;

	/**
	 * The error dialog the error output is send to.
	 */
	private ErrorDialog errorDialog;
	
	/**
	 * The FileWriter used to write the error output to a file.
	 */
	private FileWriter fileWriter;
	
	/**
	 * The default error printstream provided by the system. This needs to be
	 * saved so we can both output to the default printstream and the textbox.
	 */
	private PrintStream defaultErr = System.err;

	/**
	 * Customized error output stream.
	 */
	private static PrintStream customErr;

	/**
	 * Enables or disables the customized error outputs provided by this class.
	 * When enabled, errors are written to the console, a GUI component and a
	 * text file. When disabled, errors are only written to the console.
	 */
	public void setEnabled(boolean enabled) {
		if (forceDisabled) return;
		if (enabled) {
			if (customErr == null)
				customErr = new PrintStream(new CustomError());
			System.setErr(customErr);
		}
		else {
			System.setErr(defaultErr);
		}
	}
	
	/**
	 * Close the file output stream if one was opened.
	 */
	public void closeErrorFile() {
		if (fileWriter == null) return;
		try {
			fileWriter.close();
		} catch (IOException e) {
		}
	}
	
	/**
	 * Appends the given String to the error textfile and the textbox of the
	 * error dialog.
	 */
	private void appendError(String str) {
		if (errorDialog == null) {
			/*
			 * Try to give the error dialog box an informative title, because
			 * that's what most bug reporters will choose for the bug report
			 * summary line.
			 */
			Pattern errorPattern = Pattern.compile(".*?\\.([^.]*?)\\:.*"); //$NON-NLS-1$
			Matcher matcher = errorPattern.matcher(str);
			String shellTitle = null;
			if (matcher.matches())
				shellTitle = matcher.group(1);
			errorDialog = new ErrorDialog(shellTitle);
			
			// Add some useful system info to the stacktrace
			String[] programVersion = DocFetcher.getProgramVersion();
			if (programVersion == null)
				programVersion = new String[] {"Unknown", "Unknown"}; //$NON-NLS-1$ //$NON-NLS-2$
			errorDialog.append("program.version=" + programVersion[0] + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			errorDialog.append("program.portable=" + Const.IS_PORTABLE + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			errorDialog.append("program.build=" + programVersion[1] + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			String[] keys = new String[] {
					"java.runtime.name", //$NON-NLS-1$
					"java.runtime.version", //$NON-NLS-1$
					"java.version", //$NON-NLS-1$
					"os.arch", //$NON-NLS-1$
					"os.name", //$NON-NLS-1$
					"os.version", //$NON-NLS-1$
					"user.language" //$NON-NLS-1$
			};
			for (String key : keys)
				errorDialog.append(key + "=" + System.getProperty(key) + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		errorDialog.append(str);
		try {
			if (fileWriter == null) {
				File file = new File(Const.ERROR_FILEPATH);
				boolean fileExists = file.exists();
				fileWriter = new FileWriter(file, true);
				
				/*
				 * If the file already exists, but the FileWriter was null, then
				 * DocFetcher must have been launched at least twice within a
				 * single minute (due to the minute resolution of the file's
				 * date signature), and each time an error was printed. In other
				 * words, we're writing multiple stacktraces to a single file.
				 * In order to make the file output more readable, we're
				 * separating the stacktraces with this line terminator:
				 */
				if (fileExists) fileWriter.write(Const.LS);
			}
			fileWriter.write(str);
			fileWriter.flush();
		} catch (IOException e) {
			// This might happen when the DocFetcher is run from a CD-ROM
		}
	}
	
	/**
	 * A custom implementation of a printstream that writes to a textbox and a
	 * text file.
	 */
	class CustomError extends OutputStream {

		public void write(int b) throws IOException {
			defaultErr.write(b);

			/*
			 * Check if we're running in the GUI thread. If not, we cannot
			 * directly write into the textbox, but instead have to
			 * instantiate a Runnable for each method call, which is
			 * much slower.
			 */
			if (Display.getCurrent() != null) {
				appendError(String.valueOf((char) b));
			}
			else {
				Display display = Display.getDefault();
				if (display == null || display.isDisposed()) return;
				final int fb = b;
				display.syncExec(new Runnable() {
					public void run() {
						appendError(String.valueOf((char) fb));
					}
				});
			}
		}

		public void write(byte[] b, int off, int len) throws IOException {
			defaultErr.write(b, off, len);

			// Check input
			if (b == null)
				throw new NullPointerException("No byte array to write to."); //$NON-NLS-1$
			else if ((off < 0) || (off > b.length) || (len < 0) ||
					((off + len) > b.length) || ((off + len) < 0))
				throw new IndexOutOfBoundsException();
			else if (len == 0)
				return;

			// Convert to StringBuffer
			final StringBuffer buf = new StringBuffer(len);
			for (int i = 0; i < len; i++)
				buf.append((char) b[off + i]);

			// Display message
			/*
			 * Check if we're running in the GUI thread. If not, we cannot
			 * directly write into the textbox, but instead have to
			 * instantiate a Runnable for each method call, which is
			 * much slower.
			 */
			if (Display.getCurrent() != null)
				appendError(buf.toString());
			else {
				Display display = Display.getDefault();
				if (display == null || display.isDisposed()) return;
				display.syncExec(new Runnable() {
					public void run() {
						appendError(buf.toString());
					}
				});
			}
		}

	}

}
