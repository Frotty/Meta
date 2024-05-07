package de.fatox.meta.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.TimeUtils
import com.kotcrab.vis.ui.widget.MenuBar
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import de.fatox.meta.ScreenConfig
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.NoWindowHandler
import de.fatox.meta.api.WindowHandler
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.api.model.MetaWindowData
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.api.ui.WindowConfig
import de.fatox.meta.api.ui.metaGet
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.windows.MetaDialog
import java.io.File
import kotlin.properties.Delegates
import kotlin.reflect.KClass

private val log = MetaLoggerFactory.logger {}

/**
 * Created by Frotty on 20.05.2016.
 */
class MetaUiManager : UIManager {
	private val uiRenderer: UIRenderer by lazyInject()
	private val metaData: MetaData by lazyInject()
	private val metaInput: MetaInputProcessor by lazyInject()
	private val assetProvider: AssetProvider by lazyInject()

	private val displayedWindows = Array<Window>()
	private val cachedWindows = Array<Window>()
	private var mainMenuBar: MenuBar? = null
	private val contentTable = Table()
	private var currentScreenId: String = "(none)"
	private val whitePixel = TextureRegionDrawable(Texture(Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
		setColor(Color.WHITE)
		fill()
	}))
	private val backdrop = VisImage(ColorDrawable(whitePixel, Color.valueOf("1F2025BB"))).apply {
		addListener {
			false
		}
	}
	override var preventShowWindow: Boolean by Delegates.observable(false) { _, _, newValue ->
		preventShowWindowObservers.forEach { it(newValue) }
	}
		private set
	override val preventShowWindowObservers: Array<(Boolean) -> Unit> = Array()
	private val hiddenWindows = Array<Window>()

	override var windowHandler: WindowHandler = NoWindowHandler

	override val windowConfig: WindowConfig by lazyInject()

	override val screenConfig: ScreenConfig by lazyInject()

	override fun moveWindow(x: Int, y: Int) {
		windowHandler.modify(x, y)
	}

	override fun resize(width: Int, height: Int) {
		uiRenderer.resize(width, height)
	}

	override fun <T : Screen> changeScreen(screenClass: KClass<T>) {
		changeScreen(screenConfig.classToName[screenClass]!!)
	}

	override fun <T : Tab> changeTab(tabClass: KClass<T>) {
		changeScreen(tabClass.qualifiedName!!)
	}

	private fun changeScreen(screenId: String) {
		log.debug { "Change screen to: $screenId" }

		currentScreenId = screenId
		metaInput.changeScreen()

		if (preventShowWindow) restoreOtherWindowsAndAllowNew()

		// Close or move currently shown windows
		for (window: Window in displayedWindows) {
			val name = windowConfig.nameOf(window::class)
			if (metaHas(name)) {
				val metaWindowData = metaGet<MetaWindowData>(name)!!
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
		displayedWindows.clear()
		contentTable.remove()
		contentTable.clear()
		mainMenuBar = null
		uiRenderer.addActor(contentTable)
		restoreWindows()
	}

	private fun restoreWindows() {
		val list = metaData.getCachedHandle(currentScreenId).list() // TODO use MetaDataKey and cache it
		outer@ for (fh: FileHandle in list) {
			if (fh.name().endsWith("Window")) {
				val windowClass: KClass<out Window>? = windowConfig.nameToClass[fh.name()]
				if (windowClass != null) {
					val metaWindowData = metaGet(windowConfig.nameOf(windowClass), MetaWindowData::class)
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
					log.debug { "Window class not found: ${fh.name()}" }
					fh.delete()
				}
			}
		}
	}

	override fun addTable(table: Table?, growX: Boolean, growY: Boolean) {
		contentTable.row()
		contentTable.add(table).apply {
			if (growX) growX()
			if (growY) growY()
		}
		contentTable.invalidate()
	}

	/**
	 * Shows an instance of the given class on the current screen.
	 * If metadata exists for the window, it will be loaded.
	 *
	 * @param windowClass The window to show
	 */
	override fun <T : Window> showWindow(windowClass: KClass<out T>): T {
		log.debug { "Show window: ${windowConfig.nameOf(windowClass)}" }

		return windowConfig.create(windowClass).apply { if (!preventShowWindow) display() }
	}

	private fun <T : Window> T.display(): T {
		isVisible = true
		if (displayedWindows.contains(this, true)) return this

		uiRenderer.addActor(this)
		displayedWindows.add(this)
		val configName = windowConfig.nameOf(this::class)
		if (metaHas(configName)) {
			// There exists metadata for this window.
			val windowData: MetaWindowData = metaGet(configName)!!
			windowData.set(this)
			if (!windowData.displayed) {
				windowData.displayed = true
				metaSave(configName, windowData)
			}
		} else {
			// First time the window has been shown on this screen
			metaSave(configName, MetaWindowData(this))
		}
		return this
	}

	override fun hideOtherWindowsAndPreventNew(window: Window) {
		preventShowWindow = true
		displayedWindows.filterNot { it === window }.forEach {
			if (it.isVisible) {
				it.isVisible = false
				hiddenWindows.add(it)
			}
		}
	}

	override fun restoreOtherWindowsAndAllowNew() {
		if (preventShowWindow) {
			preventShowWindow = false
			hiddenWindows.forEach { it.isVisible = true }
			hiddenWindows.clear()
		}
	}

	override fun <T : MetaDialog> showDialog(dialogClass: KClass<out T>, showBackdrop: Boolean): T {
		log.debug { "Show dialog: ${windowConfig.nameOf(dialogClass)}" }
		// Dialogs are just Window subtypes, so we show it as usual
		if  (showBackdrop) {
			backdrop.apply {
				width = Gdx.graphics.width.toFloat()
				height = Gdx.graphics.height.toFloat()
			}
			uiRenderer.addActor(backdrop)
		}
		return showWindow(dialogClass).apply { if (!preventShowWindow) show() }
	}

	override fun setMainMenuBar(menuBar: MenuBar?) {
		if (menuBar != null) {
			contentTable.row().height(26f)
			contentTable.add(menuBar.table).growX().top()
		} else if (mainMenuBar != null) {
			val cell = contentTable.getCell(mainMenuBar!!.table)
			cell.clearActor()
			contentTable.cells.removeValue(cell, true)
			contentTable.invalidate()
		}
		mainMenuBar = menuBar
	}

	override fun <T : Window> getWindow(windowClass: KClass<out T>): T {
		return displayedWindows.firstOrNull { it::class == windowClass } as T?
			?: windowConfig.create(windowClass)
	}

	override fun closeWindow(window: Window) {
		displayedWindows.firstOrNull { it === window }?.let { displayedWindow ->
			displayedWindows.removeValue(window, true)
			val name = windowConfig.nameOf(displayedWindow::class)
			if (metaHas(name)) metaGet<MetaWindowData>(name)?.let {
				it.displayed = false
				metaSave(name, it)
			}
			cacheWindow(window, false)
		}
	}

	override fun updateWindow(window: Window) {
		val name = windowConfig.nameOf(window::class)
		if (metaHas(name)) {
			val metaWindowData = metaGet<MetaWindowData>(name)!!
			metaWindowData.setFrom(window)
			metaSave(name, metaWindowData)
		}
	}

	override fun bringWindowsToFront() {
		for (window in displayedWindows) {
			window!!.toFront()
		}
		mainMenuBar?.table?.toFront()
	}

	private fun cacheWindow(window: Window, forceClose: Boolean) {
		cachedWindows.add(window)
		window.isVisible = false
		if (forceClose) {
			window.remove()
		}
	}

	override fun metaHas(name: String): Boolean {
		return metaData.has(currentScreenId + File.separator + name) // TODO use MetaDataKey and cache it
	}

	override fun <T : Any> metaGet(name: String, c: KClass<out T>): T {
		return metaData[currentScreenId + File.separator + name, c] // TODO use MetaDataKey and cache it
	}

	override fun metaSave(name: String, windowData: Any) {
		val id = currentScreenId + File.separator + name // TODO use MetaDataKey and cache it
		if (TimeUtils.timeSinceMillis(metaData.getCachedHandle(id).lastModified()) > 200) {
			metaData.save(id, windowData)
		}
	}

	override fun closeDialog(metaDialog: MetaDialog) {
		backdrop.remove()
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