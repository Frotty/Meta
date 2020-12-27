package de.fatox.meta.ui

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.kotcrab.vis.ui.widget.Menu
import com.kotcrab.vis.ui.widget.MenuBar
import com.kotcrab.vis.ui.widget.MenuItem
import com.kotcrab.vis.ui.widget.Separator
import de.fatox.meta.api.AssetProvider
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
import de.fatox.meta.ui.windows.MetaConfirmDialog
import kotlin.reflect.KClass

private val log = MetaLoggerFactory.logger {}

class EditorMenuBar {
	private val languageBundle: LanguageBundle by lazyInject()
	private val assetProvider: AssetProvider by lazyInject()
	private val projectManager: ProjectManager by lazyInject()
	private val uiManager: UIManager by lazyInject()

	val menuBar: MenuBar
	private var windowsMenu: Menu? = null

	init {

		this.menuBar = MenuBar()
		log.info { "Created MenuBar" }
		val fileMenu = createFileMenu()
		menuBar.addMenu(fileMenu)
		menuBar.addMenu(createWindowsMenu())
		log.info { "Added File Menu" }
		menuBar.table.add().growX()
		menuBar.table.row().height(1f).left()
		menuBar.table.add(Separator()).colspan(2).left().growX()
	}

	fun clear() {
		windowsMenu!!.clear()
	}

	fun addAvailableWindow(windowClass: KClass<out Window>, icon: Image?) {
		val menuItem = MenuItem(windowClass.simpleName!!.substring(0, windowClass.simpleName!!.indexOf("Window")), icon)
		menuItem.onChange { uiManager.showWindow(windowClass) }
		windowsMenu!!.addItem(menuItem)
	}

	private fun createFileMenu(): Menu {
		val fileMenu = Menu(languageBundle["filemenu_title"])
		val menuItemNewProject = MenuItem(languageBundle["filemenu_new_proj"], Image(assetProvider.getResource("ui/appbar.new.png", Texture::class.java)))
		menuItemNewProject.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				uiManager.showDialog<ProjectWizardDialog>()
			}
		})
		fileMenu.addItem(menuItemNewProject)
		val menuItemNewScene = MenuItem(languageBundle["filemenu_new_scene"], Image(assetProvider.getResource("ui/appbar.new.png", Texture::class.java)))
		menuItemNewScene.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				if (projectManager.currentProject != null) {
					uiManager.showDialog<SceneWizardDialog>()
				} else {
					MetaConfirmDialog("Project required", "Please open a project first").show(fileMenu.stage)
				}
			}
		})
		fileMenu.addItem(menuItemNewScene)
		val menuItemOpen = MenuItem(languageBundle["filemenu_open"], Image(assetProvider.getResource("ui/appbar.folder.open.png", Texture::class.java)))
		menuItemOpen.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				uiManager.showDialog<OpenProjectDialog>()
			}
		})
		fileMenu.addItem(menuItemOpen)
		fileMenu.width = 200f
		return fileMenu
	}

	private fun createWindowsMenu(): Menu {
		windowsMenu = Menu(languageBundle["windowsmenu_title"])
		return windowsMenu as Menu
	}


}
