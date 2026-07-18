package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.extensions.cursorPointer
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.reactive.subscribe
import de.fatox.meta.input.MetaUiAction
import de.fatox.meta.ui.MetaControlSize
import de.fatox.meta.ui.MetaFocusable
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaUiActionHandler
import kotlin.math.abs

/**
 * A [Slider] flanked by decrement/increment buttons, laid out horizontally or vertically. Each button steps the
 * slider by one [Slider.getStepSize].
 *
 * Horizontal: `[-] [====slider====] [+]`. Vertical: top increments, bottom decrements.
 */
class SliderWithButtons(
	min: Float,
	max: Float,
	stepSize: Float,
	vertical: Boolean,
	@Suppress("UNUSED_PARAMETER")
	assetProvider: AssetProvider = inject(),
) : MetaFlexBox(
	direction = if (vertical) MetaFlexDirection.COLUMN else MetaFlexDirection.ROW,
	mainGap = MetaSpacing.SM,
	align = MetaFlexAlign.CENTER,
) {

	/** The underlying slider; exposed for listeners/styling. */
	val slider: Slider = DirectionalSlider(min, max, stepSize, vertical).apply { cursorPointer() }

	val decrementButton: MetaIconButton = MetaIconButton("ri-subtract-line")
	val incrementButton: MetaIconButton = MetaIconButton("ri-add-line")
	val valueValue: Signal<Float> = signal(slider.value) { a, b -> abs(a - b) < 0.0001f }
	/**
	 * Stable value for expensive or geometry-changing reactions. Unlike [valueValue], slider drags publish here only
	 * on release; keyboard and step-button changes still publish immediately.
	 */
	val committedValue: Signal<Float> = signal(slider.value) { a, b -> abs(a - b) < 0.0001f }
	private var pointerGestureActive = false
	private var syncingFromSignal = false
	@Suppress("unused")
	private val valueBinding = valueValue.subscribe {
		if (syncingFromSignal) return@subscribe
		syncingFromSignal = true
		slider.value = valueValue.peek()
		val actual = slider.value
		if (abs(valueValue.peek() - actual) >= 0.0001f) valueValue.value = actual
		if (!pointerGestureActive && !slider.isDragging) committedValue.value = actual
		syncingFromSignal = false
	}

	/** Syntactic sugar for the current slider value. */
	var value: Float
		get() = slider.value
		set(value) {
			valueValue.value = value
		}

	init {
		decrementButton.onStep(-stepSize)
		incrementButton.onStep(+stepSize)
		slider.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				if (!syncingFromSignal) valueValue.value = slider.value
				if (!pointerGestureActive && !slider.isDragging) committedValue.value = slider.value
			}
		})
		slider.addListener(object : InputListener() {
			override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
				if (pointer != 0 || button != 0 || slider.isDisabled) return false
				pointerGestureActive = true
				return true
			}

			override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
				if (pointer != 0 || !pointerGestureActive) return
				pointerGestureActive = false
				committedValue.value = slider.value
			}
		})

		if (!vertical) {
			addItem(decrementButton, basisWidth = MetaControlSize.STANDARD.iconTarget,
				basisHeight = MetaControlSize.STANDARD.iconTarget, shrink = 0f)
			addItem(slider, grow = 1f, minWidth = 0f)
			addItem(incrementButton, basisWidth = MetaControlSize.STANDARD.iconTarget,
				basisHeight = MetaControlSize.STANDARD.iconTarget, shrink = 0f)
		} else {
			addItem(incrementButton, basisWidth = MetaControlSize.STANDARD.iconTarget,
				basisHeight = MetaControlSize.STANDARD.iconTarget, shrink = 0f)
			addItem(slider, grow = 1f, minHeight = 0f)
			addItem(decrementButton, basisWidth = MetaControlSize.STANDARD.iconTarget,
				basisHeight = MetaControlSize.STANDARD.iconTarget, shrink = 0f)
		}
	}

	private fun MetaIconButton.onStep(delta: Float) {
		addListener(object : ClickListener() {
			override fun clicked(event: InputEvent, x: Float, y: Float) {
				if (!slider.isDisabled) slider.value = slider.value + delta
			}
		})
	}

	private class DirectionalSlider(
		min: Float,
		max: Float,
		stepSize: Float,
		private val vertical: Boolean,
	) : Slider(min, max, stepSize, vertical, sliderStyle(vertical)), MetaFocusable, MetaUiActionHandler {
		private val normalStyle = SliderStyle(style)
		private val focusedStyle = SliderStyle(style).apply {
			knob = normalStyle.knobOver ?: normalStyle.knob
		}

		override fun setMetaFocused(focused: Boolean) {
			style = if (focused) focusedStyle else normalStyle
		}

		override fun handleMetaUiAction(action: MetaUiAction): Boolean {
			if (isDisabled) return false
			val delta = when {
				!vertical && action == MetaUiAction.NAVIGATE_LEFT -> -stepSize
				!vertical && action == MetaUiAction.NAVIGATE_RIGHT -> stepSize
				vertical && action == MetaUiAction.NAVIGATE_DOWN -> -stepSize
				vertical && action == MetaUiAction.NAVIGATE_UP -> stepSize
				else -> return false
			}
			value += delta
			return true
		}

		private companion object {
			fun sliderStyle(vertical: Boolean): SliderStyle {
				val skin = MetaSkin.skin()
				return if (!vertical && skin.has(MetaSkin.SLIDER_HORIZONTAL, SliderStyle::class.java)) {
					SliderStyle(skin.get(MetaSkin.SLIDER_HORIZONTAL, SliderStyle::class.java))
				} else {
					val styleName = if (vertical && skin.has("default-vertical", SliderStyle::class.java)) {
						"default-vertical"
					} else {
						"default-horizontal"
					}
					SliderStyle(skin.get(styleName, SliderStyle::class.java))
				}
			}
		}
	}
}
