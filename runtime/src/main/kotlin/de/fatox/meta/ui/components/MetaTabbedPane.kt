package de.fatox.meta.ui.components

import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter
import de.fatox.meta.ui.tabs.MetaTab

class MetaTabbedPane : TabbedPane() {
	val activeMetaTab: MetaTab? get() = activeTab as? MetaTab

	fun add(tab: MetaTab) {
		super.add(tab)
	}

	fun switchTab(tab: MetaTab) {
		super.switchTab(tab)
	}
}

open class MetaTabbedPaneAdapter : TabbedPaneAdapter() {
	final override fun switchedTab(tab: Tab) {
		switchedMetaTab(tab as? MetaTab ?: return)
	}

	open fun switchedMetaTab(tab: MetaTab) = Unit
}
