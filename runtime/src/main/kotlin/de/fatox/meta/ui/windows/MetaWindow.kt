package de.fatox.meta.ui.windows

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.utils.Align
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.extensions.onChange
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.api.graphics.snapToPhysicalPixel
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.MetaDockSide
import de.fatox.meta.api.ui.MetaWindowInteraction
import de.fatox.meta.api.ui.dockPreviewChanged
import de.fatox.meta.api.ui.windowGestureChanged
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.ui.FontRefreshable
import de.fatox.meta.ui.MetaMotion
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.refreshFontsRecursively
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.components.MetaIconButton
import de.fatox.meta.ui.components.MetaResizeGrip
import de.fatox.meta.ui.components.MetaScrollPane
import de.fatox.meta.ui.components.MetaSeparator
import de.fatox.meta.ui.components.MetaTable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Created by Frotty on 08.05.2016.
 */
abstract class MetaWindow(
	title: String,
	resizable: Boolean = false,
	closeButton: Boolean = false,
	/** Disable only for presentations such as untitled dialogs that intentionally have no window chrome. */
	hasHeader: Boolean = true,
) : Window(title, MetaSkin.skin(), if (resizable) MetaSkin.WINDOW_RESIZABLE else MetaSkin.WINDOW), FontRefreshable {
	protected val uiManager: UIManager by lazyInject()
	protected val assetProvider: AssetProvider by lazyInject()
	protected val metaData: MetaData by lazyInject()
	private val fontProvider: FontProvider by lazyInject()

	var contentTable: Table = MetaTable()

	/**
	 * Optional sticky controls rendered above [contentTable]. This row never participates in the content viewport,
	 * so filters, breadcrumbs and primary actions remain reachable while a long body scrolls. Leave it empty when a
	 * window has no persistent controls; its layout row then collapses to zero height.
	 */
	val controlTable: Table = MetaTable()

	/**
	 * Reactive bindings owned by this window's current on-screen presentation. Create bindings/effects here inside
	 * [onShown] (e.g. `reactiveScope.bindText(label) { someSignal() }`); they are disposed automatically in
	 * [onRemovedFromStage], so a window can be shown, hidden and re-shown without leaking effects or firing them on a
	 * hidden widget. Do NOT use it from `init` - those run once at construction, before the window is on a stage.
	 */
	protected var reactiveScope: ReactiveScope = ReactiveScope()
		private set

	private var startDrag = false
	private var activeInteraction = MetaWindowInteraction.PROGRAMMATIC
	private var gestureStartX = 0f
	private var gestureStartY = 0f
	private var gestureStartWidth = 0f
	private var gestureStartHeight = 0f
	private var settledX = 0f
	private var settledY = 0f
	private var settledWidth = 0f
	private var settledHeight = 0f
	private var previewX = Float.NaN
	private var previewY = Float.NaN
	private var previewWidth = Float.NaN
	private var previewOrderAnchorY = Float.NaN
	private var liveResizeWidth = Float.NaN
	private var liveResizeHeight = Float.NaN
	private var fadeInAction: Action? = null
	// Preallocated so entrances don't allocate a capturing lambda per show.
	private val resetEntranceTransform = Runnable { setTransform(false) }
	private val headerEnabled = hasHeader
	private var metaResizable = resizable
	internal var dockSide: MetaDockSide? = null
		private set
	internal var dockFill: Boolean = false
		private set
	private var dockMinimumWidth = MIN_WINDOW_WIDTH
	private var configuredMinimumWidth = 0f
	private var configuredMinimumHeight = 0f
	/** Dialogs override this so live content changes resize around their current screen center. */
	protected open val preserveCenterOnAutoFit: Boolean = false
	private var resizeEdge = 0
	private var resizeStartStageX = 0f
	private var resizeStartStageY = 0f
	private var resizeStartX = 0f
	private var resizeStartY = 0f
	private var resizeStartWidth = 0f
	private var resizeStartHeight = 0f
	internal var dockOrderAnchorY = Float.NaN
		private set

	/** The provider font generation this window's fonts were fetched for; see [refreshFontsIfStale]. */
	private var fontGeneration = 0
	private val headerSeparator = MetaSeparator().apply {
		touchable = Touchable.disabled
		isVisible = headerEnabled
	}
	private val contentScrollPane = MetaScrollPane(contentTable).apply {
		configureWindowContentScrolling(this)
	}
	internal val contentViewport: MetaScrollPane get() = contentScrollPane
	private val resizeIndicator = MetaResizeGrip().apply {
		touchable = Touchable.disabled
		isVisible = resizable
	}
	private var resizeCursorActive = false
	private val resizeCursorListener = object : InputListener() {
		override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
			if (pointer != 0 || button != 0) return false
			dockOrderAnchorY = event.stageY
			resizeEdge = resizeEdgeAt(x, y)
			// Keep a non-exclusive touch focus for title drags so their pointer anchor remains available even when a
			// tall window itself is clamped by the stage. The built-in Window listener still owns the actual movement.
			if (resizeEdge == 0) return y >= height - padTop
			updateResizeCursor(x, y)
			activeInteraction = if (resizeEdge and (Align.left or Align.right) != 0 && dockSide != null) {
				MetaWindowInteraction.DOCK_WIDTH_RESIZE
			} else {
				MetaWindowInteraction.RESIZE
			}
			resizeStartStageX = event.stageX
			resizeStartStageY = event.stageY
			resizeStartX = this@MetaWindow.x
			resizeStartY = this@MetaWindow.y
			resizeStartWidth = width
			resizeStartHeight = height
			event.stop()
			return true
		}

		override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
			if (pointer != 0) return
			dockOrderAnchorY = event.stageY
			if (resizeEdge == 0) return
			resizeFrom(event.stageX, event.stageY)
			// Platforms may restore the default cursor on mouse press; retain the active edge cursor for the gesture.
			updateResizeCursor(x, y)
			event.stop()
		}

		override fun mouseMoved(event: InputEvent, x: Float, y: Float): Boolean {
			updateResizeCursor(x, y)
			return false
		}

		override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: com.badlogic.gdx.scenes.scene2d.Actor?) {
			if (pointer == -1 && resizeCursorActive && !isDragging) setResizeCursor(null)
		}

		override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
			if (pointer != 0) return
			resizeEdge = 0
			updateResizeCursor(x, y)
		}
	}

	init {
		titleLabel.setAlignment(Align.left)
		applyTitleStyle()
		fontGeneration = fontProvider.fontGeneration
		applyWindowMetrics()

		titleTable.apply {
			clearChildren()
			isVisible = headerEnabled
			left().top()
			add(titleLabel).growX().height(HEADER_CONTENT_HEIGHT)
				.padLeft(MetaSpacing.MD).padTop(MetaSpacing.XS).padBottom(MetaSpacing.XS)
			if (closeButton) {
				val exitButton = MetaIconButton("ri-close-line").apply {
					setColor(1f, 1f, 1f, 0.72f)
					setIconSize(16f)
					onChange { close() }
					addListener(object : InputListener() {
						override fun touchDown(
							event: InputEvent,
							x: Float,
							y: Float,
							pointer: Int,
							button: Int
						): Boolean {
							event.stop()
							return false
						}
					})
				}
				add(exitButton).size(CLOSE_BUTTON_SIZE).pad(3f).padRight(MetaSpacing.SM)
			}
			row()
			add(headerSeparator).colspan(if (closeButton) 2 else 1).growX().height(HEADER_SEPARATOR_HEIGHT)
		}

		// libGDX Window expands every detected resize edge by an additional 25 px. Keep its resize path disabled and
		// use Meta's precise capture listener so scroll panes and other content cannot steal a resize press.
		super.setResizable(false)
		addCaptureListener(resizeCursorListener)
		applyWindowMetrics()
		contentTable.top().left()
		controlTable.top().left()
		add(controlTable).top().growX()
		row()
		add(contentScrollPane).top().grow()
			.padTop(MetaSpacing.XS).padLeft(MetaSpacing.XS).padRight(MetaSpacing.SM).padBottom(MetaSpacing.XS)
		row()
		addActor(resizeIndicator)
	}

	fun setDefaultSize(width: Float, height: Float) {
		configuredMinimumWidth = width
		configuredMinimumHeight = height
		setSize(width, height)
	}

	fun setDefaultPos(x: Float, y: Float) {
		setPosition(x, y)
	}

	fun setDefault(x: Float, y: Float, width: Float, height: Float) {
		configuredMinimumWidth = width
		configuredMinimumHeight = height
		setPosition(x, y)
		setSize(width, height)
	}

	override fun draw(batch: Batch, parentAlpha: Float) {
		snapBoundsToPixelGrid()
		validate()
		snapTitleToPixelGrid()
		super.draw(batch, parentAlpha)
		if (isDragging) {
			if (!startDrag) {
				startDrag = true
				gestureStartX = settledX
				gestureStartY = settledY
				gestureStartWidth = settledWidth
				gestureStartHeight = settledHeight
				if (activeInteraction == MetaWindowInteraction.PROGRAMMATIC) activeInteraction = MetaWindowInteraction.MOVE
			}
			if (resizeEdge == 0) {
				if (dockPreviewChanged(
						x, y, width, dockOrderAnchorY,
						previewX, previewY, previewWidth, previewOrderAnchorY,
					)
				) {
					previewX = x
					previewY = y
					previewWidth = width
					previewOrderAnchorY = dockOrderAnchorY
					uiManager.previewWindowDock(this)
					uiManager.updateWindow(this, MetaWindowInteraction.MOVE, finished = false)
				}
			} else {
				clearDockPreview()
				if (width != liveResizeWidth || height != liveResizeHeight) {
					liveResizeWidth = width
					liveResizeHeight = height
					uiManager.updateWindow(this, activeInteraction, finished = false)
				}
			}
		} else if (startDrag) {
			startDrag = false
			clearDockPreview()
			val changed = windowGestureChanged(
				activeInteraction,
				gestureStartX,
				gestureStartY,
				gestureStartWidth,
				gestureStartHeight,
				x,
				y,
				width,
				height,
			)
			if (changed) uiManager.updateWindow(this, activeInteraction, finished = true)
			dockOrderAnchorY = Float.NaN
			activeInteraction = MetaWindowInteraction.PROGRAMMATIC
			liveResizeWidth = Float.NaN
			liveResizeHeight = Float.NaN
		}
		if (!isDragging) {
			settledX = x
			settledY = y
			settledWidth = width
			settledHeight = height
		}
	}

	override fun setVisible(visible: Boolean) {
		super.setVisible(visible)
		// Drop any pending entrance first so rapid show/hide can't stack conflicting alpha/scale actions.
		fadeInAction?.let { removeAction(it) }
		fadeInAction = null
		if (visible) {
			// Quick fade+scale pop only when showing; hiding must not reset alpha or keep animating in the background.
			setColor(1f, 1f, 1f, 0f)
			val entrance = if (MetaMotion.enabled && width > 0f && height > 0f) {
				setOrigin(Align.center)
				setTransform(true)
				setScale(MetaMotion.POP_SCALE)
				Actions.sequence(
					Actions.parallel(
						Actions.alpha(1f, MetaMotion.QUICK),
						Actions.scaleTo(1f, 1f, MetaMotion.POP, MetaMotion.OVERSHOOT),
					),
					Actions.run(resetEntranceTransform),
				)
			} else {
				Actions.alpha(1f, MetaMotion.QUICK)
			}
			fadeInAction = entrance
			addAction(entrance)
		} else {
			// A window hidden mid-entrance must not stay scaled or keep the batch-breaking transform enabled.
			setTransform(false)
			setScale(1f)
		}
	}

	/** Manager-owned temporary hiding must not replay a floating-window entrance when dock panels are restored. */
	internal fun setManagerVisible(visible: Boolean) {
		if (!visible) {
			setVisible(false)
			return
		}
		// These windows never left the stage; they are resuming the same presentation with settled dock geometry.
		super.setVisible(true)
		fadeInAction?.let { removeAction(it) }
		fadeInAction = null
		setColor(color.r, color.g, color.b, 1f)
		setScale(1f)
		setTransform(false)
		positionTitleTableGeometry()
		invalidateHierarchy()
	}

	override fun setResizable(isResizable: Boolean) {
		metaResizable = isResizable
		super.setResizable(false)
		val skin = MetaSkin.skin()
		val styleName = if (isResizable) MetaSkin.WINDOW_RESIZABLE else MetaSkin.WINDOW
		if (skin.has(styleName, WindowStyle::class.java)) {
			style = skin.get(styleName, WindowStyle::class.java)
			applyTitleStyle()
		}
		applyWindowMetrics()
		resizeIndicator.isVisible = isResizable
		if (!isResizable) {
			resizeEdge = 0
			if (resizeCursorActive) setResizeCursor(null)
		}
	}

	internal fun applyDockState(
		side: MetaDockSide?,
		fill: Boolean = false,
		minimumWidth: Float = MIN_WINDOW_WIDTH,
	) {
		val previousSide = dockSide
		dockSide = side
		dockFill = side != null && fill
		dockMinimumWidth = minimumWidth
		// Re-layout reapplies the same dock state continuously during sidebar-width drags. Only a real side transition
		// should clear the old edge cursor; clearing it for an unchanged side breaks the active resize gesture.
		if (side != previousSide && resizeCursorActive) setResizeCursor(null)
		invalidate()
	}

	private fun clearDockPreview() {
		if (previewX.isNaN() && previewY.isNaN() && previewWidth.isNaN() && previewOrderAnchorY.isNaN()) return
		previewX = Float.NaN
		previewY = Float.NaN
		previewWidth = Float.NaN
		previewOrderAnchorY = Float.NaN
		uiManager.previewWindowDock(null)
	}

	override fun isResizable(): Boolean = metaResizable

	override fun isDragging(): Boolean = resizeEdge != 0 || super.isDragging()

	override fun hit(x: Float, y: Float, touchable: Boolean): Actor? {
		// Resize chrome is conceptually above window content even though it is not a layout row. Returning the window
		// here keeps scroll panes from receiving presses or cursor events in any resize activation zone.
		if (isVisible && (!touchable || this.touchable == Touchable.enabled) && resizeEdgeAt(x, y) != 0) return this
		return super.hit(x, y, touchable)
	}

	override fun layout() {
		fitStaticSurfaceToContent()
		// Assign child widths before asking for minimum height. A wrapped Label reports prefWidth=0 and derives its
		// height from its current width; querying minHeight before this pass can briefly wrap one character per line
		// and permanently inflate a small window because this layout only grows to satisfy minima.
		super.layout()
		updateWindowContentScrolling(contentScrollPane)
		val minW = minWidth
		val minH = minHeight
		// Sidebar width belongs to the shared dock, so a wide panel's preferred minimum must not override the user.
		val newW = if (dockSide == null) max(width, minW) else width
		val newH = if (dockSide == null) max(height, minH) else height
		if (newW != width || newH != height) {
			val oldH = height
			val top = y + height
			setSize(newW, newH)
			if (newH != oldH) setY(top - newH)
			super.layout()
		}
		positionTitleTableGeometry()
		positionChrome()
	}

	private fun positionTitleTableGeometry() {
		if (!headerEnabled) return
		titleTable.setSize((width - padLeft - padRight).coerceAtLeast(0f), padTop.coerceAtMost(height))
		titleTable.setPosition(padLeft, (height - padTop).coerceAtLeast(0f))
	}

	override fun getMinWidth(): Float {
		return max(super.getMinWidth(), MIN_WINDOW_WIDTH)
	}

	override fun getMinHeight(): Float {
		// Content is owned by an overflow viewport. Keeping its preferred height as a hard window minimum makes
		// constrained floating layouts overlap and then snap larger; chrome is the only true minimum.
		return max(super.getMinHeight(), MIN_WINDOW_HEIGHT)
	}

	open fun close() {
		remove()
		uiManager.closeWindow(this)
	}

	fun centerWindow() {
		val stage = stage ?: return
		setPosition((stage.width - width) * 0.5f, (stage.height - height) * 0.5f)
		snapBoundsToPixelGrid()
	}

	/**
	 * Fits this surface inside its stage and centers it with a stable edge margin. Use this after dynamically
	 * rebuilding dialog content; unlike the normal layout pass it is explicit, so movable dialogs stay where the user
	 * dragged them until the consumer deliberately requests a new presentation fit.
	 */
	fun fitAndCenterInStage(margin: Float = MetaSpacing.MD) {
		val stage = stage ?: return
		layout()
		val bounds = resolveCenteredSurfaceBounds(width, height, stage.width, stage.height, margin)
		setBounds(bounds.x, bounds.y, bounds.width, bounds.height)
		layout()
		snapBoundsToPixelGrid()
	}

	/**
	 * Drives the presentation lifecycle off scene2d's single detach/attach signal (every add/remove path goes through
	 * `setStage`). On attach we (re)open [reactiveScope] and call [onShown]; on detach we call [onRemovedFromStage]
	 * and dispose the scope, tearing down every binding created during this presentation.
	 */
	override fun setStage(stage: Stage?) {
		val wasOnStage = this.stage != null
		if (wasOnStage && stage == null && startDrag) {
			clearDockPreview()
			startDrag = false
			activeInteraction = MetaWindowInteraction.PROGRAMMATIC
			dockOrderAnchorY = Float.NaN
		}
		super.setStage(stage)
		if (stage != null && !wasOnStage) {
			fitStaticSurfaceToContent(stage.width, stage.height)
			refreshFontsIfStale()
			if (reactiveScope.isDisposed) reactiveScope = ReactiveScope()
			onShown()
		} else if (wasOnStage && stage == null) {
			onRemovedFromStage()
			reactiveScope.dispose()
		}
	}

	/** Auto-fit dialogs and fixed windows. User-resizable windows deliberately retain their chosen/persisted size. */
	internal fun fitStaticSurfaceToContent(
		viewportWidth: Float = stage?.width ?: 0f,
		viewportHeight: Float = stage?.height ?: 0f,
	) {
		if (metaResizable || viewportWidth <= 0f || viewportHeight <= 0f) return
		val resolved = resolveMetaSurfaceSize(
			configuredMinimumWidth = configuredMinimumWidth,
			configuredMinimumHeight = configuredMinimumHeight,
			preferredWidth = prefWidth,
			preferredHeight = prefHeight,
			viewportWidth = viewportWidth,
			viewportHeight = viewportHeight,
		)
		if (width != resolved.width || height != resolved.height) {
			val centerX = x + width * 0.5f
			val centerY = y + height * 0.5f
			setSize(resolved.width, resolved.height)
			if (preserveCenterOnAutoFit && stage != null) {
				setPosition(centerX - resolved.width * 0.5f, centerY - resolved.height * 0.5f)
			}
		}
	}

	/** Called when this window is attached to the stage (shown). Create reactive bindings in [reactiveScope] here. */
	protected open fun onShown() {}

	/** Called once when this window is detached from the stage by ANY path (close, screen change, direct remove). */
	protected open fun onRemovedFromStage() {}

	/** Re-fetches the title font after a UI-scale change; child Meta widgets refresh via the recursive stage walk. */
	override fun refreshFont() {
		fontGeneration = fontProvider.fontGeneration
		applyTitleStyle()
		titleLabel.invalidateHierarchy()
	}

	/**
	 * Windows are cached off-stage (see `WindowConfig.singletonCache`/`UIManager`), so the renderer's on-scale-change
	 * stage walk misses hidden ones — and their old fonts get disposed. Detect that via the provider's font
	 * generation and refresh this window's whole subtree when it is re-shown with stale fonts.
	 */
	private fun refreshFontsIfStale() {
		if (fontGeneration != fontProvider.fontGeneration) {
			refreshFontsRecursively()
		}
	}

	private fun applyTitleStyle() {
		titleLabel.style = Label.LabelStyle(fontProvider.getFont(14, FontType.REGULAR), titleLabel.color)
	}

	private fun applyWindowMetrics() {
		padTop(if (headerEnabled) HEADER_HEIGHT else BORDER_PAD)
		padLeft(BORDER_PAD)
		padRight(BORDER_PAD)
		// Window's built-in resize listener treats these pads as edge origins. Keep structural padding on the actual
		// border so the absolutely positioned grip and hit testing cannot drift apart.
		padBottom(BORDER_PAD)
	}

	/** Keeps the complete window transform on the physical pixel grid, including at non-integer HiDPI scales. */
	private fun snapBoundsToPixelGrid() {
		val currentStage = stage ?: return
		val viewport = currentStage.viewport
		val worldWidth = viewport.worldWidth
		val worldHeight = viewport.worldHeight
		if (worldWidth <= 0f || worldHeight <= 0f || Gdx.graphics.backBufferWidth <= 0 || Gdx.graphics.backBufferHeight <= 0) return
		val horizontalPixelsPerUnit = Gdx.graphics.backBufferWidth / worldWidth
		val verticalPixelsPerUnit = Gdx.graphics.backBufferHeight / worldHeight
		val snappedX = snapToPhysicalPixel(x, horizontalPixelsPerUnit)
		val snappedY = snapToPhysicalPixel(y, verticalPixelsPerUnit)
		val snappedWidth = snapToPhysicalPixel(width, horizontalPixelsPerUnit)
		val snappedHeight = snapToPhysicalPixel(height, verticalPixelsPerUnit)
		if (snappedX != x || snappedY != y || snappedWidth != width || snappedHeight != height) {
			setBounds(snappedX, snappedY, snappedWidth, snappedHeight)
		}
	}

	/** libGDX's Window title is its only remaining raw Label, so snap its final stage-space origin explicitly. */
	private fun snapTitleToPixelGrid() {
		if (!headerEnabled) return
		val currentStage = stage ?: return
		val viewport = currentStage.viewport
		if (viewport.worldWidth <= 0f || viewport.worldHeight <= 0f ||
			Gdx.graphics.backBufferWidth <= 0 || Gdx.graphics.backBufferHeight <= 0
		) return
		val horizontalPixelsPerUnit = Gdx.graphics.backBufferWidth / viewport.worldWidth
		val verticalPixelsPerUnit = Gdx.graphics.backBufferHeight / viewport.worldHeight
		val title = titleLabel
		title.setPosition(
			snapToPhysicalPixel(x + title.x, horizontalPixelsPerUnit) - x,
			snapToPhysicalPixel(y + title.y, verticalPixelsPerUnit) - y,
		)
	}

	private fun updateResizeCursor(localX: Float, localY: Float) {
		if (!metaResizable) {
			if (resizeCursorActive) setResizeCursor(null)
			return
		}
		val edge = if (resizeEdge != 0) resizeEdge else resizeEdgeAt(localX, localY)
		val cursor = when {
			edge and Align.bottom != 0 && edge and Align.right != 0 -> Cursor.SystemCursor.NWSEResize
			edge and Align.bottom != 0 && edge and Align.left != 0 -> Cursor.SystemCursor.NESWResize
			edge and Align.bottom != 0 -> Cursor.SystemCursor.VerticalResize
			edge and (Align.left or Align.right) != 0 -> Cursor.SystemCursor.HorizontalResize
			else -> null
		}
		if (cursor != null || resizeCursorActive) setResizeCursor(cursor)
	}

	private fun resizeEdgeAt(localX: Float, localY: Float): Int {
		if (!metaResizable || localX < 0f || localY < 0f || localX > width || localY > height) return 0
		// The inner edge changes the shared sidebar width. Height dividers live exclusively in the gap between docked
		// windows and are owned by MetaUiManager, so this window never steals input from its bottom content.
		val side = dockSide
		if (side != null) {
			// Deliberately avoid an enum `when`: Kotlin emits a separate MetaWindow$WhenMappings class for it. A stale
			// incremental desktop artifact once packaged MetaWindow.class without that new synthetic companion and
			// crashed on the first mouse move. Direct comparison keeps this input hot path self-contained.
			return if (side === MetaDockSide.LEFT) {
				if (localX >= width - DOCK_RESIZE_SIZE) Align.right else 0
			} else {
				if (localX <= DOCK_RESIZE_SIZE) Align.left else 0
			}
		}
		if (localX >= width - CORNER_RESIZE_SIZE && localY <= CORNER_RESIZE_SIZE) {
			return Align.right or Align.bottom
		}
		var edge = 0
		if (localX <= EDGE_RESIZE_SIZE) edge = edge or Align.left
		if (localX >= width - EDGE_RESIZE_SIZE) edge = edge or Align.right
		if (localY <= EDGE_RESIZE_SIZE) edge = edge or Align.bottom
		return edge
	}

	private fun resizeFrom(stageX: Float, stageY: Float) {
		val deltaX = stageX - resizeStartStageX
		val deltaY = stageY - resizeStartStageY
		var newX = resizeStartX
		var newY = resizeStartY
		var newWidth = resizeStartWidth
		var newHeight = resizeStartHeight
		val minWidth = if (dockSide != null) dockMinimumWidth else minWidth
		val minHeight = if (dockSide == null) minHeight else DOCK_MIN_WINDOW_HEIGHT
		val maxWidth = maxWidth
		val maxHeight = maxHeight

		if (resizeEdge and Align.left != 0) {
			newWidth = (resizeStartWidth - deltaX).coerceAtLeast(minWidth)
			if (maxWidth > 0f) newWidth = newWidth.coerceAtMost(maxWidth)
			newX = resizeStartX + resizeStartWidth - newWidth
		}
		if (resizeEdge and Align.right != 0) {
			newWidth = (resizeStartWidth + deltaX).coerceAtLeast(minWidth)
			if (maxWidth > 0f) newWidth = newWidth.coerceAtMost(maxWidth)
		}
		if (resizeEdge and Align.bottom != 0) {
			newHeight = (resizeStartHeight - deltaY).coerceAtLeast(minHeight)
			if (maxHeight > 0f) newHeight = newHeight.coerceAtMost(maxHeight)
			newY = resizeStartY + resizeStartHeight - newHeight
		}

		val currentStage = stage
		if (currentStage != null && parent === currentStage.root) {
			if (newX < 0f) {
				newWidth += newX
				newX = 0f
			}
			if (newY < 0f) {
				newHeight += newY
				newY = 0f
			}
			newWidth = newWidth.coerceAtMost(currentStage.width - newX)
		}
		setBounds(round(newX), round(newY), round(newWidth), round(newHeight))
	}

	private fun setResizeCursor(cursor: Cursor.SystemCursor?) {
		resizeCursorActive = cursor != null
		Gdx.graphics.setSystemCursor(cursor ?: Cursor.SystemCursor.Arrow)
	}

	private fun positionChrome() {
		headerSeparator.isVisible = headerEnabled
		resizeIndicator.isVisible = metaResizable && dockSide == null
		if (!resizeIndicator.isVisible) return
		resizeIndicator.setBounds(
			width - RESIZE_INDICATOR_SIZE - RESIZE_GRIP_RIGHT_PAD,
			RESIZE_GRIP_BOTTOM_PAD,
			RESIZE_INDICATOR_SIZE,
			RESIZE_INDICATOR_SIZE,
		)
		resizeIndicator.toFront()
	}

	private companion object {
		const val HEADER_HEIGHT = 40f
		const val HEADER_CONTENT_HEIGHT = 31f
		const val HEADER_SEPARATOR_HEIGHT = 1f
		const val BORDER_PAD = 1f
		const val CLOSE_BUTTON_SIZE = 24f
		const val EDGE_RESIZE_SIZE = 4f
		const val DOCK_RESIZE_SIZE = 8f
		const val CORNER_RESIZE_SIZE = 24f
		const val RESIZE_INDICATOR_SIZE = 16f
		// The grip drawable itself leaves two more units on its right than below. Compensate at placement so the
		// visible dots have equal distance from both window borders.
		const val RESIZE_GRIP_RIGHT_PAD = 0f
		const val RESIZE_GRIP_BOTTOM_PAD = 2f
		const val MIN_WINDOW_WIDTH = 96f
		const val MIN_WINDOW_HEIGHT = 64f
		const val DOCK_MIN_WINDOW_HEIGHT = HEADER_HEIGHT + 24f
	}

	internal val minimumDockHeight: Float get() = DOCK_MIN_WINDOW_HEIGHT
	internal val dockOrderThresholdY: Float get() = y + height - HEADER_HEIGHT * 0.5f
}

