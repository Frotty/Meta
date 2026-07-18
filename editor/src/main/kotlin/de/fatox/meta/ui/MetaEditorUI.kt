package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.ui.Table
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.info
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.components.MetaTabbedPane
import de.fatox.meta.ui.components.MetaTabbedPaneAdapter
import de.fatox.meta.ui.components.MetaTable
import de.fatox.meta.ui.tabs.MetaTab
import de.fatox.meta.ui.tabs.WelcomeTab

private val log = MetaLoggerFactory.logger {}

/**
 * Created by Frotty on 04.06.2016.
 */
class MetaEditorUI {
	private val uiManager: UIManager by lazyInject()

	lateinit var metaToolbar: EditorMenuBar

	private lateinit var tabbedPane: MetaTabbedPane
	private val tabTable = Table()
	private var shownTab: MetaTab? = null

	fun setup() {
		metaToolbar = EditorMenuBar()
		log.info { "Toolbar created" }
		uiManager.setMainMenuBar(metaToolbar.menuBar)
		tabbedPane = MetaTabbedPane()
		tabbedPane.addListener(object : MetaTabbedPaneAdapter() {
			override fun switchedMetaTab(tab: MetaTab) {
				shownTab?.onHide()
				uiManager.changeTab(tab::class)
				apply()
				val content = tab.contentTable
				tabTable.clearChildren()
				tabTable.add(content).grow()
				tabTable.toFront()
				content.toBack()
				tab.onShow()
				shownTab = tab
				uiManager.bringWindowsToFront()
			}
		})
		val visTable = MetaTable()
		visTable.add().top()
		visTable.add().grow()
		addTab(WelcomeTab())
	}

	fun apply() {
		uiManager.setMainMenuBar(metaToolbar.menuBar)
		uiManager.addActor(tabbedPane.layout, growX = true, growY = false)
		uiManager.addTable(tabTable, growX = true, growY = true)
	}

	fun addTab(tab: MetaTab) {
		tabbedPane.add(tab)
	}

	private fun getTab(name: String): MetaTab? =
		tabbedPane.tabs.firstOrNull { it.tabTitle.equals(name, ignoreCase = true) } as? MetaTab

	val currentTab: MetaTab? get() = tabbedPane.activeMetaTab

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
