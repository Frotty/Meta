package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.info
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.tabs.WelcomeTab

private val log = MetaLoggerFactory.logger {}

/**
 * Created by Frotty on 04.06.2016.
 */
class MetaEditorUI {
	private val uiManager: UIManager by lazyInject()

	lateinit var metaToolbar: EditorMenuBar

	private lateinit var tabbedPane: TabbedPane
	private val tabTable = Table()
	fun setup() {
		metaToolbar = EditorMenuBar()
		log.info { "Toolbar created" }
		uiManager.setMainMenuBar(metaToolbar.menuBar)
		tabbedPane = TabbedPane()
		tabbedPane.addListener(object : TabbedPaneAdapter() {
			override fun switchedTab(tab: Tab) {
				tabbedPane.activeTab.onHide()
				uiManager.changeTab(tab::class)
				apply()
				val content = tab.contentTable
				tabTable.clearChildren()
				tabTable.add(content).grow()
				tabTable.toFront()
				content.toBack()
				uiManager.bringWindowsToFront()
			}
		})
		val visTable = VisTable()
		visTable.add().top()
		visTable.add().grow()
		addTab(WelcomeTab())
	}

	fun apply() {
		uiManager.addTable(tabbedPane.table, growX = true, growY = false)
		uiManager.addTable(tabTable, growX = true, growY = true)
	}

	fun addTab(tab: Tab) {
		tabbedPane.add(tab)
	}

	private fun getTab(name: String): Tab? = tabbedPane.tabs.firstOrNull { it.tabTitle.equals(name, ignoreCase = true) }

	val currentTab: Tab? get() = tabbedPane.activeTab

	/**
	 * @return `true` if the focus was successfully gained, `false` otherwise
	 */
	fun tryFocusTab(name: String): Boolean {
		return getTab(name)?.also { tabbedPane.switchTab(it) } != null
	}

	fun tryCloseTab(name: String) {
		getTab(name)?.removeFromTabPane()
	}
}