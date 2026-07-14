package de.fatox.meta.ui

import com.badlogic.gdx.Screen
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.TimeUtils
import com.badlogic.gdx.utils.Timer
import de.fatox.meta.ScreenConfig
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.NoWindowHandler
import de.fatox.meta.api.WindowHandler
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.api.extensions.warn
import de.fatox.meta.api.model.MetaWindowData
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.api.ui.WindowConfig
import de.fatox.meta.api.ui.metaGet
import de.fatox.meta.assets.MetaData
import de.fatox.meta.assets.MetaDataKey
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.reactive.Disposable
import de.fatox.meta.reactive.ReactiveValue
import de.fatox.meta.reactive.effect
import de.fatox.meta.reactive.signal
import de.fatox.meta.reactive.subscribe
import de.fatox.meta.ui.components.MetaMenuBar
import de.fatox.meta.ui.components.MetaTooltip
import de.fatox.meta.ui.tabs.MetaTab
import de.fatox.meta.ui.windows.MetaDialog
import java.io.File
import kotlin.reflect.KClass

private val log = MetaLoggerFactory.logger {}

internal fun contextualBottomOverlay(screenOverlay: Actor?, hasModal: Boolean, modalOverlay: Actor?): Actor? =
	if (hasModal) modalOverlay else screenOverlay

/**
 * Created by Frotty on 20.05.2016.
 */
class MetaUiManager : UIManager {
	private val uiRenderer: UIRenderer by lazyInject()
	private val metaData: MetaData by lazyInject()
	private val metaInput: MetaInputProcessor by lazyInject()
	private val assetProvider: AssetProvider by lazyInject()

	private val displayedWindows = Array<Window>()
	private var mainMenuBar: MetaMenuBar? = null
	private val contentTable = Table()
	private val bottomOverlayLayer = Table().apply {
		setFillParent(true)
		bottom()
		touchable = Touchable.disabled
	}
	private var screenBottomOverlay: Actor? = null
	private var displayedBottomOverlay: Actor? = null
	private var currentScreenId: String = "(none)"
	private val screenMetaDataKeys = mutableMapOf<String, MetaDataKey<*>>()
	private val whitePixelTexture = Pixmap(1, 1, Pixmap.Format.RGBA8888).let { pixmap ->
		pixmap.setColor(Color.WHITE)
		pixmap.fill()
		Texture(pixmap).also { pixmap.dispose() }
	}
	private val whitePixel = TextureRegionDrawable(whitePixelTexture)
	private val backdrop = com.badlogic.gdx.scenes.scene2d.ui.Image(ColorDrawable(whitePixel, Color.valueOf("1F2025BB"))).apply {
		// Swallow input so the backdrop is a real modal shield: clicks on it never reach the UI beneath.
		addListener { true }
	}

	/**
	 * The stack of currently-shown modal dialogs that requested a backdrop, top-most last. This is the single source
	 * of truth for the shared backdrop: [backdropEffect] reacts to it, so the backdrop's presence and z-position are
	 * always a pure function of which dialogs are actually on screen - it can never be orphaned by a dialog that was
	 * disposed through an unusual path. [MetaDialog.setStage] keeps it accurate via [onDialogRemoved].
	 */
	private val modalDialogs = ArrayList<MetaDialog>()
	private val modalRevision = signal(0)
	private var topDialog: MetaDialog? = null

	/**
	 * Keeps the shared backdrop directly beneath the top-most modal dialog (or removed when none remain), and hands
	 * keyboard/scroll focus to whichever dialog is now on top. Re-runs automatically whenever [modalRevision] bumps.
	 */
	private val backdropEffect: Disposable = effect {
		modalRevision() // subscribe
		// Only a dialog that is actually on stage AND visible should hold the input-blocking backdrop up. A dialog
		// being hidden (isVisible=false during a close fade, or hidden by other windows) must not keep the shield
		// over whatever is beneath it.
		val top = modalDialogs.lastOrNull { it.stage != null && it.isVisible }
		val topStage = top?.stage
		if (top != null && topStage != null) {
			top.toFront()
			backdrop.setSize(topStage.width, topStage.height) // UI units (stage world), not physical pixels
			backdrop.remove()
			topStage.root.addActorBefore(top, backdrop)
			updateBottomOverlay(contextualBottomOverlay(screenBottomOverlay, hasModal = true, top.bottomOverlay))
			if (displayedBottomOverlay != null) {
				bottomOverlayLayer.remove()
				topStage.root.addActorBefore(top, bottomOverlayLayer)
			}
		} else {
			backdrop.remove()
			updateBottomOverlay(contextualBottomOverlay(screenBottomOverlay, hasModal = false, modalOverlay = null))
			if (displayedBottomOverlay != null) {
				if (bottomOverlayLayer.stage == null) uiRenderer.addActor(bottomOverlayLayer)
				bottomOverlayLayer.toFront()
			}
		}
		// Refocus only when the top dialog actually changes, so we never steal focus from a field the user clicked.
		if (top !== topDialog) {
			topDialog = top
			top?.focusDialog()
		}
		// Dialogs were just (re)fronted; keep toasts above them.
		uiRenderer.getToastManager().toFront()
		MetaTooltip.bringVisibleToFront()
	}
	private val preventShowWindowSignal = signal(false)
	override val preventShowWindowState: ReactiveValue<Boolean> get() = preventShowWindowSignal
	override var preventShowWindow: Boolean
		get() = preventShowWindowSignal.value
		private set(value) {
			preventShowWindowSignal.value = value
		}

