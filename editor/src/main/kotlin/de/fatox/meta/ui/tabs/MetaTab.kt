package de.fatox.meta.ui.tabs

import com.kotcrab.vis.ui.widget.tabbedpane.Tab

/**
 * Created by Frotty on 16.06.2016.
 */
abstract class MetaTab(savable: Boolean = false, closeableByUser: Boolean = true) : Tab(savable, closeableByUser)