package de.fatox.meta.ui

import com.badlogic.gdx.Screen
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.TimeUtils
import com.kotcrab.vis.ui.widget.MenuBar
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import de.fatox.meta.ScreenConfig
import de.fatox.meta.api.DummyPosModifier
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.PosModifier
import de.fatox.meta.api.model.MetaWindowData
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.api.ui.WindowConfig
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.windows.MetaDialog
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.reflect.KClass

/**
 * Created by Frotty on 20.05.2016.
 */
object MetaUiManager : UIManager {
	private val uiRenderer: UIRenderer by lazyInject()
	private val metaData: MetaData by lazyInject()
	private val metaInput: MetaInputProcessor by lazyInject()

	private val displayedWindows = Array<Window>()
	private val cachedWindows = Array<Window>()
	private var mainMenuBar: MenuBar? = null
	private val contentTable = Table()
	private var currentScreenId: String = "(none)"
	override var posModifier: PosModifier = DummyPosModifier


	override val windowConfig: WindowConfig by lazyInject()
	override val screenConfig: ScreenConfig by lazyInject()

	override fun moveWindow(x: Int, y: Int) {
		posModifier.modify(x, y)
	}

	override fun resize(width: Int, height: Int) {
		uiRenderer.resize(width, height)
	}

	override fun <T: Screen> changeScreen(screenClass: KClass<T>) {
		changeScreen(screenConfig.classToName[screenClass]!!)
	}

	override fun <T: Tab> changeTab(tabClass: KClass<T>) {
		changeScreen(tabClass.qualifiedName!!)
	}

	private fun changeScreen(screenId: String) {
		currentScreenId = screenId
		metaInput.changeScreen()

		// Close or move currently shown windows
		for (window: Window in displayedWindows) {
			val name = windowConfig.classToName[window::class]!!
			if (metaHas(name)) {
				val metaWindowData = metaGet(name, MetaWindowData::class.java)!!
				if (metaWindowData.displayed) {
					// There exists saved window metadata
					metaWindowData.set(window)
				} else {
					cacheWindow(window, true)
				}
			} else {
				cacheWindow(window, true)
			}
		}
		displayedWindows.removeAll(cachedWindows, true)
		contentTable.remove()
		contentTable.clear()
		if (mainMenuBar != null) {
			contentTable.row().height(26f)
			contentTable.add(mainMenuBar!!.table).growX().top()
			mainMenuBar!!.table.toFront()
		}
		uiRenderer.addActor(contentTable)
		restoreWindows()
	}

	private fun restoreWindows() {
		val list = metaData.getCachedHandle(currentScreenId).list()
		outer@ for (fh: FileHandle in list) {
			if (fh.name().endsWith("Window")) {
				val windowClass: KClass<out Window>? = windowConfig.nameToClass[fh.name()]
				if (windowClass != null) {
					val metaWindowData = metaGet(windowConfig.classToName[windowClass]!!, MetaWindowData::class.java)!!
					for (displayedWindow in displayedWindows) {
						if (displayedWindow!!.javaClass == windowClass) {
							if (!metaWindowData.displayed) {
								cacheWindow(displayedWindow, true)
							}
							continue@outer
						}
					}
					if (metaWindowData.displayed) {
						metaWindowData.set(showWindow(windowClass))
					}
				} else {
					log.debug("Window class not found: ${fh.name()}")
					fh.delete()
				}
			}
		}
	}

	override fun addTable(table: Table?, growX: Boolean, growY: Boolean) {
		contentTable.row()
		val add = contentTable.add(table)
		if (growX) add.growX()
		if (growY) add.growY()
		contentTable.invalidate()
	}

	/**
	 * Shows an instance of the given class on the current screen.
	 * If metadata exists for the window, it will be loaded.
	 *
	 * @param windowClass The window to show
	 */
	override fun <T : Window> showWindow(windowClass: KClass<out T>): T {
		log.debug("show window: " + windowConfig.classToName[windowClass])
		val window: T? = displayWindow(windowClass)
		window!!.isVisible = true
		if (metaHas(windowConfig.classToName[windowClass]!!)) {
			// There exists metadata for this window.
			val windowData = metaGet(windowConfig.classToName[windowClass]!!, MetaWindowData::class.java)!!
			windowData.set(window)
			if (!windowData.displayed) {
				windowData.displayed = true
				metaSave(windowConfig.classToName[windowClass]!!, windowData)
			}
		} else {
			// First time the window has been shown on this screen
			metaSave(windowConfig.classToName[windowClass]!!, MetaWindowData(window))
		}
		return window
	}

