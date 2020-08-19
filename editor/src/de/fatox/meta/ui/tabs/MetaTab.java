package de.fatox.meta.ui.tabs;

import com.kotcrab.vis.ui.widget.tabbedpane.Tab;

/**
 * Created by Frotty on 16.06.2016.
 */
public abstract class MetaTab extends Tab {

	public MetaTab() {
	}

	public MetaTab(boolean savable) {
		super(savable);
	}

	public MetaTab(boolean savable, boolean closeableByUser) {
		super(savable, closeableByUser);
	}

}
