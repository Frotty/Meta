package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisSlider
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.injection.MetaInject.Companion.inject

/**
 * A [Slider] flanked by decrement/increment buttons, laid out horizontally or vertically. Each button steps the
 * slider by one [Slider.getStepSize]. The +/- icons resolve through the [AssetProvider] under the conventional
 * `textures/ui/minus` and `textures/ui/plus` paths.
 *
 * Horizontal: `[-] [====slider====] [+]`. Vertical: top increments, bottom decrements.
 */
class SliderWithButtons(
	min: Float,
	max: Float,
	stepSize: Float,
	vertical: Boolean,
	assetProvider: AssetProvider = inject(),
) : Table(VisUI.getSkin()) {

	/** The underlying slider; exposed for listeners/styling. */
	val slider: Slider = VisSlider(min, max, stepSize, vertical)

	val decrementButton: VisImageButton = VisImageButton(assetProvider.getDrawable("textures/ui/minus"))
	val incrementButton: VisImageButton = VisImageButton(assetProvider.getDrawable("textures/ui/plus"))

	/** Syntactic sugar for the current slider value. */
	var value: Float
		get() = slider.value
		set(value) {
			slider.value = value
		}

	init {
		decrementButton.onStep(-stepSize)
		incrementButton.onStep(+stepSize)

		if (!vertical) {
			add<Button>(decrementButton).height(40f).width(26f)
			add(slider).grow().padLeft(6f).padRight(6f)
			add<Button>(incrementButton).height(40f).width(26f)
		} else {
			add<Button>(incrementButton).height(40f).width(26f).row()
			add(slider).expand().fill().row()
			add<Button>(decrementButton).height(40f).width(26f)
		}
	}

	private fun VisImageButton.onStep(delta: Float) {
		addListener(object : ClickListener() {
			override fun clicked(event: InputEvent, x: Float, y: Float) {
				if (!slider.isDisabled) slider.value = slider.value + delta
			}
		})
	}
}
