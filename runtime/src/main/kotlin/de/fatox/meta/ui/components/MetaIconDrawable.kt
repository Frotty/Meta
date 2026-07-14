package de.fatox.meta.ui.components

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import kotlin.math.max
import kotlin.math.roundToInt

class MetaIconDrawable(
	icon: String,
	private val size: Int = MetaIconButton.DEFAULT_ICON_SIZE.toInt(),
	private val tint: Color = Color.WHITE,
) : BaseDrawable() {
	private val fontProvider: FontProvider by lazyInject()
	private val uiRenderer: UIRenderer by lazyInject()
	private val layout = GlyphLayout()
	private val glyph = MetaIcons.glyph(icon)
	private val oldColor = Color()

	// Font instance the cached [layout] was measured with; getFont returns a new instance after a UI-scale change,
	// so re-measuring only on identity change keeps draw() allocation-free per frame.
	private var layoutFont: BitmapFont? = null

	init {
		minWidth = size.toFloat()
		minHeight = size.toFloat()
	}

	override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
		val font = fontProvider.getFont(size, FontType.ICON)
		if (font !== layoutFont) {
			layoutFont = font
			layout.setText(font, glyph)
		}

		oldColor.set(font.color)
		font.color.set(tint)
		// Snap the centered position to the physical pixel grid (like MetaLabel) so the glyph stays crisp on HiDPI.
		val pixelsPerUnit = pixelsPerUnit()
		font.draw(
			batch,
			glyph,
			snapToPixel(x + (width - layout.width) * 0.5f, pixelsPerUnit),
			snapToPixel(y + (height + layout.height) * 0.5f, pixelsPerUnit),
		)
		font.color.set(oldColor)
	}

	private fun pixelsPerUnit(): Float {
		val uiWidth = uiRenderer.uiWidth
		if (uiWidth <= 0f) return 1f
		return max(1f, Gdx.graphics.backBufferWidth / uiWidth)
	}

	private fun snapToPixel(value: Float, pixelsPerUnit: Float): Float {
		return (value * pixelsPerUnit).roundToInt() / pixelsPerUnit
	}
}
