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
		val checkerboard = MetaTable().apply {
			for (row in 0 until CHECKER_ROWS) {
				for (column in 0 until CHECKER_COLUMNS) {
					add(Image(ColorDrawable(
						MetaSkin.skin().getDrawable(MetaSkin.COLOR_FILL),
						if ((row + column) and 1 == 0) CHECK_LIGHT else CHECK_DARK,
					))).grow()
				}
				row()
			}
		}
		add(MetaStack().apply {
			addItem(checkerboard)
			addItem(fill)
		}).grow().pad(BORDER_WIDTH)
		selectedColor = initialColor
	}

	fun setSelectedColor(color: Color): MetaColorSwatch = apply {
		selectedColor = color
	}

	private companion object {
		const val BORDER_WIDTH = 2f
		const val CHECKER_COLUMNS = 12
		const val CHECKER_ROWS = 2
		val CHECK_LIGHT = Color(0.72f, 0.72f, 0.72f, 1f)
		val CHECK_DARK = Color(0.42f, 0.42f, 0.42f, 1f)
	}
}
