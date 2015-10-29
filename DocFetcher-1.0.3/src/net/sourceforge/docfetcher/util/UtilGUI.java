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

package net.sourceforge.docfetcher.util;


import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.enumeration.Msg;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * GUI-related utility methods.
 * 
 * @author Tran Nam Quang
 */
public class UtilGUI {

	/**
	 * Static use only.
	 */
	private UtilGUI() {
	}

	/**
	 * Convenience method for retrieving system colors. Enter SWT stylebits as
	 * parameter (e.g. <code>SWT.COLOR_WHITE</code>).
	 */
	public static Color getColor(int styleBit) {		
		return Display.getDefault().getSystemColor(styleBit);		
	}
	
	/**
	 * Checks if the given SWT style bit contains the second SWT style bit.
	 */
	public static boolean contains(int styleBit, int otherStyleBit) {
		return (styleBit & otherStyleBit) == otherStyleBit;
	}
	
	/**
	 * Places <tt>shell</tt> at the center of the shell <tt>parent</tt>, or
	 * in the middle of the screen if <tt>parent</tt> is null. Note that the
	 * shell size must have already been set in order to get a correct result.
	 */
	public static void centerShell(Shell parent, Shell shell) {
		Rectangle parentBounds = null;
		if (parent == null)
			parentBounds = shell.getMonitor().getBounds();
		else
			parentBounds = parent.getBounds();
		int parentWidth = parentBounds.width;
		int parentHeight = parentBounds.height;
		int shellWidth = shell.getSize().x;
		int shellHeight = shell.getSize().y;
		int shellPosX = (parentWidth - shellWidth) / 2;
		int shellPosY = (parentHeight - shellHeight) / 2;
		if (parent != null) {
			shellPosX += parentBounds.x;
			shellPosY += parentBounds.y;
		}
		shell.setLocation(shellPosX, shellPosY);
	}
	
	/**
	 * Displays a message box with the given title, message and SWT flags
	 * (SWT.OK, SWT.CANCEL, etc.). Returns the return code of the message box
	 * (e.g. SWT.OK if the OK button was pressed).
	 * <p>
	 * This method creates its own shell if none is found, and it can be called
	 * from a non-GUI thread.
	 */
	private static int showMsg(final String title, final String message, final int flags) {
		if (Display.getCurrent() == null) {
			class MyRunnable implements Runnable {
				public int answer = -1;
				public void run() {
					answer = showMsg(title, message, flags);
				}
			}
			MyRunnable myRunnable = new MyRunnable();
			Display.getDefault().syncExec(myRunnable);
			return myRunnable.answer;
		}
		Shell activeShell = Display.getDefault().getActiveShell();
		if (activeShell != null) {  
			MessageBox msgBox = new MessageBox(activeShell, flags);
			msgBox.setText(title);
			msgBox.setMessage(message);
			return msgBox.open();
		}
		/*
		 * Somehow, activeShell can be null at this point. See bug #2792186.
		 */
		else {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			shell.setSize(1, 1);
			UtilGUI.centerShell(null, shell);
			shell.open();
			shell.setVisible(false);
			MessageBox msgBox = new MessageBox(shell, flags);
			msgBox.setText(title);
			msgBox.setMessage(message);
			int answer = msgBox.open();
			return answer;
		}
	}

	/**
	 * Shows a message box with an error icon.
	 * <p>
	 * This method creates its own shell if none is found, and it can be called
	 * from a non-GUI thread.
	 */
	public static void showErrorMsg(String message) {
		showMsg(Msg.system_error.value(), message, SWT.ICON_ERROR | SWT.OK | SWT.PRIMARY_MODAL);
	}

	/**
	 * Shows a message box with a question icon.
	 * <p>
	 * This method creates its own shell if none is found, and it can be called
	 * from a non-GUI thread.
	 * 
	 * @return The return code of the message box, either SWT.OK or SWT.CANCEL
	 */
	public static int showConfirmMsg(String message) {
		return showMsg(Msg.confirm_operation.value(), message, SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL | SWT.PRIMARY_MODAL);
	}

