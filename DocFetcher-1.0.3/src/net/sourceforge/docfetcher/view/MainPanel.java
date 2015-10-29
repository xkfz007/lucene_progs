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

import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.util.Event;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * The main panel of the application, containing the search bar, the result
 * panel and the preview panel.
 * 
 * @author Tran Nam Quang
 */
public class MainPanel extends Composite {
	
	private SashForm sash;
	private SearchPanel searchPanel;
	private PreviewPanel previewPanel;
	private Composite searchPanelWithButtons;
	private ThinArrowButton togglePreviewBottomBt;
	private ThinArrowButton togglePreviewRightBt;
	private ThinArrowButton togglePreviewRightBt2;

	public MainPanel(Composite parent) {
		super(parent, SWT.NONE);
		setLayout(GridLayoutFactory.fillDefaults().numColumns(3).spacing(0, 0).create());
		
		// Button to toggle filter panel
		int btLeftStyle = Pref.Bool.ShowFilterPanel.getValue() ? SWT.LEFT : SWT.RIGHT;
		final ThinArrowButton toggleFilterPanelBt = new ThinArrowButton(this, btLeftStyle);
		toggleFilterPanelBt.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		
		// The sash that separates the result panel from the preview panel
		sash = new SashForm(this, Pref.Bool.PreviewBottom.getValue() ? SWT.VERTICAL : SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		boolean previewBottom = Pref.Bool.PreviewBottom.getValue();
		boolean showPreview = Pref.Bool.ShowPreview.getValue();
		
		// Button to toggle preview panel #1
		int btRightStyle = showPreview && ! previewBottom ? SWT.RIGHT : SWT.LEFT;
		togglePreviewRightBt = new ThinArrowButton(this, btRightStyle);
		togglePreviewRightBt.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 2));
		
