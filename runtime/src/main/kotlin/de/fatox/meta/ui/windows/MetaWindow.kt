package de.fatox.meta.ui.windows

import com.badlogic.gdx.graphics.g2d.Batch
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
import de.fatox.meta.ui.MetaSkin
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
) : Window(title, MetaSkin.skin(), if (resizable) MetaSkin.WINDOW_RESIZABLE else MetaSkin.WINDOW) {
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
	private val headerSeparator = MetaSeparator().apply {
		touchable = Touchable.disabled
	}
	private val resizeIndicator = MetaResizeGrip().apply {
		touchable = Touchable.disabled
		isVisible = resizable
	}

	init {
		titleLabel.setAlignment(Align.left)
		applyTitleStyle()
		applyWindowMetrics(resizable)

		titleTable.apply {
			clearChildren()
			left().top()
			add(titleLabel).growX().height(HEADER_CONTENT_HEIGHT).padLeft(MetaSpacing.SM)
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
		applyWindowMetrics(resizable)
		contentTable.top().left().pad(5f, 1f, 1f, 1f)
		add(contentTable).top().grow().pad(MetaSpacing.XS)
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
		setColor(1f, 1f, 1f, 0f)
		addAction(Actions.alpha(0.9f, 0.75f))
	}

	override fun setResizable(isResizable: Boolean) {
		super.setResizable(isResizable)
		val skin = MetaSkin.skin()
		val styleName = if (isResizable) MetaSkin.WINDOW_RESIZABLE else MetaSkin.WINDOW
		if (skin.has(styleName, WindowStyle::class.java)) {
			style = skin.get(styleName, WindowStyle::class.java)
			applyTitleStyle()
		}
		applyWindowMetrics(isResizable)
		resizeIndicator.isVisible = isResizable
	}

	override fun layout() {
		val minW = minWidth
		val minH = minHeight
		val newW = max(width, minW)
		val newH = max(height, minH)
		if (newW != width || newH != height) {
			val oldH = height
			val top = y + height
			setSize(newW, newH)
			if (newH != oldH) setY(top - newH)
		}
		super.layout()
		positionChrome()
	}

	override fun getMinWidth(): Float {
		return max(super.getMinWidth(), MIN_WINDOW_WIDTH)
	}

	override fun getMinHeight(): Float {
		val contentMin = contentTable.minHeight + padTop + padBottom + MetaSpacing.SM
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

	private fun applyTitleStyle() {
		titleLabel.style = Label.LabelStyle(fontProvider.getFont(14, FontType.REGULAR), titleLabel.color)
	}

	private fun applyWindowMetrics(resizable: Boolean) {
		padTop(HEADER_HEIGHT)
		padLeft(1f)
		padRight(1f)
		padBottom(if (resizable) RESIZE_BOTTOM_PAD else 1f)
	}

	private fun positionChrome() {
		headerSeparator.setBounds(1f, height - HEADER_HEIGHT, width - 2f, HEADER_SEPARATOR_HEIGHT)
		headerSeparator.toFront()
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
		const val HEADER_HEIGHT = 32f
		const val HEADER_CONTENT_HEIGHT = 30f
		const val HEADER_SEPARATOR_HEIGHT = 1f
		const val CLOSE_BUTTON_SIZE = 24f
		const val RESIZE_BOTTOM_PAD = 11f
		const val RESIZE_INDICATOR_SIZE = 12f
		const val RESIZE_GRIP_RIGHT_PAD = 8f
		const val RESIZE_GRIP_BOTTOM_PAD = 8f
		const val MIN_WINDOW_WIDTH = 96f
		const val MIN_WINDOW_HEIGHT = 64f
	}
}