/** Start responsive: horizontal scrolling is enabled later only when the content's minimum width truly overflows. */
internal fun configureWindowContentScrolling(scrollPane: ScrollPane) {
	scrollPane.setScrollingDisabled(true, false)
}

/**
 * Re-evaluates horizontal overflow after the viewport has a real size. Preferred width is deliberately ignored:
 * responsive layouts may be narrower than preferred without clipping. Their minimum width is the point below which
 * content can no longer reflow, so only then does the viewport expose horizontal scrolling. Vertical overflow stays
 * owned by libGDX's normal preferred-height check.
 */
internal fun updateWindowContentScrolling(scrollPane: ScrollPane) {
	repeat(2) {
		scrollPane.validate()
		val layout = scrollPane.actor as? Layout
		val minimumWidth = layout?.minWidth ?: scrollPane.actor?.width ?: 0f
		val disableHorizontal = minimumWidth <= scrollPane.scrollWidth + SCROLL_OVERFLOW_TOLERANCE
		if (scrollPane.isScrollingDisabledX == disableHorizontal) return
		scrollPane.setScrollingDisabled(disableHorizontal, false)
		scrollPane.invalidate()
	}
	scrollPane.validate()
}

internal data class MetaSurfaceSize(val width: Float, val height: Float)

internal data class MetaSurfaceBounds(
	val x: Float,
	val y: Float,
	val width: Float,
	val height: Float,
)

