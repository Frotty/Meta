package de.fatox.meta.ui.components

import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.tabs.MetaTab

class MetaTabbedPane : TabbedPane() {
	val activeMetaTab: MetaTab? get() = activeTab as? MetaTab
	val activeMetaTabValue: Signal<MetaTab?> = signal(null)

	fun add(tab: MetaTab) {
		super.add(tab)
		activeMetaTabValue.value = activeMetaTab
	}

	fun switchTab(tab: MetaTab) {
		super.switchTab(tab)
		activeMetaTabValue.value = activeMetaTab
	}
}

open class MetaTabbedPaneAdapter : TabbedPaneAdapter() {
	final override fun switchedTab(tab: Tab) {
		switchedMetaTab(tab as? MetaTab ?: return)
	}

	open fun switchedMetaTab(tab: MetaTab) = Unit
}