	@Deprecated("Bind to preventShowWindowState instead, e.g. actor.bindDisabled { uiManager.preventShowWindowState() }.")
	override val preventShowWindowObservers: Array<(Boolean) -> Unit> = Array()
	private val hiddenWindows = Array<Window>()

	override var windowHandler: WindowHandler = NoWindowHandler

	override val windowConfig: WindowConfig by lazyInject()

	override val screenConfig: ScreenConfig by lazyInject()

	override val uiWidth: Float get() = uiRenderer.uiWidth
	override val uiHeight: Float get() = uiRenderer.uiHeight

	override fun setBottomOverlay(overlay: Actor?) {
		if (screenBottomOverlay === overlay) return
		screenBottomOverlay = overlay
		modalRevision.update { it + 1 }
	}

	override fun onDialogBottomOverlayChanged(dialog: MetaDialog) {
		if (modalDialogs.contains(dialog)) modalRevision.update { it + 1 }
	}

	private fun updateBottomOverlay(overlay: Actor?) {
		if (displayedBottomOverlay === overlay && (overlay == null || overlay.parent === bottomOverlayLayer)) return
		displayedBottomOverlay?.remove()
		bottomOverlayLayer.clearChildren()
		displayedBottomOverlay = overlay
		if (overlay == null) {
			bottomOverlayLayer.remove()
			return
		}
		overlay.remove()
		bottomOverlayLayer.add(overlay).bottom()
	}

	override fun moveWindow(x: Int, y: Int) {
		windowHandler.modify(x, y)
	}

	override fun resize(width: Int, height: Int) {
		uiRenderer.resize(width, height)
		if (backdrop.hasParent()) backdrop.setSize(uiRenderer.uiWidth, uiRenderer.uiHeight)
		for(i in 0 until displayedWindows.size) {
			val window = displayedWindows[i]
			if (window is MetaDialog) {
				window.centerWindow()
			} else {
				val name = windowConfig.nameOf(window::class)
				if (metaHas(name)) metaGet<MetaWindowData>(name)?.set(window, uiRenderer.uiWidth, uiRenderer.uiHeight)
			}
		}
	}

	override fun <T : Screen> changeScreen(screenClass: KClass<T>) {
		changeScreen(screenConfig.classToName[screenClass]!!)
	}

	override fun <T : MetaTab> changeTab(tabClass: KClass<T>) {
		changeScreen(tabClass.qualifiedName!!)
	}

	private fun changeScreen(screenId: String) {
		log.debug { "Change screen to: $screenId" }

		currentScreenId = screenId
		setBottomOverlay(null)
		screenMetaDataKeys.clear()
		metaInput.changeScreen()

		if (preventShowWindow) restoreOtherWindowsAndAllowNew()

		// Close or move currently shown windows. A window whose metadata says it is displayed on the new screen
		// survives the change: it stays on stage AND stays registered in displayedWindows, so restoreWindows will
		// recognize it instead of creating a duplicate instance. Everything else is detached (letting the window's
		// stage-driven lifecycle dispose its reactive scope) and forgotten.
		for (i in displayedWindows.size - 1 downTo 0) {
			val window = displayedWindows[i]
			val name = windowConfig.nameOf(window::class)
			val metaWindowData = if (metaHas(name)) metaGet<MetaWindowData>(name) else null
			if (metaWindowData != null && metaWindowData.displayed) {
				// There exists saved window metadata for the new screen; keep the instance and reposition it.
				metaWindowData.set(window, uiRenderer.uiWidth, uiRenderer.uiHeight)
			} else {
				window.remove()
				displayedWindows.removeIndex(i)
			}
		}
		contentTable.remove()
		contentTable.clear()
		mainMenuBar = null
		uiRenderer.addActor(contentTable)
		restoreWindows()
	}