internal fun resolveCenteredSurfaceBounds(
	surfaceWidth: Float,
	surfaceHeight: Float,
	viewportWidth: Float,
	viewportHeight: Float,
	margin: Float,
): MetaSurfaceBounds {
	val safeMargin = margin.coerceAtLeast(0f)
	val maximumWidth = (viewportWidth - safeMargin * 2f).coerceAtLeast(0f)
	val maximumHeight = (viewportHeight - safeMargin * 2f).coerceAtLeast(0f)
	val width = surfaceWidth.coerceAtMost(maximumWidth)
	val height = surfaceHeight.coerceAtMost(maximumHeight)
	return MetaSurfaceBounds(
		x = (viewportWidth - width) * 0.5f,
		y = (viewportHeight - height) * 0.5f,
		width = width,
		height = height,
	)
}

/** Responsive caps shared by dialogs and fixed windows; limits include their complete chrome. */
internal fun resolveMetaSurfaceSize(
	configuredMinimumWidth: Float,
	configuredMinimumHeight: Float,
	preferredWidth: Float,
	preferredHeight: Float,
	viewportWidth: Float,
	viewportHeight: Float,
): MetaSurfaceSize {
	val maximumWidth = min(viewportWidth * SURFACE_VIEWPORT_WIDTH_FRACTION, SURFACE_ABSOLUTE_MAX_WIDTH)
	val maximumHeight = min(viewportHeight * SURFACE_VIEWPORT_HEIGHT_FRACTION, SURFACE_ABSOLUTE_MAX_HEIGHT)
	return MetaSurfaceSize(
		width = max(configuredMinimumWidth, preferredWidth).coerceIn(0f, maximumWidth.coerceAtLeast(0f)),
		height = max(configuredMinimumHeight, preferredHeight).coerceIn(0f, maximumHeight.coerceAtLeast(0f)),
	)
}

private const val SCROLL_OVERFLOW_TOLERANCE = 0.5f
private const val SURFACE_VIEWPORT_WIDTH_FRACTION = 0.90f
private const val SURFACE_VIEWPORT_HEIGHT_FRACTION = 0.88f
private const val SURFACE_ABSOLUTE_MAX_WIDTH = 960f
private const val SURFACE_ABSOLUTE_MAX_HEIGHT = 840f
