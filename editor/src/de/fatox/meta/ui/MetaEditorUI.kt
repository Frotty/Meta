package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.tabs.WelcomeTab
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by Frotty on 04.06.2016.
 */
class MetaEditorUI {
	private val uiManager: UIManager by lazyInject()

	lateinit var metaToolbar: EditorMenuBar

	private var tabbedPane: TabbedPane? = null
	private val tabTable = Table()
	fun setup() {
		metaToolbar = EditorMenuBar()
		log.info("Toolbar created")
		uiManager.setMainMenuBar(metaToolbar.menuBar)
		tabbedPane = TabbedPane()
		tabbedPane!!.addListener(object : TabbedPaneAdapter() {
			override fun switchedTab(tab: Tab) {
				tabbedPane!!.activeTab.onHide()
				uiManager.changeScreen(tab.javaClass.name)
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
		uiManager.addTable(tabbedPane!!.table, true, false)
		uiManager.addTable(tabTable, true, true)
	}

	fun addTab(tab: Tab?) {
		tabbedPane!!.add(tab)
	}

	fun hasTab(name: String): Boolean {
		return getTab(name) != null
	}

	private fun getTab(name: String): Tab? {
		for (tab in tabbedPane!!.tabs) {
			if (tab.tabTitle.equals(name, ignoreCase = true)) {
				return tab
			}
		}
		return null
	}

	val currentTab: Tab?
		get() = tabbedPane!!.activeTab

	fun focusTab(name: String) {
		if (hasTab(name)) {
			tabbedPane!!.switchTab(getTab(name))
		}
	}

	fun closeTab(name: String) {
		if (hasTab(name)) {
			getTab(name)!!.removeFromTabPane()
		}
	}
}

private val log: Logger = LoggerFactory.getLogger(MetaEditorUI::class.java)