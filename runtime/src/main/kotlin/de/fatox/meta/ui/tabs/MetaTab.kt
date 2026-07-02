package de.fatox.meta.ui.tabs

import com.kotcrab.vis.ui.widget.tabbedpane.Tab

abstract class MetaTab(savable: Boolean = false, closeableByUser: Boolean = true) : Tab(savable, closeableByUser)