	private fun restoreWindows() {
		val list = metaData.getCachedHandle(MetaDataKey<Any>(currentScreenId)).list()
		outer@ for (fh: FileHandle in list) {
			if (fh.name().endsWith("Window")) {
				val windowClass: KClass<out Window>? = windowConfig.nameToClass[fh.name()]
				if (windowClass != null) {
					val metaWindowData = metaGet(windowConfig.nameOf(windowClass), MetaWindowData::class)
					for (displayedWindow in displayedWindows) {
						if (displayedWindow.javaClass == windowClass) {
							if (!metaWindowData.displayed) {
								displayedWindow.remove()
								displayedWindows.removeValue(displayedWindow, true)
							}
							continue@outer
						}
					}
					if (metaWindowData.displayed) {
						metaWindowData.set(showWindow(windowClass), uiRenderer.uiWidth, uiRenderer.uiHeight)
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
		// Only (re)trigger MetaWindow's fade-in when actually becoming visible, so re-showing an
		// already-displayed window (e.g. clicking its menu entry twice) doesn't replay the fade and flicker.
		if (!isVisible) isVisible = true
		if (displayedWindows.contains(this, true)) return this

		uiRenderer.addActor(this)
		displayedWindows.add(this)
		val configName = windowConfig.nameOf(this::class)
		if (metaHas(configName)) {
			// There exists metadata for this window.
			val windowData: MetaWindowData = metaGet(configName)!!
			windowData.set(this, uiRenderer.uiWidth, uiRenderer.uiHeight)
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
		// Visibility changed outside show/remove: re-derive the backdrop, or it would stay up (swallowing input)
		// over a now-invisible modal dialog.
		modalRevision.update { it + 1 }
	}

	override fun restoreOtherWindowsAndAllowNew() {
		if (preventShowWindow) {
			preventShowWindow = false
			hiddenWindows.forEach { it.isVisible = true }
			hiddenWindows.clear()
			// Visibility changed outside show/remove: re-derive the backdrop for any re-shown modal dialog.
			modalRevision.update { it + 1 }
		}
	}

	override fun <T : MetaDialog> showDialog(dialogClass: KClass<out T>, showBackdrop: Boolean): T {
		log.debug { "Show dialog: ${windowConfig.nameOf(dialogClass)}" }
		// Dialogs are just Window subtypes, so we show it as usual.
		return showWindow(dialogClass).apply {
			if (!preventShowWindow) {
				// A press/drag that began behind the modal must not complete through it after the dialog appears.
				uiRenderer.cancelTouchFocus()
				// Defense-in-depth for the input contract: a fresh modal should start with NO exclusive grab active.
				// If one is still set here it means a previous owner (e.g. a key-rebind dialog) leaked it without
				// popping - a real bug that would otherwise make this dialog's buttons silently dead. Surface it
				// loudly and reset, rather than hiding it. With disciplined push/pop (pop in onHidden) this never fires.
				if (metaInput.exclusiveProcessor != null) {
					log.warn {
						"Showing dialog ${windowConfig.nameOf(dialogClass)} while an exclusive input processor is still " +
							"active (${metaInput.exclusiveProcessor}). The previous owner leaked it - it should pop on " +
							"teardown (MetaDialog.onHidden). Clearing to keep input alive."
					}
					metaInput.clearExclusiveProcessors()
				}
				show()
				if (showBackdrop) {
					// Register as the new top-most modal; the backdropEffect positions the shared backdrop just
					// beneath it and gives it focus. Re-add (remove first) so re-showing pushes it back to the top.
					modalDialogs.remove(this)
					modalDialogs.add(this)
					modalRevision.update { it + 1 }
				} else {
					toFront()
				}
			}
		}
	}

	override fun setMainMenuBar(menuBar: MetaMenuBar?) {
		if (mainMenuBar === menuBar && menuBar?.table?.parent === contentTable) return
		mainMenuBar?.table?.let { table ->
			val cell = contentTable.getCell(table)
			if (cell != null) {
				cell.clearActor()
				contentTable.cells.removeValue(cell, true)
			}
			table.remove()
		}
		mainMenuBar = menuBar
		if (menuBar != null) {
			contentTable.row().height(MENU_BAR_HEIGHT)
			contentTable.add(menuBar.table).growX().top()
			menuBar.table.toFront()
		}
		contentTable.invalidate()
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
			// Detach from the stage so the window's stage-driven lifecycle runs (MetaWindow disposes its reactive
			// scope on setStage(null)). Closed windows are not pooled: singletons are cached by WindowConfig and
			// creator-registered windows are constructed fresh, so the instance can simply be dropped/GC'd here.
			window.remove()
		}
	}

	override fun updateWindow(window: Window) {
		val name = windowConfig.nameOf(window::class)
		if (metaHas(name)) {
			val metaWindowData = metaGet<MetaWindowData>(name)!!
			metaWindowData.setFrom(window, uiRenderer.uiWidth, uiRenderer.uiHeight)
			metaSave(name, metaWindowData)
		}
	}

	override fun bringWindowsToFront() {
		for (window in displayedWindows) {
			window.toFront()
		}
		mainMenuBar?.table?.toFront()
		modalRevision.update { it + 1 }
		uiRenderer.getToastManager().toFront()
		MetaTooltip.bringVisibleToFront()
	}

	override fun metaHas(name: String): Boolean {
		return metaData.has(screenMetaKey<Any>(name))
	}

	override fun <T : Any> metaGet(name: String, c: KClass<out T>): T {
		return metaData.get(screenMetaKey(name), c)
	}

	// Trailing-edge companion to the metaSave throttle: latest throttled value per key, flushed shortly after.
	private val pendingSaves = ObjectMap<MetaDataKey<Any>, Any>()
	private val flushPendingSavesTask = object : Timer.Task() {
		override fun run() = flushPendingSaves()
	}

	override fun metaSave(name: String, windowData: Any) {
		val key = screenMetaKey<Any>(name)
		if (TimeUtils.timeSinceMillis(metaData.getCachedHandle(key).lastModified()) > SAVE_THROTTLE_MILLIS) {
			metaData.save(key, windowData)
			// A newer value just hit the disk; a stale pending value must not overwrite it later.
			pendingSaves.remove(key)
		} else {
			// Throttled (leading edge). Remember the LATEST value and schedule a trailing flush, so the final state
			// (e.g. displayed=false right after a drag-save) is guaranteed to be persisted.
			pendingSaves.put(key, windowData)
			if (!flushPendingSavesTask.isScheduled) {
				Timer.schedule(flushPendingSavesTask, SAVE_THROTTLE_MILLIS / 1000f)
			}
		}
	}

	private fun flushPendingSaves() {
		if (pendingSaves.isEmpty) return
		for (entry in pendingSaves.entries()) {
			metaData.save(entry.key, entry.value)
		}
		pendingSaves.clear()
	}

	override fun showToast(message: String, duration: Float): Unit = uiRenderer.getToastManager().show(message, duration)

	override fun showToast(table: Table, duration: Float): Unit = uiRenderer.getToastManager().show(table, duration)

	override fun clearToasts(): Unit = uiRenderer.getToastManager().clear()

	override fun onDialogRemoved(dialog: MetaDialog) {
		// Called by MetaDialog the instant it leaves the stage, by ANY path. Drop it from the modal stack and let
		// backdropEffect re-derive the backdrop's position/presence and move focus to the next dialog down (if any).
		if (modalDialogs.remove(dialog)) {
			modalRevision.update { it + 1 }
		}
	}

	private val copy = Array<Window>()
	override val currentlyActiveWindows: Array<Window>
		get() {
			copy.clear()
			copy.addAll(displayedWindows)
			return copy
		}

	@Suppress("UNCHECKED_CAST")
	private fun <T : Any> screenMetaKey(name: String): MetaDataKey<T> {
		val id = currentScreenId + File.separator + name
		return screenMetaDataKeys.getOrPut(id) { MetaDataKey<Any>(id) } as MetaDataKey<T>
	}

	private companion object {
		const val MENU_BAR_HEIGHT = 34f
		const val SAVE_THROTTLE_MILLIS = 200L
	}

	override fun dispose() {
		backdropEffect.dispose()
		flushPendingSavesTask.cancel()
		flushPendingSaves() // don't lose a trailing save that was still in flight
		for (window in displayedWindows) {
			window.remove()
		}
		displayedWindows.clear()
		windowConfig.disposeCachedWindows()
		hiddenWindows.clear()
		modalDialogs.clear()
		screenBottomOverlay = null
		displayedBottomOverlay = null
		bottomOverlayLayer.clearChildren()
		bottomOverlayLayer.remove()
		topDialog = null
		backdrop.remove()
		contentTable.remove()
		uiRenderer.dispose()
		whitePixelTexture.dispose()
	}

	init {
		contentTable.apply {
			top().left()
			setPosition(0f, 0f)
			setFillParent(true)
		}
		uiRenderer.addActor(contentTable)

		// Legacy bridge: drive the deprecated preventShowWindowObservers from the signal (fires on change only),
		// so old observer-list consumers keep working while new code binds to preventShowWindowState.
		@Suppress("DEPRECATION")
		preventShowWindowSignal.subscribe {
			val value = preventShowWindowSignal.peek()
			for (i in 0 until preventShowWindowObservers.size) preventShowWindowObservers[i](value)
		}
	}
}
