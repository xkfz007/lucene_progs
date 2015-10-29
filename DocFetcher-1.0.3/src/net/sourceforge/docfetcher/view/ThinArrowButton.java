/*******************************************************************************
 * Copyright (c) 2009 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.view;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.util.UtilGUI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * A longish button with a small triangle-shaped arrow on it, the orientation of
 * which can be set with a style bit.
 * 
 * @author Tran Nam Quang
 */
public class ThinArrowButton extends Canvas {
	
	private int style;
	private int btWidth = 12; // width of the whole button
	private int btHeight = 100; // height of the whole button
	
	private int a = 2; // height of arrow divided by 2
	private int b = 5; // width of arrow divided by 2
	
	/**
	 * @param style
	 *            Either SWT.LEFT, SWT.RIGHT, SWT.UP or SWT.DOWN.
	 */
	public ThinArrowButton(Composite parent, int style) {
		/*
		 * Note: For some unknown reason, the arrow is not visible if the widget
		 * has no SWT.NO_BACKGROUND flag. This bug seemed to have occurred after
		 * migrating from SWT 3.4 to SWT 3.5.
		 */
		super(parent, useCustomBorder() ? SWT.NONE : SWT.BORDER | SWT.NO_BACKGROUND);
		
		if (useCustomBorder())
			UtilGUI.paintBorder(this);
		
		this.style = style;
		if ((style & SWT.UP) == SWT.UP || (style & SWT.DOWN) == SWT.DOWN) {
			int tmp = btWidth;
			btWidth = btHeight;
			btHeight = tmp;
		}
		
		// Highlight button when mouse hovers over it
		addMouseTrackListener(new MouseTrackAdapter() {
			public void mouseEnter(MouseEvent e) {
				setBackground(UtilGUI.getColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
			}
			public void mouseExit(MouseEvent e) {
				setBackground(UtilGUI.getColor(SWT.COLOR_WIDGET_BACKGROUND));
			}
		});
		
		// Draw the arrow
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				try {
					int style = ThinArrowButton.this.style;
					int[] coords = null;

					if (UtilGUI.contains(style, SWT.LEFT))
						coords = new int[] {-a, 0, a, b, a, -b};
					else if (UtilGUI.contains(style, SWT.RIGHT))
						coords = new int[] {a, 0, -a, -b, -a, b};
					else if (UtilGUI.contains(style, SWT.UP))
						coords = new int[] {0, -a, -b, a, b, a};
					else if (UtilGUI.contains(style, SWT.DOWN))
						coords = new int[] {0, a, b, -a, -b, -a};

					for (int i = 0; i < coords.length - 1; i = i + 2)
						coords[i] += btWidth / 2;
					for (int i = 1; i < coords.length; i = i + 2)
						coords[i] += btHeight / 2;

					if (useCustomBorder())
						e.gc.fillRectangle(2, 2, btWidth - 4, btHeight - 4);
					else
						e.gc.fillRectangle(0, 0, btWidth, btHeight);

					e.gc.setBackground(UtilGUI.getColor(SWT.COLOR_WIDGET_FOREGROUND));
					e.gc.setAntialias(SWT.ON);
					e.gc.fillPolygon(coords);
				} catch (SWTException ex) {
					/*
					 * FIXME This happens when no graphics library is found
					 * (GDI+), probably on an older Windows 2000 OS. This is
					 * only a temporary workaround. See bug #2943966.
					 */
				}
			}
		});
	}
	
	/**
	 * Returns true if we should draw our own borders instead of using the
	 * system borders. This is necessary on Windows (classic theme) and KDE,
	 * where the system borders look sort of ugly.
	 */
	private static boolean useCustomBorder() {
		return Const.IS_WINDOWS || System.getenv("KDE_FULL_SESSION") != null; //$NON-NLS-1$
	}
	
	public Point computeSize(int hint, int hint2, boolean changed) {
		return new Point(btWidth, btHeight);
	}
	
	/**
	 * Sets the orientation of the arrow. Expected values are SWT.LEFT,
	 * SWT.RIGHT, SWT.UP and SWT.DOWN. <br>
	 * Note that this will only change the orientation of the arrow, not the
	 * width and height of the button.
	 */
	public void setOrientation(int orientation) {
		style = style & ~SWT.LEFT & ~SWT.RIGHT & ~SWT.UP & ~SWT.DOWN;
		style |= orientation;
		redraw();
	}

}
