package de.fatox.meta.ui.tabs;

import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import de.fatox.meta.Meta;

/**
 * Created by Frotty on 16.06.2016.
 */
public abstract class MetaTab extends Tab {

    public MetaTab() {
        Meta.inject(this);
    }

    public MetaTab(boolean savable) {
        super(savable);
        Meta.inject(this);
    }

    public MetaTab(boolean savable, boolean closeableByUser) {
        super(savable, closeableByUser);
        Meta.inject(this);
    }

}
