package de.fatox.meta.ui.tabs

import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import de.fatox.meta.Meta.Companion.inject

/**
 * Created by Frotty on 16.06.2016.
 */
abstract class MetaTab : Tab {
    constructor() {
        inject(this)
    }

    constructor(savable: Boolean) : super(savable) {
        inject(this)
    }

    constructor(savable: Boolean, closeableByUser: Boolean) : super(savable, closeableByUser) {
        inject(this)
    }
}