	override fun <T : MetaDialog> showDialog(dialogClass: KClass<out T>): T {
		log.debug("show dialog: " + windowConfig.classToName[dialogClass]!!)
		// Dialogs are just Window subtypes so we show it as usual
		val dialog: T = showWindow(dialogClass)
		dialog.show()
		return dialog
	}

	override fun setMainMenuBar(menuBar: MenuBar?) {
		if (menuBar != null) {
			contentTable.row().height(26f)
			contentTable.add(menuBar.table).growX().top()
		} else if (mainMenuBar != null) {
			contentTable.removeActor(mainMenuBar!!.table)
		}
		mainMenuBar = menuBar
	}

	override fun <T : Window> getWindow(windowClass: KClass<out T>): T {
		// TODO avoid NPE
		return getDisplayedClass(windowClass)!!
	}

	override fun closeWindow(window: Window) {
		val displayedWindow = getDisplayedInstance(window)
		if (displayedWindow != null) {
			displayedWindows.removeValue(window, true)
			val metaWindowData = metaGet(windowConfig.classToName[window::class]!!, MetaWindowData::class.java)
			if (metaWindowData != null) {
				metaWindowData.displayed = false
				metaSave(windowConfig.classToName[displayedWindow::class]!!, metaWindowData)
			}
			cacheWindow(window, false)
		}
	}

	override fun updateWindow(window: Window) {
		val name = windowConfig.classToName[window::class]!!
		if (metaHas(name)) {
			val metaWindowData = metaGet(name, MetaWindowData::class.java)!!
			metaWindowData.setFrom(window)
			metaSave(name, metaWindowData)
		}
	}

	override fun bringWindowsToFront() {
		for (window in displayedWindows) {
			window!!.toFront()
		}
		mainMenuBar!!.table.toFront()
	}

	private fun <T : Window> displayWindow(windowClass: KClass<out T>): T? {
		// Check if this window is a singleton. If it is and it is displayed, return displayed instance
		// Check for a cached instance
		var theWindow: T? = null
		// If there was no cached instance we create a new one
		if (theWindow != null) {
			cachedWindows.removeValue(theWindow, true)
		} else {
			log.debug("try instance")
			theWindow = windowConfig.creators[windowConfig.classToName[windowClass]]!!.invoke() as T
		}
		uiRenderer.addActor(theWindow)
		displayedWindows.add(theWindow)
		return theWindow
	}

	private fun <T : Window> getDisplayedClass(windowClass: KClass<out T>): T? {
		return displayedWindows.firstOrNull { it::class == windowClass } as T?
	}

	private fun getDisplayedInstance(window: Window): Window? {
		return displayedWindows.firstOrNull { it == window }
	}

	private fun cacheWindow(window: Window?, forceClose: Boolean) {
		cachedWindows.add(window)
		window!!.isVisible = false
		if (forceClose) {
			window.remove()
		}
	}

	override fun metaHas(name: String): Boolean {
		return metaData.has(currentScreenId + File.separator + name)
	}

	override fun <T> metaGet(name: String, c: Class<T>): T? {
		return metaData[currentScreenId + File.separator + name, c]
	}

	override fun metaSave(name: String, windowData: Any) {
		val id = currentScreenId + File.separator + name
		if (TimeUtils.timeSinceMillis(metaData.getCachedHandle(id).lastModified()) > 200) {
			metaData.save(id, windowData)
		}
	}

	private val copy = Array<Window>()
	override val currentlyActiveWindows: Array<Window>
		get() {
			copy.clear()
			copy.addAll(displayedWindows)
			return copy
		}

	init {
		contentTable.apply {
			top().left()
			setPosition(0f, 0f)
			setFillParent(true)
		}
		uiRenderer.addActor(contentTable)
	}
}

private val log: Logger = LoggerFactory.getLogger(MetaUiManager::class.java)