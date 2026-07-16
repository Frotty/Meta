package de.fatox.meta.ui

import com.badlogic.gdx.Screen
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Image
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
import de.fatox.meta.api.ui.MetaDockConfig
import de.fatox.meta.api.ui.MetaDockLayoutData
import de.fatox.meta.api.ui.MetaDockSide
import de.fatox.meta.api.ui.MetaWindowInteraction
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.api.ui.WindowConfig
import de.fatox.meta.api.ui.detectDockSide
import de.fatox.meta.api.ui.dockDividerBottom
import de.fatox.meta.api.ui.resolveDockUpdate
import de.fatox.meta.api.ui.resolveDockWidths
import de.fatox.meta.api.ui.resolveDockWidthForSide
import de.fatox.meta.api.ui.resizedLowerDockPanelHeight
import de.fatox.meta.api.ui.resizedDockPanelHeight
import de.fatox.meta.api.ui.shouldResizeLowerDockPanel
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
import de.fatox.meta.ui.windows.MetaWindow
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
	private var windowDockConfig: MetaDockConfig? = null
	private var leftDockWidth = 0f
	private var rightDockWidth = 0f
	private val dockEntryCache = ObjectMap<Window, DockedWindow>()
	private val dockDividerCache = ObjectMap<Window, DockDivider>()
	private val dockDividers = Array<DockDivider>()
	private var dockLayoutGeneration = 0
	private val leftDockedWindows = Array<DockedWindow>()
	private val rightDockedWindows = Array<DockedWindow>()
	private var leftDockHeights = FloatArray(0)
	private var rightDockHeights = FloatArray(0)
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
	private val dockPreview = Image(ColorDrawable(whitePixel, Color.valueOf("5D8DFF2E"))).apply {
		touchable = Touchable.disabled
	}
	private val leftDockHint = Image(ColorDrawable(whitePixel, Color.valueOf("5D8DFF99"))).apply {
		touchable = Touchable.disabled
	}
	private val rightDockHint = Image(ColorDrawable(whitePixel, Color.valueOf("5D8DFF99"))).apply {
		touchable = Touchable.disabled
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
		layoutDockedWindows()
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
		windowDockConfig = null
		previewWindowDock(null)
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
			val metaWindow = window as? MetaWindow
			val wasDocked = metaWindow?.dockSide != null
			metaWindow?.applyDockState(null)
			val name = windowConfig.nameOf(window::class)
			val metaWindowData = if (metaHas(name)) metaGet<MetaWindowData>(name) else null
			if (metaWindowData != null && metaWindowData.displayed) {
				// There exists saved window metadata for the new screen; keep the instance and reposition it.
				metaWindowData.set(
					window,
					uiRenderer.uiWidth,
					uiRenderer.uiHeight,
					restoreSize = window.isResizable || wasDocked,
				)
			} else {
				window.remove()
				displayedWindows.removeIndex(i)
				dockEntryCache.remove(window)
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
					for (displayedIndex in 0 until displayedWindows.size) {
						val displayedWindow = displayedWindows[displayedIndex]
						if (displayedWindow.javaClass == windowClass) {
							if (!metaWindowData.displayed) {
								displayedWindow.remove()
								displayedWindows.removeIndex(displayedIndex)
								dockEntryCache.remove(displayedWindow)
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
		layoutDockedWindows()
		return this
	}

	override fun hideOtherWindowsAndPreventNew(window: Window) {
		preventShowWindow = true
		for (index in 0 until displayedWindows.size) {
			val displayedWindow = displayedWindows[index]
			if (displayedWindow !== window && displayedWindow.isVisible) {
				displayedWindow.isVisible = false
				hiddenWindows.add(displayedWindow)
			}
		}
		// Visibility changed outside show/remove: re-derive the backdrop, or it would stay up (swallowing input)
		// over a now-invisible modal dialog.
		modalRevision.update { it + 1 }
	}

	override fun restoreOtherWindowsAndAllowNew() {
		if (preventShowWindow) {
			preventShowWindow = false
			for (index in 0 until hiddenWindows.size) hiddenWindows[index].isVisible = true
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
		previewWindowDock(null)
		(window as? MetaWindow)?.applyDockState(null)
		displayedWindows.firstOrNull { it === window }?.let { displayedWindow ->
			displayedWindows.removeValue(window, true)
			dockEntryCache.remove(window)
			removeDockDivider(window)
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
		layoutDockedWindows()
	}

	override fun updateWindow(window: Window) {
		updateWindow(window, MetaWindowInteraction.PROGRAMMATIC, finished = true)
	}

	override fun updateWindow(window: Window, interaction: MetaWindowInteraction) {
		updateWindow(window, interaction, finished = true)
	}

	override fun updateWindow(window: Window, interaction: MetaWindowInteraction, finished: Boolean) {
		val name = windowConfig.nameOf(window::class)
		if (metaHas(name)) {
			val metaWindowData = metaGet<MetaWindowData>(name)!!
			val dockConfig = windowDockConfig
			val currentSide = MetaDockSide.fromPersisted(metaWindowData.dockSide)
			if (!finished && currentSide != null && dockConfig != null) {
				if (interaction === MetaWindowInteraction.MOVE) {
					val detected = detectDockSide(window.x, window.width, uiRenderer.uiWidth, dockConfig.snapDistance)
					if (detected == currentSide) {
						metaWindowData.dockOrder = nextDockOrder(currentSide, window, persistNormalized = false)
						layoutDockedWindows(activeMovingWindow = window)
					}
				} else if (interaction === MetaWindowInteraction.RESIZE) {
					updateDockDivider(currentSide, window, metaWindowData, persist = false)
					layoutDockedWindows()
				} else if (interaction === MetaWindowInteraction.DOCK_WIDTH_RESIZE) {
					setDesiredDockWidth(currentSide, window.width)
					if (!metaWindowData.dockFill) metaWindowData.dockHeight = window.height
					layoutDockedWindows()
				}
				return
			}
			if (interaction === MetaWindowInteraction.RESIZE && currentSide != null && dockConfig != null) {
				updateDockDivider(currentSide, window, metaWindowData, persist = true)
				layoutDockedWindows()
				return
			}
			if (interaction == MetaWindowInteraction.DOCK_WIDTH_RESIZE && currentSide != null && dockConfig != null) {
				setDesiredDockWidth(currentSide, window.width)
				if (!metaWindowData.dockFill) metaWindowData.dockHeight = window.height
				metaSave(name, metaWindowData)
				persistDockWidths()
				layoutDockedWindows()
				return
			}
			val detectedSide = if (interaction == MetaWindowInteraction.MOVE && dockConfig != null && window !is MetaDialog) {
				detectDockSide(window.x, window.width, uiRenderer.uiWidth, dockConfig.snapDistance)
			} else null
			val update = resolveDockUpdate(currentSide, interaction, detectedSide)
			metaWindowData.dockSide = update.side?.persistedName.orEmpty()
			if (update.side == null) {
				metaWindowData.dockFill = false
				(window as? MetaWindow)?.applyDockState(null)
			} else if (interaction == MetaWindowInteraction.MOVE) {
				metaWindowData.dockOrder = nextDockOrder(update.side, window, persistNormalized = true)
			}
			if (update.updateDockHeight) metaWindowData.dockHeight = window.height
			if (update.updateFloatingBounds) {
				metaWindowData.setFrom(window, uiRenderer.uiWidth, uiRenderer.uiHeight)
			}
			metaSave(name, metaWindowData)
			if (interaction == MetaWindowInteraction.MOVE) {
				// A live insertion may have normalized the source side's sparse order values. Commit both affected sides,
				// including when the gesture finishes by undocking or crossing to the opposite sidebar.
				currentSide?.let(::persistDockSide)
				if (update.side != null && update.side != currentSide) persistDockSide(update.side)
			}
			layoutDockedWindows()
		}
	}

	override fun configureWindowDocking(config: MetaDockConfig?) {
		windowDockConfig = config
		if (config != null) {
			val saved = if (metaHas(DOCK_LAYOUT_DATA_KEY)) metaGet<MetaDockLayoutData>(DOCK_LAYOUT_DATA_KEY) else null
			leftDockWidth = saved?.leftWidth?.takeIf { it.isFinite() && it > 0f } ?: config.leftWidth
			rightDockWidth = saved?.rightWidth?.takeIf { it.isFinite() && it > 0f } ?: config.rightWidth
		}
		if (config == null) {
			previewWindowDock(null)
			removeAllDockDividers()
			for (index in 0 until displayedWindows.size) {
				(displayedWindows[index] as? MetaWindow)?.applyDockState(null)
			}
		}
		layoutDockedWindows()
	}

	override fun dockWindow(window: Window, side: MetaDockSide, order: Int, height: Float, fill: Boolean) {
		if (window is MetaDialog) return
		require(displayedWindows.contains(window, true)) { "Only a displayed window can be docked" }
		require(height.isFinite()) { "Dock height must be finite" }
		val name = windowConfig.nameOf(window::class)
		val data = if (metaHas(name)) metaGet<MetaWindowData>(name)!! else MetaWindowData(window)
		data.dockSide = side.persistedName
		data.dockOrder = order
		data.dockHeight = height.coerceAtLeast(window.minHeight)
		data.dockFill = fill
		metaSave(name, data)
		layoutDockedWindows()
	}

	override fun undockWindow(window: Window) {
		require(displayedWindows.contains(window, true)) { "Only a displayed window can be undocked" }
		val name = windowConfig.nameOf(window::class)
		if (!metaHas(name)) return
		val data = metaGet<MetaWindowData>(name)!!
		data.set(window, uiRenderer.uiWidth, uiRenderer.uiHeight, restoreSize = true)
		data.dockSide = ""
		data.dockFill = false
		data.setFrom(window, uiRenderer.uiWidth, uiRenderer.uiHeight)
		(window as? MetaWindow)?.applyDockState(null)
		metaSave(name, data)
		layoutDockedWindows()
	}

	override fun previewWindowDock(window: Window?) {
		val config = windowDockConfig
		if (window == null || config == null || window is MetaDialog) {
			clearDockIndicators()
			return
		}
		val side = detectDockSide(window.x, window.width, uiRenderer.uiWidth, config.snapDistance)
		if (side == null) {
			dockPreview.remove()
			showDockHints(config, window)
			return
		}
		leftDockHint.remove()
		rightDockHint.remove()
		val leftMinimum = previewSideMinimum(MetaDockSide.LEFT, side, window)
		val rightMinimum = previewSideMinimum(MetaDockSide.RIGHT, side, window)
		val width = resolveDockWidthForSide(
			uiRenderer.uiWidth, config, side, leftMinimum, rightMinimum, leftDockWidth, rightDockWidth,
		)
		if (width == null) {
			clearDockIndicators()
			return
		}
		val x = if (side == MetaDockSide.LEFT) config.margin else uiRenderer.uiWidth - config.margin - width
		dockPreview.setBounds(
			x,
			config.bottomInset,
			width,
			(uiRenderer.uiHeight - config.topInset - config.bottomInset).coerceAtLeast(0f),
		)
		if (dockPreview.stage == null) uiRenderer.addActor(dockPreview)
		dockPreview.toFront()
		window.toFront()
	}

	private fun showDockHints(config: MetaDockConfig, movingWindow: Window) {
		val availableHeight = (uiRenderer.uiHeight - config.topInset - config.bottomInset).coerceAtLeast(0f)
		val hintHeight = availableHeight.coerceAtMost(DOCK_HINT_HEIGHT)
		val hintY = config.bottomInset + (availableHeight - hintHeight) * 0.5f
		leftDockHint.setBounds(config.margin, hintY, DOCK_HINT_WIDTH, hintHeight)
		rightDockHint.setBounds(
			uiRenderer.uiWidth - config.margin - DOCK_HINT_WIDTH,
			hintY,
			DOCK_HINT_WIDTH,
			hintHeight,
		)
		if (leftDockHint.stage == null) uiRenderer.addActor(leftDockHint)
		if (rightDockHint.stage == null) uiRenderer.addActor(rightDockHint)
		leftDockHint.toFront()
		rightDockHint.toFront()
		movingWindow.toFront()
	}

	private fun clearDockIndicators() {
		dockPreview.remove()
		leftDockHint.remove()
		rightDockHint.remove()
	}

	private fun previewSideMinimum(side: MetaDockSide, targetSide: MetaDockSide, movingWindow: Window): Float? {
		val config = windowDockConfig ?: return null
		var minimum: Float? = if (side == targetSide) {
			movingWindow.minWidth.coerceAtLeast(config.minimumSidebarWidth)
		} else null
		for (index in 0 until displayedWindows.size) {
			val window = displayedWindows[index]
			if (window === movingWindow || window is MetaDialog) continue
			val name = windowConfig.nameOf(window::class)
			val data = if (metaHas(name)) metaGet<MetaWindowData>(name) else null
			if (data?.dockSide == side.persistedName) {
				val windowMinimum = window.minWidth.coerceAtLeast(config.minimumSidebarWidth)
				minimum = minimum?.coerceAtLeast(windowMinimum) ?: windowMinimum
			}
		}
		return minimum
	}

	private fun nextDockOrder(side: MetaDockSide, movingWindow: Window, persistNormalized: Boolean): Int {
		val movingAnchorY = (movingWindow as? MetaWindow)?.dockOrderAnchorY
			?.takeIf(Float::isFinite)
			?: movingWindow.y + movingWindow.height
		val ordered = if (side == MetaDockSide.LEFT) leftDockedWindows else rightDockedWindows
		collectDockedWindows(side, ordered, movingWindow)
		ordered.sort(DOCKED_WINDOW_COMPARATOR)
		var insertion = ordered.size
		for (index in 0 until ordered.size) {
			val window = ordered[index].window
			val thresholdY = (window as? MetaWindow)?.dockOrderThresholdY ?: window.y + window.height
			if (movingAnchorY > thresholdY) {
				insertion = index
				break
			}
		}
		if (ordered.size == 0) return 0
		if (insertion == 0) return ordered.first().data.dockOrder - DOCK_ORDER_STEP
		if (insertion == ordered.size) return ordered.peek().data.dockOrder + DOCK_ORDER_STEP
		val before = ordered[insertion - 1].data.dockOrder
		val after = ordered[insertion].data.dockOrder
		if (after - before > 1) return before + (after - before) / 2

		for (index in 0 until ordered.size) {
			val entry = ordered[index]
			entry.data.dockOrder = (if (index < insertion) index else index + 1) * DOCK_ORDER_STEP
			if (persistNormalized) metaSave(entry.name, entry.data)
		}
		return insertion * DOCK_ORDER_STEP
	}

	private fun persistDockSide(side: MetaDockSide) {
		for (index in 0 until displayedWindows.size) {
			val window = displayedWindows[index]
			if (window is MetaDialog) continue
			val name = windowConfig.nameOf(window::class)
			val data = if (metaHas(name)) metaGet<MetaWindowData>(name) else null
			if (data?.dockSide == side.persistedName) metaSave(name, data)
		}
	}

	private fun updateDockDivider(
		side: MetaDockSide,
		window: Window,
		data: MetaWindowData,
		persist: Boolean,
	) {
		if (!data.dockFill) {
			data.dockHeight = window.height.coerceAtLeast(dockPanelMinimumHeight(window))
			if (persist) metaSave(windowConfig.nameOf(window::class), data)
			return
		}

		val docked = if (side == MetaDockSide.LEFT) leftDockedWindows else rightDockedWindows
		collectDockedWindows(side, docked)
		docked.sort(DOCKED_WINDOW_COMPARATOR)
		var currentIndex = -1
		for (index in 0 until docked.size) {
			if (docked[index].window === window) {
				currentIndex = index
				break
			}
		}
		if (currentIndex < 0 || currentIndex >= docked.size - 1) return
		val lower = docked[currentIndex + 1]
		val config = windowDockConfig ?: return
		lower.data.dockHeight = resizedLowerDockPanelHeight(
			upperBottom = window.y,
			lowerTop = lower.window.y + lower.window.height,
			lowerHeight = lower.window.height,
			gap = config.gap,
			minimumHeight = dockPanelMinimumHeight(lower.window),
		)
		// Once the user sizes the lower side of a fill/fill divider, the lower panel becomes the fixed side and the
		// upper panel continues filling the remaining space.
		lower.data.dockFill = false
		if (persist) metaSave(lower.name, lower.data)
	}

	private fun setDesiredDockWidth(side: MetaDockSide, width: Float) {
		if (!width.isFinite()) return
		if (side == MetaDockSide.LEFT) leftDockWidth = width else rightDockWidth = width
	}

	private fun persistDockWidths() {
		metaSave(DOCK_LAYOUT_DATA_KEY, MetaDockLayoutData(leftDockWidth, rightDockWidth))
	}

	private fun collectDockedWindows(
		side: MetaDockSide,
		out: Array<DockedWindow>,
		excludedWindow: Window? = null,
	) {
		out.clear()
		for (index in 0 until displayedWindows.size) {
			val window = displayedWindows[index]
			if (window === excludedWindow || window is MetaDialog) continue
			val name = windowConfig.nameOf(window::class)
			val data = if (metaHas(name)) metaGet<MetaWindowData>(name) else null
			if (data != null && data.dockSide == side.persistedName) {
				var entry = dockEntryCache[window]
				if (entry == null) {
					entry = DockedWindow(window, name, data)
					dockEntryCache.put(window, entry)
				} else {
					entry.data = data
				}
				out.add(entry)
			}
		}
	}

	private fun layoutDockedWindows(activeMovingWindow: Window? = null) {
		val config = windowDockConfig ?: run {
			removeAllDockDividers()
			return
		}
		dockLayoutGeneration++
		collectDockedWindows(MetaDockSide.LEFT, leftDockedWindows)
		collectDockedWindows(MetaDockSide.RIGHT, rightDockedWindows)
		val leftMinimum = dockMinimum(config, leftDockedWindows)
		val rightMinimum = dockMinimum(config, rightDockedWindows)
		val widths = resolveDockWidths(
			uiRenderer.uiWidth,
			config,
			leftMinimum,
			rightMinimum,
			leftDockWidth,
			rightDockWidth,
		)
		widths.left?.let { leftDockWidth = it }
		widths.right?.let { rightDockWidth = it }
		leftDockHeights = layoutDockSide(
			MetaDockSide.LEFT, config, leftDockedWindows, widths.left, leftMinimum, leftDockHeights, activeMovingWindow,
		)
		rightDockHeights = layoutDockSide(
			MetaDockSide.RIGHT, config, rightDockedWindows, widths.right, rightMinimum, rightDockHeights,
			activeMovingWindow,
		)
		for (index in 0 until dockDividers.size) {
			val divider = dockDividers[index]
			if (divider.layoutGeneration != dockLayoutGeneration && !divider.dragging) divider.detach()
		}
	}

	private fun dockMinimum(config: MetaDockConfig, docked: Array<DockedWindow>): Float? {
		if (docked.size == 0) return null
		var minimum = config.minimumSidebarWidth
		for (index in 0 until docked.size) minimum = minimum.coerceAtLeast(docked[index].window.minWidth)
		return minimum
	}

	private fun layoutDockSide(
		side: MetaDockSide,
		config: MetaDockConfig,
		docked: Array<DockedWindow>,
		width: Float?,
		minimumWidth: Float?,
		heightBuffer: FloatArray,
		activeMovingWindow: Window?,
	): FloatArray {
		if (docked.size == 0 || width == null) return heightBuffer
		docked.sort(DOCKED_WINDOW_COMPARATOR)
		for (index in 0 until docked.size) {
			val window = docked[index].window
			if (window === activeMovingWindow) continue
			window.setWidth(width)
			window.invalidateHierarchy()
			window.validate()
		}

		val heights = if (heightBuffer.size >= docked.size) heightBuffer else FloatArray(docked.size)
		val availableHeight = (uiRenderer.uiHeight - config.topInset - config.bottomInset -
			config.gap * (docked.size - 1)).coerceAtLeast(0f)
		var minimumTotal = 0f
		for (index in 0 until docked.size) {
			heights[index] = dockPanelMinimumHeight(docked[index].window)
			minimumTotal += heights[index]
		}
		if (minimumTotal > availableHeight && minimumTotal > 0f) {
			val scale = availableHeight / minimumTotal
			for (index in 0 until docked.size) heights[index] *= scale
			minimumTotal = availableHeight
		}
		var remaining = (availableHeight - minimumTotal).coerceAtLeast(0f)
		for (index in 0 until docked.size) {
			val entry = docked[index]
			if (entry.data.dockFill || remaining <= 0f) continue
			val requestedHeight = if (entry.data.dockHeight > 0f) entry.data.dockHeight else entry.window.height
			val allocated = (requestedHeight - heights[index]).coerceAtLeast(0f).coerceAtMost(remaining)
			heights[index] += allocated
			remaining -= allocated
		}
		var fillCount = 0
		for (index in 0 until docked.size) if (docked[index].data.dockFill) fillCount++
		if (fillCount > 0 && remaining > 0f) {
			val fillExtra = remaining / fillCount
			for (index in 0 until docked.size) if (docked[index].data.dockFill) heights[index] += fillExtra
		}

		val x = if (side == MetaDockSide.LEFT) config.margin else uiRenderer.uiWidth - config.margin - width
		var top = uiRenderer.uiHeight - config.topInset
		for (index in 0 until docked.size) {
			val entry = docked[index]
			val height = heights[index]
			val y = top - height
			(entry.window as? MetaWindow)?.applyDockState(
				side,
				entry.data.dockFill,
				minimumWidth ?: config.minimumSidebarWidth,
			)
			if (entry.window !== activeMovingWindow) {
				entry.window.setBounds(x, y, width, height)
				entry.window.invalidateHierarchy()
			}
			if (index < docked.size - 1 && config.gap > 0f) {
				val divider = dockDividerFor(entry.window)
				divider.configure(entry, docked[index + 1], index + 1 == docked.size - 1, dockLayoutGeneration)
				divider.setBounds(x, dockDividerBottom(y, config.gap), width, config.gap)
				if (divider.stage == null) uiRenderer.addActor(divider)
				divider.toFront()
			} else if (index == docked.size - 1 && config.gap > 0f) {
				// The final panel has no neighbour, but its lower edge is still a meaningful divider: dragging it turns a
				// fill panel into a fixed-height panel and intentionally leaves the remaining dock area empty.
				val divider = dockDividerFor(entry.window)
				divider.configureLast(entry, dockLayoutGeneration)
				divider.setBounds(x, dockDividerBottom(y, config.gap), width, config.gap)
				if (divider.stage == null) uiRenderer.addActor(divider)
				divider.toFront()
			}
			top = y - config.gap
		}
		return heights
	}

	private fun dockDividerFor(window: Window): DockDivider {
		var divider = dockDividerCache[window]
		if (divider == null) {
			divider = DockDivider()
			dockDividerCache.put(window, divider)
			dockDividers.add(divider)
		}
		return divider
	}

	private fun removeDockDivider(window: Window) {
		val divider = dockDividerCache.remove(window) ?: return
		divider.detach()
		dockDividers.removeValue(divider, true)
	}

	private fun removeAllDockDividers() {
		for (index in 0 until dockDividers.size) dockDividers[index].detach()
	}

	private inner class DockDivider : Actor() {
		var layoutGeneration = -1
		var dragging = false
		private lateinit var upper: DockedWindow
		private var lower: DockedWindow? = null
		private lateinit var resized: DockedWindow
		private var fixedLower: DockedWindow? = null
		private var resizeLowerPanel = false
		private var lowerIsLast = false
		private var startStageY = 0f
		private var startHeight = 0f
		private var cursorActive = false

		init {
			addListener(object : InputListener() {
				override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
					if (pointer == -1) setDividerCursor(true)
				}

				override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
					if (pointer == -1 && !dragging) setDividerCursor(false)
				}

				override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
					if (pointer != 0 || button != Input.Buttons.LEFT) return false
					dragging = true
					startStageY = event.stageY
					val lowerPanel = lower
					fixedLower = if (lowerPanel != null && lowerIsLast) lowerPanel else null
					fixedLower?.let {
						// Preserve the final panel's current height so dragging the divider moves it as a block. The
						// released space then remains intentionally empty below the dock.
						it.data.dockHeight = it.window.height
						it.data.dockFill = false
					}
					resizeLowerPanel = lowerPanel != null &&
						shouldResizeLowerDockPanel(upper.data.dockFill, lowerIsLast)
					resized = if (resizeLowerPanel) lowerPanel!! else upper
					startHeight = resized.window.height
					return true
				}

				override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
					val deltaY = event.stageY - startStageY
					resized.data.dockHeight = resizedDockPanelHeight(
						startHeight, deltaY, resizeLowerPanel, dockPanelMinimumHeight(resized.window),
					)
					resized.data.dockFill = false
					layoutDockedWindows()
				}

				override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
					if (!dragging) return
					dragging = false
					metaSave(resized.name, resized.data)
					fixedLower?.let { metaSave(it.name, it.data) }
					fixedLower = null
					setDividerCursor(hit(x, y, true) === this@DockDivider)
				}
			})
		}

		fun configure(upper: DockedWindow, lower: DockedWindow, lowerIsLast: Boolean, generation: Int) {
			this.upper = upper
			this.lower = lower
			this.lowerIsLast = lowerIsLast
			layoutGeneration = generation
		}

		fun configureLast(window: DockedWindow, generation: Int) {
			upper = window
			lower = null
			lowerIsLast = false
			layoutGeneration = generation
		}

		fun detach() {
			if (!dragging) setDividerCursor(false)
			remove()
		}

		private fun setDividerCursor(active: Boolean) {
			if (cursorActive == active) return
			cursorActive = active
			Gdx.graphics.setSystemCursor(if (active) Cursor.SystemCursor.VerticalResize else Cursor.SystemCursor.Arrow)
		}
	}

	private fun dockPanelMinimumHeight(window: Window): Float =
		(window as? MetaWindow)?.minimumDockHeight ?: window.minHeight.coerceAtLeast(0f)

	override fun bringWindowsToFront() {
		for (index in 0 until displayedWindows.size) displayedWindows[index].toFront()
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

	private class DockedWindow(
		val window: Window,
		val name: String,
		var data: MetaWindowData,
	)

	private companion object {
		val DOCKED_WINDOW_COMPARATOR = Comparator<DockedWindow> { first, second ->
			val orderComparison = first.data.dockOrder.compareTo(second.data.dockOrder)
			if (orderComparison != 0) orderComparison else first.name.compareTo(second.name)
		}
		const val DOCK_LAYOUT_DATA_KEY = "DockLayout"
		const val DOCK_ORDER_STEP = 100
		const val MENU_BAR_HEIGHT = 34f
		const val SAVE_THROTTLE_MILLIS = 200L
		const val DOCK_HINT_WIDTH = 5f
		const val DOCK_HINT_HEIGHT = 112f
	}

	override fun dispose() {
		backdropEffect.dispose()
		flushPendingSavesTask.cancel()
		flushPendingSaves() // don't lose a trailing save that was still in flight
		for (index in 0 until displayedWindows.size) displayedWindows[index].remove()
		displayedWindows.clear()
		dockEntryCache.clear()
		removeAllDockDividers()
		dockDividerCache.clear()
		dockDividers.clear()
		leftDockedWindows.clear()
		rightDockedWindows.clear()
		clearDockIndicators()
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
