package de.fatox.meta.ui.windows

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.utils.Align
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.extensions.onChange
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.ui.FontRefreshable
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.refreshFontsRecursively
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.components.MetaIconButton
import de.fatox.meta.ui.components.MetaResizeGrip
import de.fatox.meta.ui.components.MetaSeparator
import de.fatox.meta.ui.components.MetaTable
import kotlin.math.max

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
	 * Reactive bindings owned by this window's current on-screen presentation. Create bindings/effects here inside
	 * [onShown] (e.g. `reactiveScope.bindText(label) { someSignal() }`); they are disposed automatically in
	 * [onRemovedFromStage], so a window can be shown, hidden and re-shown without leaking effects or firing them on a
	 * hidden widget. Do NOT use it from `init` - those run once at construction, before the window is on a stage.
	 */
	protected var reactiveScope: ReactiveScope = ReactiveScope()
		private set

	private var startDrag = false
	private var fadeInAction: Action? = null
	private val headerEnabled = hasHeader

	/** The provider font generation this window's fonts were fetched for; see [refreshFontsIfStale]. */
	private var fontGeneration = 0
	private val headerSeparator = MetaSeparator().apply {
		touchable = Touchable.disabled
		isVisible = headerEnabled
	}
	private val resizeIndicator = MetaResizeGrip().apply {
		touchable = Touchable.disabled
		isVisible = resizable
	}
	private var resizeCursorActive = false
	private val resizeCursorListener = object : InputListener() {
		override fun mouseMoved(event: InputEvent, x: Float, y: Float): Boolean {
			updateResizeCursor(x, y)
			return false
		}

		override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: com.badlogic.gdx.scenes.scene2d.Actor?) {
			if (pointer == -1 && resizeCursorActive && !isDragging) setResizeCursor(null)
		}

		override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
			if (pointer == 0) updateResizeCursor(x, y)
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
				add(exitButton).size(CLOSE_BUTTON_SIZE).padTop(3f).padBottom(3f).padRight(MetaSpacing.XS)
			}

		}

		if (resizable) {
			super.setResizable(true)
		}
		setResizeBorder(RESIZE_BORDER)
		addCaptureListener(resizeCursorListener)
		applyWindowMetrics()
		contentTable.top().left()
		add(contentTable).top().grow()
			.padTop(MetaSpacing.XS).padLeft(MetaSpacing.XS).padRight(MetaSpacing.XS).padBottom(0f)
		row()
		addActor(headerSeparator)
		addActor(resizeIndicator)
	}

	fun setDefaultSize(width: Float, height: Float) {
		setDefault(x, y, width, height)
	}

	fun setDefaultPos(x: Float, y: Float) {
		setDefault(x, y, width, height)
	}

	fun setDefault(x: Float, y: Float, width: Float, height: Float) {
		setPosition(x, y)
		setSize(width, height)
	}

	override fun draw(batch: Batch, parentAlpha: Float) {
		super.draw(batch, parentAlpha)
		if (isDragging) {
			startDrag = true
		} else if (startDrag) {
			startDrag = false
			uiManager.updateWindow(this)
		}
	}

	override fun setVisible(visible: Boolean) {
		super.setVisible(visible)
		// Drop any pending fade first so rapid show/hide can't stack conflicting alpha actions.
		fadeInAction?.let { removeAction(it) }
		fadeInAction = null
		if (visible) {
			// Fade in only when showing; hiding must not reset alpha or keep animating in the background.
			setColor(1f, 1f, 1f, 0f)
			val fade = Actions.alpha(0.9f, 0.75f)
			fadeInAction = fade
			addAction(fade)
		}
	}

	override fun setResizable(isResizable: Boolean) {
		super.setResizable(isResizable)
		val skin = MetaSkin.skin()
		val styleName = if (isResizable) MetaSkin.WINDOW_RESIZABLE else MetaSkin.WINDOW
		if (skin.has(styleName, WindowStyle::class.java)) {
			style = skin.get(styleName, WindowStyle::class.java)
			applyTitleStyle()
		}
		applyWindowMetrics()
		resizeIndicator.isVisible = isResizable
		if (!isResizable && resizeCursorActive) setResizeCursor(null)
	}

	override fun layout() {
		// Assign child widths before asking for minimum height. A wrapped Label reports prefWidth=0 and derives its
		// height from its current width; querying minHeight before this pass can briefly wrap one character per line
		// and permanently inflate a small window because this layout only grows to satisfy minima.
		super.layout()
		val minW = minWidth
		val minH = minHeight
		val newW = max(width, minW)
		val newH = max(height, minH)
		if (newW != width || newH != height) {
			val oldH = height
			val top = y + height
			setSize(newW, newH)
			if (newH != oldH) setY(top - newH)
			super.layout()
		}
		positionChrome()
	}

	override fun getMinWidth(): Float {
		return max(super.getMinWidth(), MIN_WINDOW_WIDTH)
	}

	override fun getMinHeight(): Float {
		val contentMin = contentTable.minHeight + padTop + padBottom
		return max(max(super.getMinHeight(), contentMin), MIN_WINDOW_HEIGHT)
	}

	open fun close() {
		remove()
		uiManager.closeWindow(this)
	}

	fun centerWindow() {
		val stage = stage ?: return
		setPosition((stage.width - width) * 0.5f, (stage.height - height) * 0.5f)
	}

	/**
	 * Drives the presentation lifecycle off scene2d's single detach/attach signal (every add/remove path goes through
	 * `setStage`). On attach we (re)open [reactiveScope] and call [onShown]; on detach we call [onRemovedFromStage]
	 * and dispose the scope, tearing down every binding created during this presentation.
	 */
	override fun setStage(stage: Stage?) {
		val wasOnStage = this.stage != null
		super.setStage(stage)
		if (stage != null && !wasOnStage) {
			refreshFontsIfStale()
			if (reactiveScope.isDisposed) reactiveScope = ReactiveScope()
			onShown()
		} else if (wasOnStage && stage == null) {
			onRemovedFromStage()
			reactiveScope.dispose()
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

	private fun updateResizeCursor(localX: Float, localY: Float) {
		if (!isResizable) {
			if (resizeCursorActive) setResizeCursor(null)
			return
		}
		val halfBorder = RESIZE_BORDER * 0.5f
		val left = localX <= padLeft + halfBorder
		val right = localX >= width - padRight - halfBorder
		val bottom = localY <= padBottom + halfBorder
		val cursor = when {
			bottom && right -> Cursor.SystemCursor.NWSEResize
			bottom && left -> Cursor.SystemCursor.NESWResize
			bottom -> Cursor.SystemCursor.VerticalResize
			left || right -> Cursor.SystemCursor.HorizontalResize
			else -> null
		}
		if (cursor != null || resizeCursorActive) setResizeCursor(cursor)
	}

	private fun setResizeCursor(cursor: Cursor.SystemCursor?) {
		resizeCursorActive = cursor != null
		Gdx.graphics.setSystemCursor(cursor ?: Cursor.SystemCursor.Arrow)
	}

	private fun positionChrome() {
		headerSeparator.isVisible = headerEnabled
		if (headerEnabled) {
			headerSeparator.setBounds(1f, height - HEADER_HEIGHT, width - 2f, HEADER_SEPARATOR_HEIGHT)
			headerSeparator.toFront()
		}
		resizeIndicator.isVisible = isResizable
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
		const val HEADER_CONTENT_HEIGHT = 32f
		const val HEADER_SEPARATOR_HEIGHT = 1f
		const val BORDER_PAD = 1f
		const val CLOSE_BUTTON_SIZE = 24f
		const val RESIZE_BORDER = 36
		const val RESIZE_INDICATOR_SIZE = 16f
		const val RESIZE_GRIP_RIGHT_PAD = 2f
		const val RESIZE_GRIP_BOTTOM_PAD = 2f
		const val MIN_WINDOW_WIDTH = 96f
		const val MIN_WINDOW_HEIGHT = 64f
	}
}
