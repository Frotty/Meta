package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.info
import de.fatox.meta.api.extensions.onChange
import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.showDialog
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.dialogs.OpenProjectDialog
import de.fatox.meta.ui.dialogs.ProjectWizardDialog
import de.fatox.meta.ui.dialogs.SceneWizardDialog
import de.fatox.meta.ui.components.MetaMenu
import de.fatox.meta.ui.components.MetaMenuBar
import de.fatox.meta.ui.components.MetaMenuItem
import de.fatox.meta.ui.components.MetaSeparator
import de.fatox.meta.ui.windows.MetaConfirmDialog
import kotlin.reflect.KClass

private val log = MetaLoggerFactory.logger {}

class EditorMenuBar {
	private val languageBundle: LanguageBundle by lazyInject()
	private val projectManager: ProjectManager by lazyInject()
	private val uiManager: UIManager by lazyInject()

	val menuBar: MetaMenuBar
	private var windowsMenu: MetaMenu? = null

	init {

		this.menuBar = MetaMenuBar()
		log.info { "Created MenuBar" }
		val fileMenu = createFileMenu()
		menuBar.addMenu(fileMenu)
		menuBar.addMenu(createWindowsMenu())
		log.info { "Added File Menu" }
		menuBar.table.add().growX()
		menuBar.table.row().height(1f).left()
		menuBar.table.add(MetaSeparator()).colspan(2).left().growX()
	}

	fun clear() {
		windowsMenu!!.clear()
	}

	fun addAvailableWindow(windowClass: KClass<out Window>, icon: String = windowIcon(windowClass)) {
		val menuItem = MetaMenuItem(windowTitle(windowClass), icon)
		menuItem.onChange { uiManager.showWindow(windowClass) }
		windowsMenu!!.addItem(menuItem)
	}

	private fun windowTitle(windowClass: KClass<out Window>): String {
		return windowClass.simpleName!!.substringBefore("Window")
	}

	private fun windowIcon(windowClass: KClass<out Window>): String {
		return when (windowTitle(windowClass)) {
			"AssetDiscoverer" -> "ri-search-2-line"
			"ShaderLibrary" -> "ri-book-open-line"
			"ShaderComposer" -> "ri-node-tree"
			"SceneOptions" -> "ri-settings-3-line"
			"Primitives" -> "ri-box-3-line"
			else -> "ri-window-line"
		}
	}

	private fun createFileMenu(): MetaMenu {
		val fileMenu = MetaMenu(languageBundle["filemenu.title"])
		val menuItemNewProject = MetaMenuItem(languageBundle["filemenu.new.project"], "ri-file-add-line")
		menuItemNewProject.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				uiManager.showDialog<ProjectWizardDialog>()
			}
		})
		fileMenu.addItem(menuItemNewProject)
		val menuItemNewScene = MetaMenuItem(languageBundle["filemenu.new.scene"], "ri-file-add-line")
		menuItemNewScene.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				if (projectManager.hasCurrentProject) {
					uiManager.showDialog<SceneWizardDialog>()
				} else {
					MetaConfirmDialog("Project required", "Please open a project first").show(fileMenu.stage)
				}
			}
		})
		fileMenu.addItem(menuItemNewScene)
		val menuItemOpen = MetaMenuItem(languageBundle["filemenu.open"], "ri-folder-open-line")
		menuItemOpen.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				uiManager.showDialog<OpenProjectDialog>()
			}
		})
		fileMenu.addItem(menuItemOpen)
		fileMenu.width = 200f
		return fileMenu
	}

	private fun createWindowsMenu(): MetaMenu {
		windowsMenu = MetaMenu(languageBundle["windowsmenu.title"])
		return windowsMenu as MetaMenu
	}


}
