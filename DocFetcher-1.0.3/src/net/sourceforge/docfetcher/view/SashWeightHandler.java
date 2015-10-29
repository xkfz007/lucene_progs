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

import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

/**
 * This listener class causes the first child of a <tt>SashForm</tt> to keep a
 * constant width after the application window is resized. Although it extends
 * <tt>ControlListener</tt>, it does not make sense to add it as one. Use the
 * constructor instead.
 * 
 * @author Tran Nam Quang
 */
public class SashWeightHandler extends ControlAdapter {
	
	private Composite parent;
	private SashForm sash;
	private int parentWidthCache;
	private double[] cached_w = new double[2];
	
	// Assumption that the sash weights have already been set
	// Assumption that parent has a meaningful size (!= (0,0))
	public SashWeightHandler(final Composite parent, final SashForm sash) {
		this.parent = parent;
		this.sash = sash;
		parentWidthCache = parent.getSize().x;
		int[] weights = sash.getWeights();
		cached_w[0] = weights[0];
		cached_w[1] = weights[1];
		sash.addControlListener(this);
		sash.getChildren()[0].addControlListener(new ControlAdapter() {
			private int parentWidthCache2;
			private int parentHeightCache2;
			public void controlResized(ControlEvent e) {
				Point newParentSize = parent.getSize();
				if (newParentSize.x == parentWidthCache2 && newParentSize.y == parentHeightCache2) {
					int[] newWeights = sash.getWeights();
					cached_w[0] = newWeights[0];
					cached_w[1] = newWeights[1];
					parentWidthCache = parentWidthCache2;
				}
				parentWidthCache2 = newParentSize.x;
				parentHeightCache2 = newParentSize.y;
			}
		});
	}
	
	public void controlResized(ControlEvent e) {
		double w_ratio = cached_w[0] / cached_w[1];
		double constWidth = (w_ratio * parentWidthCache) / (1 + w_ratio);
		int[] new_W = new int[2];
		int newParentWidth = parent.getSize().x;
		new_W[0] = (int) Math.round(constWidth);
		new_W[1] = Math.max(0, newParentWidth - (int) Math.round(constWidth));
		if (new_W[0] == 0 || new_W[1] == 0)
			return;
		sash.setWeights(new_W);
	}
	
}