	/**
	 * Shows a message box with an information icon.
	 * <p>
	 * This method creates its own shell if none is found, and it can be called
	 * from a non-GUI thread.
	 */
	public static void showInfoMsg(String message) {
		showMsg("", message, SWT.ICON_INFORMATION | SWT.OK | SWT.PRIMARY_MODAL);
	}

	/**
	 * Shows a message box with a warning icon.
	 * <p>
	 * This method creates its own shell if none is found, and it can be called
	 * from a non-GUI thread.
	 */
	public static void showWarningMsg(String message) {
		showMsg(Msg.invalid_operation.value(), message, SWT.ICON_WARNING | SWT.OK | SWT.PRIMARY_MODAL);
	}

	public static Point minimum(Point target, Point min) {
		Point out = new Point(target.x, target.y);
		if (out.x < min.x) out.x = min.x;
		if (out.y < min.y) out.y = min.y;
		return out;
	}
	
	private static class SelectAllOnFocus extends MouseAdapter implements FocusListener {
		private boolean selectAllTextAllowed = false;
		private Control text;
		SelectAllOnFocus(Control text) {
			this.text = text;
			if (! (text instanceof Combo) && ! (text instanceof Text))
				throw new IllegalArgumentException();
			text.addFocusListener(this);
			text.addMouseListener(this);
		}
		public void focusGained(FocusEvent e) {
			Point sel = null;
			int textLength = -1;
			if (text instanceof Combo) {
				sel = ((Combo) text).getSelection();
				textLength = ((Combo) text).getText().length();
			}
			else if (text instanceof Text) {
				sel = ((Text) text).getSelection();
				textLength = ((Text) text).getText().length();
			}
			
			selectAllTextAllowed = sel.x != 0 || sel.y != textLength;
		}
		public void focusLost(FocusEvent e) {
		}
		public void mouseDown(MouseEvent e) {
			if (selectAllTextAllowed) {
				if (text instanceof Combo) {
					((Combo) text).setSelection(new Point(0, ((Combo) text).getText().length()));
				}
				else if (text instanceof Text) {
					((Text) text).setSelection(new Point(0, ((Text) text).getText().length()));
				}
			}
			selectAllTextAllowed = false;
		}
	}
	
	/**
	 * Applying this method on the given widget will cause all the text in it to
	 * be selected if the user clicks on it after coming back from another part
	 * of the GUI or another program. The widget must be a Combo or a Text
	 * widget.
	 */
	public static void selectAllOnFocus(Control text) {
		new SelectAllOnFocus(text);
	}
	
	/**
	 * Is the KeyEvent a carriage return ?
	 * It can be the normal one or from the keypad
	 */
	public static boolean isCRKey (KeyEvent e){
		return (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR);
	}
	
	/**
	 * Creates a Composite with the given parent and a custom border. This
	 * method is used because creating a normal SWT Composite with SWT.BORDER
	 * style results in an ugly "etched in" look on Windows when the classic
	 * theme is used.
	 * <p>
	 * The additional parameter <tt>setFormLayout</tt> sets a FormLayout with
	 * margins specifically adjusted for the custom border as the layout of the
	 * returned Composite.
	 */
	public static Composite createCompositeWithBorder(Composite parent, boolean setFormLayout) {
		// Draw custom border on Windows
		final Composite comp = new Composite(parent, Const.IS_WINDOWS ? SWT.NONE : SWT.BORDER);
		if (Const.IS_WINDOWS)
			paintBorder(comp);
		
		// Add FormLayout with adjusted margins
		if (setFormLayout) {
			FormLayout formLayout = new FormLayout();
			if (Const.IS_WINDOWS)
				formLayout.marginWidth = formLayout.marginHeight = 2;
			comp.setLayout(formLayout);
		}
		
		return comp;
	}
	
	/**
	 * Paints a border around the given Composite. This can be used as a
	 * replacement for the ugly native border of Composites with SWT.BORDER
	 * style on Windows with classic theme turned on.
	 */
	public static void paintBorder(final Composite comp) {
		comp.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				Point size = comp.getSize();
				e.gc.setForeground(getColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
				e.gc.drawRectangle(0, 0, size.x - 1, size.y - 1);
				e.gc.setForeground(getColor(SWT.COLOR_WHITE));
				e.gc.drawRectangle(1, 1, size.x - 3, size.y - 3);
			}
		});
	}
	
}
