package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.extensions.cursorPointer
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing
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
) : Table(MetaSkin.skin()) {

	/** The underlying slider; exposed for listeners/styling. */
	val slider: Slider = Slider(min, max, stepSize, vertical, MetaSkin.skin()).apply {
		if (!vertical && MetaSkin.skin().has(MetaSkin.SLIDER_HORIZONTAL, Slider.SliderStyle::class.java)) {
			style = MetaSkin.skin().get(MetaSkin.SLIDER_HORIZONTAL, Slider.SliderStyle::class.java)
		}
		cursorPointer()
	}

	val decrementButton: MetaIconButton = MetaIconButton("ri-subtract-line")
	val incrementButton: MetaIconButton = MetaIconButton("ri-add-line")
	val valueValue: Signal<Float> = signal(slider.value) { a, b -> abs(a - b) < 0.0001f }
	/**
	 * Stable value for expensive or geometry-changing reactions. Unlike [valueValue], slider drags publish here only
	 * on release; keyboard and step-button changes still publish immediately.
	 */
	val committedValue: Signal<Float> = signal(slider.value) { a, b -> abs(a - b) < 0.0001f }
	private var pointerGestureActive = false

	/** Syntactic sugar for the current slider value. */
	var value: Float
		get() = slider.value
		set(value) {
			slider.value = value
			valueValue.value = slider.value
			committedValue.value = slider.value
		}

	init {
		decrementButton.onStep(-stepSize)
		incrementButton.onStep(+stepSize)
		slider.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				valueValue.value = slider.value
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
			add<Button>(decrementButton).size(STEP_BUTTON_SIZE)
			add(slider).growX().padLeft(MetaSpacing.SM).padRight(MetaSpacing.SM)
			add<Button>(incrementButton).size(STEP_BUTTON_SIZE)
		} else {
			add<Button>(incrementButton).size(STEP_BUTTON_SIZE).row()
			add(slider).growY().padTop(MetaSpacing.SM).padBottom(MetaSpacing.SM).row()
			add<Button>(decrementButton).size(STEP_BUTTON_SIZE)
		}
	}

	private fun MetaIconButton.onStep(delta: Float) {
		addListener(object : ClickListener() {
			override fun clicked(event: InputEvent, x: Float, y: Float) {
				if (!slider.isDisabled) slider.value = slider.value + delta
			}
		})
	}

	private companion object {
		const val STEP_BUTTON_SIZE = 32f
	}
}
