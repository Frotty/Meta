package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import de.fatox.meta.api.extensions.cursorPointer
import de.fatox.meta.input.MetaUiAction
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.reactive.subscribe
import de.fatox.meta.ui.MetaFocusable
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaUiActionHandler
import kotlin.math.abs

/** Meta-styled slider with reactive live/committed values and keyboard/controller focus behavior. */
class MetaSlider(
	min: Float,
	max: Float,
	stepSize: Float,
	private val vertical: Boolean = false,
	showTrack: Boolean = true,
) : Slider(min, max, stepSize, vertical, sliderStyle(vertical, showTrack)), MetaFocusable, MetaUiActionHandler {
	private val normalStyle = SliderStyle(style)
	private val focusedStyle = SliderStyle(style).apply {
		knob = normalStyle.knobOver ?: normalStyle.knob
	}
	val valueValue: Signal<Float> = signal(value) { a, b -> abs(a - b) < EPSILON }
	val committedValue: Signal<Float> = signal(value) { a, b -> abs(a - b) < EPSILON }
	private var pointerGestureActive = false
	private var syncingFromSignal = false
	@Suppress("unused")
	private val valueBinding = valueValue.subscribe {
		if (syncingFromSignal) return@subscribe
		syncingFromSignal = true
		value = valueValue.peek()
		val actual = value
		if (abs(valueValue.peek() - actual) >= EPSILON) valueValue.value = actual
		if (!pointerGestureActive && !isDragging) committedValue.value = actual
		syncingFromSignal = false
	}

	init {
		cursorPointer()
		addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				if (!syncingFromSignal) valueValue.value = value
				if (!pointerGestureActive && !isDragging) committedValue.value = value
			}
		})
		addListener(object : InputListener() {
			override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
				if (pointer != 0 || button != 0 || isDisabled) return false
				pointerGestureActive = true
				return true
			}

			override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
				if (pointer != 0 || !pointerGestureActive) return
				pointerGestureActive = false
				committedValue.value = value
			}
		})
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
		const val EPSILON = 0.0001f

		fun sliderStyle(vertical: Boolean, showTrack: Boolean): SliderStyle {
			val skin = MetaSkin.skin()
			val resolved = if (!vertical && skin.has(MetaSkin.SLIDER_HORIZONTAL, SliderStyle::class.java)) {
				SliderStyle(skin.get(MetaSkin.SLIDER_HORIZONTAL, SliderStyle::class.java))
			} else {
				val styleName = if (vertical && skin.has("default-vertical", SliderStyle::class.java)) {
					"default-vertical"
				} else {
					"default-horizontal"
				}
				SliderStyle(skin.get(styleName, SliderStyle::class.java))
			}
			if (!showTrack) {
				// libGDX's Slider dereferences its background while converting pointer position to a value.
				// A transparent drawable preserves that geometry contract without painting over a custom ramp.
				resolved.background = BaseDrawable()
				resolved.disabledBackground = BaseDrawable()
				resolved.knobBefore = null
				resolved.knobAfter = null
				resolved.disabledKnobBefore = null
				resolved.disabledKnobAfter = null
			}
			return resolved
		}
	}
}
