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

import net.sourceforge.docfetcher.enumeration.Filesize;
import net.sourceforge.docfetcher.enumeration.Key;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.util.Event;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * @author Tran Nam Quang
 */
public class FilesizeGroup extends GroupWrapper {
	
	/** Event: The minimum or maximum filesize setting has changed. */
	public final Event<long[]> evtValuesChanged = new Event<long[]> ();
	
	private Text minField;
	private Text maxField;
	private Combo minCombo;
	private Combo maxCombo;

	/** Cache of the current minimum filesize value. */
	private long minBytes = 0;
	
	/** Cache of the current minimum filesize value. */
	private long maxBytes = -1;
	
	private EventRedirector evtRedirector = new EventRedirector();

	public FilesizeGroup(Composite parent) {
		/*
		 * Creating widgets. They must be instantiated in this order to
		 * allow reasonable widget navigation using the tab key.
		 */
		super(parent);
		minField = new Text(group, SWT.RIGHT | SWT.SINGLE | SWT.BORDER);
		minCombo = new Combo(group, SWT.DROP_DOWN);
		maxField = new Text(group, SWT.RIGHT | SWT.SINGLE | SWT.BORDER);
		maxCombo = new Combo(group, SWT.DROP_DOWN);
		
		// Configure widgets
		group.setText(Msg.filesize_group_label.value());
		String[] comboItems = Filesize.valuesAsStrings();
		minCombo.setItems(comboItems);
		maxCombo.setItems(comboItems);
		minCombo.select(Filesize.KB.ordinal());
		maxCombo.select(Filesize.KB.ordinal());
		
		// Redirect various modification events to the notification method
		minField.addModifyListener(evtRedirector);
		maxField.addModifyListener(evtRedirector);
		evtRedirector.listenTo(minCombo);
		evtRedirector.listenTo(maxCombo);
		
		// Layout
		group.setLayout(new FormLayout());
		FormDataFactory fdf = FormDataFactory.getInstance();
		int m = FormDataFactory.DEFAULT_MARGIN;
		fdf.top().bottom().right(50, -m).applyTo(minCombo);
		fdf.right(minCombo, -m/2).left().applyTo(minField);
		fdf.reset().top().bottom().right().applyTo(maxCombo);
		fdf.right(maxCombo, -m/2).left(50, m).applyTo(maxField);
		
		/*
		 * FIXME On GNOME 2.18.1, all combos have a way too large default width,
		 * breaking the layout. Thus we're manually setting the width if its
		 * intial value seems to large.
		 */
		if (minCombo.computeSize(SWT.DEFAULT, SWT.DEFAULT).x > 150) {
			((FormData) minCombo.getLayoutData()).width = 60;
			((FormData) maxCombo.getLayoutData()).width = 60;
		}
		
		// Ensure the user can only enter non-negative integers into the textboxes
		VerifyListener numbersOnlyListener = new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				e.doit = e.text.matches("[0-9]*"); //$NON-NLS-1$
			}
		};
		minField.addVerifyListener(numbersOnlyListener);
		maxField.addVerifyListener(numbersOnlyListener);
		
		// Prevent the user from typing anything into the combos
		KeyListener noTypingListener = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				e.doit = false;
			}
		};
		minCombo.addKeyListener(noTypingListener);
		maxCombo.addKeyListener(noTypingListener);
		
		new FilesizeGroupNavigator(new Control[] {
				minField, minCombo, maxField, maxCombo
		});
	}
	
	public boolean setFocus() {
		return minField.setFocus();
	}
	
	/**
	 * Updates the cached minimum and maximum filesize values and refreshes all
	 * opened result tabs.
	 */
	private void notifyValuesChangedListeners() {
		// Set min/max defaults
		long minSize = minBytes = 0;
		long maxSize = maxBytes = -1;
		Filesize minUnit = Filesize.Byte;
		Filesize maxUnit = Filesize.Byte;
		
		// Try to change the defaults
		try {
			minSize = Long.parseLong(minField.getText());
			minUnit = Filesize.valueOf(minCombo.getText());
		} catch (Exception e) {
			minSize = 0;
			minUnit = Filesize.Byte;
		}
		try {
			maxSize = Long.parseLong(maxField.getText());
			maxUnit = Filesize.valueOf(maxCombo.getText());
		} catch (Exception e) {
			maxSize = -1;
			maxUnit = Filesize.Byte;
		}
		
		// Convert to bytes and save to cache
		if (minSize > 0)
			minBytes = Filesize.Byte.convert(minSize, minUnit);
		if (maxSize != -1)
			maxBytes = Filesize.Byte.convert(maxSize, maxUnit);
		
		evtValuesChanged.fireUpdate(new long[] {minBytes, maxBytes});
	}
	
	/**
	 * Finds out if the current minimum and maximum filesize values allow
	 * performing a search. If not, an error message is returned, otherwise
	 * returns null.
	 */
	public String checkSearchDisabled() {
		try {
			String numString1 = minField.getText();
			String numString2 = maxField.getText();
			int selIndex1 = minCombo.getSelectionIndex();
			int selIndex2 = maxCombo.getSelectionIndex();
			boolean parsable1 = ! numString1.equals("") && selIndex1 != -1; //$NON-NLS-1$
			boolean parsable2 = ! numString2.equals("") && selIndex2 != -1; //$NON-NLS-1$
			long num1 = parsable1 ? checkRange(numString1, selIndex1) : 0;
			long num2 = parsable2 ? checkRange(numString2, selIndex2) : 0;
			if (parsable1 && parsable2) {
				num1 *= Math.pow(1024, selIndex1);
				num2 *= Math.pow(1024, selIndex2);
				if (num1 > num2)
					return Msg.minsize_not_greater_maxsize.value();
			}
		} catch (NumberFormatException ex) {
			return Msg.filesize_out_of_range.value();
		}
		return null;
	}
	
	/**
	 * Checks if the provided filesize is smaller than Long.MAX_VALUE bytes and
	 * throws a NumberFormatException if not.
	 * 
	 * @param numString
	 *            The numeric value of the filesize
	 * @param power
	 *            The power corresponding to the unit of the filesize. For byte,
	 *            this is 0, for KB it's 1, for MB 2 and for GB 3.
	 * @return The given numerical string as a long number.
	 */
	private long checkRange(String numString, int power) throws NumberFormatException {
		long num = Long.parseLong(numString);
		long max = Long.MAX_VALUE;
		switch (power) {
		case 0: break;
		case 1:
			long maxKB = Filesize.KB.convert(max, Filesize.Byte);
			if (num <= maxKB) break;
		case 2:
			long maxMB = Filesize.MB.convert(max, Filesize.Byte);
			if (num <= maxMB) break;
		case 3:
			long maxGB = Filesize.GB.convert(max, Filesize.Byte);
			if (num <= maxGB) break;
		default:
			throw new NumberFormatException();
		}
		return num;
	}
	
	/**
	 * Redirects events from the text and combo widgets to
	 * <tt>notifyValuesChangedListener</tt>.
	 */
	private class EventRedirector implements ModifyListener, SelectionListener {
		public void listenTo(Combo combo) {
			combo.addSelectionListener(this);
			combo.addModifyListener(this);
		}
		public void widgetDefaultSelected(SelectionEvent e) {
			notifyValuesChangedListeners();
		}
		public void widgetSelected(SelectionEvent e) {
			notifyValuesChangedListeners();
		}
		public void modifyText(ModifyEvent e) {
			notifyValuesChangedListeners();
		}
	}
	
	/**
	 * Navigation in the filesize group
	 */
	private class FilesizeGroupNavigator extends KeyAdapter {
		
		private Control[] controls;
		
		FilesizeGroupNavigator(Control[] controls) {
			this.controls = controls;
			for (Control control : controls)
				control.addKeyListener(this);
		}
		
		public void keyReleased(KeyEvent e) {
			Key key = Key.getKey(e.stateMask, e.keyCode);
			if (key == null) return;
			
			// Get index of the control that triggered the event
			int index = -1;
			for (int i = 0; i < controls.length; i++)
				if (controls[i] == e.widget)
					index = i;
			if (index == -1) return;
			
			switch (key) {
			case Left:
				index = Math.max(0, index - 1);
				controls[index].setFocus();
				break;
			case Right:
				index = Math.min(controls.length - 1, index + 1);
				controls[index].setFocus();
				break;
			}
			
			if (e.widget instanceof Text) {
				Text text = (Text) e.widget;
				text.setSelection(0, text.getCharCount());
			}
			else if (e.widget instanceof Combo) {
				Combo combo = (Combo) e.widget;
				int selIndex = combo.getSelectionIndex();
				switch (key) {
				case Up:
					selIndex = Math.max(0, selIndex - 1);
					combo.select(selIndex);
					break;
				case Down:
					selIndex = Math.min(combo.getItemCount() - 1, selIndex + 1);
					combo.select(selIndex);
					break;
				}
			}
		}
		
	}

}
