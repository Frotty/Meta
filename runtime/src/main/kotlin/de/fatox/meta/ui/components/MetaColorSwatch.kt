package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Image
import de.fatox.meta.ui.ColorDrawable
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSkin

/** A color preview with a permanent contrasting border. */
class MetaColorSwatch(initialColor: Color = Color.WHITE) : MetaTable() {
	private val fill = Image(MetaSkin.skin().getDrawable(MetaSkin.COLOR_FILL))

	var selectedColor: Color
		get() = fill.color.cpy()
		set(value) {
			fill.color.set(value)
		}

	init {
		background = ColorDrawable(
			MetaSkin.skin().getDrawable(MetaSkin.COLOR_FILL),
			MetaColor.BORDER_STRONG
		)
		add(fill).grow().pad(BORDER_WIDTH)
		selectedColor = initialColor
	}

	fun setSelectedColor(color: Color): MetaColorSwatch = apply {
		selectedColor = color
	}

	private companion object {
		const val BORDER_WIDTH = 2f
	}
}
