package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

class MetaIconDrawable(
	icon: String,
	private val size: Int = 24,
	private val tint: Color = Color.WHITE,
) : BaseDrawable() {
	private val fontProvider: FontProvider by lazyInject()
	private val layout = GlyphLayout()
	private val glyph = MetaIcons.glyph(icon)
	private val oldColor = Color()

	init {
		minWidth = size.toFloat()
		minHeight = size.toFloat()
	}

	override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
		val font = fontProvider.getFont(size, FontType.ICON)
		layout.setText(font, glyph)

		oldColor.set(font.color)
		font.color.set(tint)
		font.draw(
			batch,
			glyph,
			x + (width - layout.width) * 0.5f,
			y + (height + layout.height) * 0.5f,
		)
		font.color.set(oldColor)
	}
}
