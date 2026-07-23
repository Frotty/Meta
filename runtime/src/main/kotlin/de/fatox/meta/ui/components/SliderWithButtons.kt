package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.ui.MetaControlSize
import de.fatox.meta.ui.MetaSpacing

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

	private val metaSlider = MetaSlider(min, max, stepSize, vertical)
	/** The underlying slider; exposed for listeners/styling. Kept as [Slider] for binary API compatibility. */
	val slider: Slider get() = metaSlider

	val decrementButton: MetaIconButton = MetaIconButton("ri-subtract-line")
	val incrementButton: MetaIconButton = MetaIconButton("ri-add-line")
	val valueValue: Signal<Float> get() = metaSlider.valueValue
	/**
	 * Stable value for expensive or geometry-changing reactions. Unlike [valueValue], slider drags publish here only
	 * on release; keyboard and step-button changes still publish immediately.
	 */
	val committedValue: Signal<Float> get() = metaSlider.committedValue

	/** Syntactic sugar for the current slider value. */
	var value: Float
		get() = slider.value
		set(value) {
			valueValue.value = value
		}

	init {
		decrementButton.onStep(-stepSize)
		incrementButton.onStep(+stepSize)
		if (!vertical) {
			addItem(decrementButton, basisWidth = MetaControlSize.STANDARD.iconTarget,
				basisHeight = MetaControlSize.STANDARD.iconTarget, shrink = 0f)
			addItem(metaSlider, grow = 1f, minWidth = 0f)
			addItem(incrementButton, basisWidth = MetaControlSize.STANDARD.iconTarget,
				basisHeight = MetaControlSize.STANDARD.iconTarget, shrink = 0f)
		} else {
			addItem(incrementButton, basisWidth = MetaControlSize.STANDARD.iconTarget,
				basisHeight = MetaControlSize.STANDARD.iconTarget, shrink = 0f)
			addItem(metaSlider, grow = 1f, minHeight = 0f)
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

}