		// Wrapper composite
		searchPanelWithButtons = new Composite(sash, SWT.NONE);
		searchPanelWithButtons.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).create());
		
		// Panel for search results and the toolbar above it
		searchPanel = new SearchPanel(searchPanelWithButtons);
		searchPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// Button to toggle preview panel #2
		int btRightStyle2 = showPreview && ! previewBottom ? SWT.RIGHT : SWT.LEFT;
		togglePreviewRightBt2 = new ThinArrowButton(searchPanelWithButtons, btRightStyle2);
		togglePreviewRightBt2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 2));
		
		// Button to toggle preview panel #3
		int btBottomStyle = showPreview && previewBottom ? SWT.DOWN : SWT.UP;
		togglePreviewBottomBt = new ThinArrowButton(searchPanelWithButtons, btBottomStyle);
		togglePreviewBottomBt.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, false, false));
		updatePreviewButtons();
		
		// Preview panel
		previewPanel = new PreviewPanel(sash);
		if (Pref.Bool.ShowPreview.getValue()) {
			loadSashWeights();
			previewPanel.setActive(true);
		}
		else 
			sash.setMaximizedControl(searchPanelWithButtons);
		
		// Toogle filter panel
		toggleFilterPanelBt.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				boolean showFilterPanel = ! Pref.Bool.ShowFilterPanel.getValue();
				Pref.Bool.ShowFilterPanel.setValue(showFilterPanel);
			}
		});
		
		// Toggle preview panel
		togglePreviewBottomBt.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				if (Pref.Bool.ShowPreview.getValue() && Pref.Bool.PreviewBottom.getValue())
					Pref.Bool.ShowPreview.setValue(false);
				else {
					Pref.Bool.ShowPreview.setValue(true);
					Pref.Bool.PreviewBottom.setValue(true);
				}
			}
		});
		MouseAdapter previewRightBtHandler = new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				if (Pref.Bool.ShowPreview.getValue() && ! Pref.Bool.PreviewBottom.getValue())
					Pref.Bool.ShowPreview.setValue(false);
				else {
					Pref.Bool.ShowPreview.setValue(true);
					Pref.Bool.PreviewBottom.setValue(false);
				}
			}
		};
		togglePreviewRightBt.addMouseListener(previewRightBtHandler);
		togglePreviewRightBt2.addMouseListener(previewRightBtHandler);
		
		Pref.Bool.ShowFilterPanel.evtChanged.add(new Event.Listener<Boolean> () {
			public void update(Boolean eventData) {
				toggleFilterPanelBt.setOrientation(eventData ? SWT.LEFT : SWT.RIGHT);
			}
		});
		
		Pref.Bool.ShowPreview.evtChanged.add(new Event.Listener<Boolean> () {
			public void update(Boolean eventData) {
				updatePreviewButtons();
				setPreviewVisible(eventData);
			}
		});
		
		Pref.Bool.PreviewBottom.evtChanged.add(new Event.Listener<Boolean> () {
			public void update(Boolean eventData) {
				updatePreviewButtons();
				setPreviewBottom(eventData);
			}
		});
	}
	
	/**
	 * Sets the orientation and visibility of the three buttons that toggle the
	 * preview panel according to the current preferences settings.
	 */
	private void updatePreviewButtons() {
		boolean showPreview = Pref.Bool.ShowPreview.getValue();
		boolean previewBottom = Pref.Bool.PreviewBottom.getValue();
		boolean bottomVisible = showPreview && previewBottom;
		boolean rightVisible = showPreview && ! previewBottom;
		
		togglePreviewBottomBt.setOrientation(bottomVisible ? SWT.DOWN : SWT.UP);
		togglePreviewRightBt.setOrientation(rightVisible ? SWT.RIGHT : SWT.LEFT);
		togglePreviewRightBt2.setOrientation(rightVisible ? SWT.RIGHT : SWT.LEFT);
		
		togglePreviewRightBt.setVisible(bottomVisible);
		((GridData) togglePreviewRightBt.getLayoutData()).exclude = ! bottomVisible;
		((GridLayout) getLayout()).numColumns = bottomVisible ? 3 : 2;

		togglePreviewRightBt2.setVisible(! bottomVisible);
		((GridData) togglePreviewRightBt2.getLayoutData()).exclude = bottomVisible;
		((GridLayout) searchPanelWithButtons.getLayout()).numColumns = bottomVisible ? 1 : 2;
		
		layout();
	}
	
	/**
	 * Sets the sash weights using the values in the preferences.
	 */
	private void loadSashWeights() {
		sash.setWeights(sash.getOrientation() == SWT.VERTICAL ?
				Pref.IntArray.SashRightVerticalWeights.getValue() :
				Pref.IntArray.SashRightHorizontalWeights.getValue()
		);
	}
	
	/**
	 * Returns whether the preview panel is visible.
	 */
	public boolean isPreviewVisible() {
		return sash.getMaximizedControl() == null;
	}
	
	/**
	 * Sets whether the preview panel is visible.
	 */
	private void setPreviewVisible(boolean show) {
		if (show) {
			sash.setMaximizedControl(null);
			previewPanel.setActive(true);
			loadSashWeights();
		}
		else {
			/**
			 * If we hide the preview panel when it's focused, it will keep its
			 * focus even after becoming invisible, which would disable global
			 * key events.
			 */
			if (isPreviewFocused())
				searchPanel.setFocus();
			
			saveWeights();
			previewPanel.setActive(false);
			sash.setMaximizedControl(searchPanelWithButtons);
		}
		if (show != Pref.Bool.ShowPreview.getValue())
			Pref.Bool.ShowPreview.setValue(show);
	}
	
	/**
	 * Returns whether the preview is shown below the result panel (instead of
	 * on the right).
	 */
	public boolean isPreviewBottom() {
		return sash.getOrientation() == SWT.VERTICAL;
	}
	
	/**
	 * Sets whether the preview is shown below the result panel (instead of
	 * on the right).
	 */
	private void setPreviewBottom(boolean bottom) {
		if (bottom == (sash.getOrientation() == SWT.VERTICAL)) return;
		sash.setOrientation(bottom ? SWT.VERTICAL : SWT.HORIZONTAL);
		if (isPreviewVisible()) {
			if (bottom) {
				Pref.IntArray.SashRightHorizontalWeights.setValue(sash.getWeights());				sash.setWeights(Pref.IntArray.SashRightVerticalWeights.getValue());
			}
			else {
				Pref.IntArray.SashRightVerticalWeights.setValue(sash.getWeights());				sash.setWeights(Pref.IntArray.SashRightHorizontalWeights.getValue());
			}
		}
		if (bottom != Pref.Bool.PreviewBottom.getValue())
			Pref.Bool.PreviewBottom.setValue(bottom);	}
	
	/**
	 * Save the current sash weights to the preferences.
	 */
	public void saveWeights() {
		if (sash.getOrientation() == SWT.VERTICAL)
			Pref.IntArray.SashRightVerticalWeights.setValue(sash.getWeights());		else
			Pref.IntArray.SashRightHorizontalWeights.setValue(sash.getWeights());	}
	
	/**
	 * Bring the focus to the search box.
	 */
	public void focusSearchBox() {
		searchPanel.setFocus();
	}
	
	/**
	 * Returns the search panel.
	 */
	public SearchPanel getSearchPanel() {
		return searchPanel;
	}
	
	/**
	 * Returns the result panel.
	 */
	public ResultPanel getResultPanel() {
		return searchPanel.getResultPanel();
	}
	
	/**
	 * Returns the preview panel.
	 */
	public PreviewPanel getPreviewPanel() {
		return previewPanel;
	}
	
	/**
	 * If the internal HTML viewer is available, this method displays the help
	 * page in it, opens the preview panel if necessary and return true. If not,
	 * it opens the help page in the external HTML browser and returns false.
	 */
	public boolean showHelpPage() {
		if (previewPanel.showHelpPage()) {
			setPreviewVisible(true);
			return true;
		}
		return false;
	}
	
	/**
	 * Returns whether the preview panel or one of its child controls have the
	 * user-interface focus.
	 */
	public boolean isPreviewFocused() {
		return hasFocus(previewPanel);
	}
	
	/**
	 * Recursively determines whether the given composite or any of its children
	 * have the user-interface focus.
	 */
	private boolean hasFocus(Composite comp) {
		if (comp.isFocusControl())
			return true;
		for (Control child : comp.getChildren()) {
			if (child instanceof Composite) {
				if (hasFocus((Composite) child))
					return true;
			}
			else if (child.isFocusControl())
				return true;
		}
		return false;
	}
	
}
