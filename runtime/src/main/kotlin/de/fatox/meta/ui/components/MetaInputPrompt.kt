package de.fatox.meta.ui.components

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFontCache
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.api.graphics.physicalPixelsPerStageUnit
import de.fatox.meta.api.graphics.physicalPixelsPerUnit
import de.fatox.meta.api.graphics.snapToPhysicalPixel
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.FontGenerationTracker
import de.fatox.meta.ui.FontRefreshable
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaInputGlyphSkin
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaType
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A button prompt: one or more glyphs and what pressing them does - "[A] Select", "[↑↓] Navigate".
 *
 * <p>Draws itself rather than composing a table of a glyph actor and a label, because the thing that makes a prompt
 * look right is an alignment no table cell can express: the glyph's centre sits on the label's cap-height centre, not
 * on the middle of the label's line box. A line box carries the font's ascender and descender, which are not
 * symmetric, so centring on it puts the words visibly above or below the button - the first thing anyone notices
 * about a prompt bar that is slightly wrong.
 *
 * <p>Everything is sized from one number, the label's font size: the glyph is a little taller than a capital, a
 * key's name is set a size smaller than the label, and the gaps scale with the glyph. So a prompt at any size is the
 * same prompt, and a game only ever chooses how big.
 */
class MetaInputPrompt @JvmOverloads constructor(
	glyphs: List<MetaInputGlyph>,
	label: String,
	fontSize: Int = MetaType.BODY,
	private val labelColor: Color = MetaColor.TEXT_MUTED,
) : Widget(), FontRefreshable {
	companion object {
		/**
		 * Every face a prompt at [fontSize] draws with, as (size, type). For an application that rasterizes its faces
		 * during startup: a prompt bar built on the first frame otherwise pays for these on that frame.
		 */
		@JvmStatic
		fun requiredFonts(fontSize: Int): List<Pair<Int, FontType>> = listOf(
			fontSize to FontType.REGULAR,
			keyTextSize(fontSize) to FontType.REGULAR,
			faceTextSize(fontSize) to FontType.BOLD,
		)

		private fun keyTextSize(fontSize: Int) = (fontSize * KEY_TEXT_SCALE).roundToInt().coerceAtLeast(6)
		private fun faceTextSize(fontSize: Int) = (fontSize * FACE_TEXT_SCALE).roundToInt().coerceAtLeast(6)

		/** Glyph height over the label's font size. A capital is ~0.7 of it; the glyph stands clear of the text. */
		private const val GLYPH_SCALE = 1.4f
		private const val KEY_TEXT_SCALE = 0.58f
		private const val FACE_TEXT_SCALE = 0.72f
		private const val SYMBOL_SCALE = 0.86f
		private const val KEY_PAD = 0.32f
		private const val GLYPH_GAP = 0.14f
		private const val LABEL_GAP = 0.36f
		private val KEY_TEXT: Color = Color.valueOf("E9ECF2FF")
		private val DPAD_TINT: Color = Color.valueOf("D3D7E0FF")
	}

	private val fontProvider: FontProvider by lazyInject()
	private val fontTracker = FontGenerationTracker()
	private var glyphs: List<MetaInputGlyph> = glyphs
	private var label: String = label
	private var fontSize: Int = fontSize

	private lateinit var labelFont: BitmapFont
	private lateinit var keyFont: BitmapFont
	private lateinit var faceFont: BitmapFont
	private val labelText = Text()
	/** One per glyph: its text laid out once, so a draw lays out nothing. */
	private val glyphTexts = ArrayList<Text>()
	private val glyphWidths = ArrayList<Float>()
	private val tint = Color()

	private val keycap: Drawable = MetaSkin.skin().getDrawable(MetaInputGlyphSkin.KEYCAP)
	private val disc: Drawable = MetaSkin.skin().getDrawable(MetaInputGlyphSkin.DISC)
	// The drawables, not their regions. An atlas rebuild keeps each drawable object and re-points it at the new page
	// (MetaSkin.rebuildAtlas); a region taken out of one at construction still points at the page it released, and
	// draws as a black box.
	private fun symbol(name: String): TextureRegionDrawable =
		MetaSkin.skin().getDrawable(name) as TextureRegionDrawable
	private val cross = symbol(MetaInputGlyphSkin.CROSS)
	private val ring = symbol(MetaInputGlyphSkin.RING)
	private val square = symbol(MetaInputGlyphSkin.SQUARE)
	private val triangle = symbol(MetaInputGlyphSkin.TRIANGLE)
	private val dpad = symbol(MetaInputGlyphSkin.DPAD)
	private val stick = symbol(MetaInputGlyphSkin.STICK)
	private val arrowUp = symbol(MetaInputGlyphSkin.ARROW_UP)

	init {
		fetchFonts()
	}

	fun setLabel(text: String) {
		if (text == label) return
		label = text
		measure()
	}

	fun setGlyphs(next: List<MetaInputGlyph>) {
		glyphs = next
		measure()
	}

	fun setFontSize(size: Int) {
		if (size == fontSize) return
		fontSize = size
		fetchFonts()
	}

	private val glyphHeight: Float get() = (fontSize * GLYPH_SCALE).roundToInt().toFloat()

	private fun fetchFonts() {
		labelFont = fontProvider.getFont(fontSize, FontType.REGULAR)
		// Regular, like the label: a key's name is text, and a game's bold face may be a display face that looks
		// nothing like the words beside it.
		keyFont = fontProvider.getFont(keyTextSize(fontSize), FontType.REGULAR)
		faceFont = fontProvider.getFont(faceTextSize(fontSize), FontType.BOLD)
		fontTracker.markFresh()
		measure()
	}

	private fun measure() {
		labelText.set(labelFont, label)
		val h = glyphHeight
		while (glyphTexts.size < glyphs.size) glyphTexts.add(Text())
		glyphWidths.clear()
		for (i in glyphs.indices) {
			val text = glyphTexts[i]
			val width = when (val glyph = glyphs[i]) {
				is MetaInputGlyph.Key -> {
					if (glyph.arrows) {
						text.clear()
						h * 1.25f
					} else {
						text.set(keyFont, glyph.text)
						max(h, text.width + h * KEY_PAD * 2f)
					}
				}
				is MetaInputGlyph.Shoulder -> {
					text.set(keyFont, glyph.text)
					max(h * 1.2f, text.width + h * KEY_PAD * 2f)
				}
				is MetaInputGlyph.Face -> {
					if (glyph.text != null) text.set(faceFont, glyph.text) else text.clear()
					h
				}
				else -> {
					text.clear()
					h
				}
			}
			glyphWidths.add(width)
		}
		invalidateHierarchy()
	}

	override fun getPrefWidth(): Float {
		val h = glyphHeight
		var width = 0f
		for (i in glyphWidths.indices) width += glyphWidths[i] + if (i > 0) h * GLYPH_GAP else 0f
		if (label.isNotEmpty()) width += (if (glyphWidths.isEmpty()) 0f else h * LABEL_GAP) + labelText.width
		return width
	}

	override fun getPrefHeight(): Float = max(glyphHeight, labelFont.lineHeight)

	override fun draw(batch: Batch, parentAlpha: Float) {
		fontTracker.refreshIfStale(this)
		validate()
		// The origin is put on the pixel grid in stage space, and every position after it is the origin plus an
		// offset rounded to whole physical pixels - so the text lands on the grid wherever the parents put this and
		// however they are transformed. Rounding local coordinates instead only moves the blur: an ancestor at a
		// fractional offset is applied by the batch after the rounding.
		snappedOrigin(origin)
		ppu = stagePixelsPerUnit()
		val h = glyphHeight
		val centreY = origin.y + px(height * 0.5f)
		val glyphBottom = origin.y + px(height * 0.5f - h * 0.5f)
		var cursor = origin.x
		val previous = batch.packedColor
		val alpha = color.a * parentAlpha
		for (i in glyphs.indices) {
			if (i > 0) cursor += px(h * GLYPH_GAP)
			drawGlyph(batch, glyphs[i], glyphTexts[i], cursor, glyphBottom, glyphWidths[i], h, alpha)
			cursor += px(glyphWidths[i])
		}
		if (label.isNotEmpty()) {
			if (glyphs.isNotEmpty()) cursor += px(h * LABEL_GAP)
			labelText.draw(batch, cursor, capTop(labelFont, centreY), tint.set(labelColor).also { it.a *= alpha })
		}
		batch.packedColor = previous
	}

	private val origin = Vector2()
	private val stageScratch = Vector2()
	/** Physical pixels per stage unit for the draw in progress. */
	private var ppu = 1f

	/** An offset in whole physical pixels. Added to a position already on the grid, it stays on the grid. */
	private fun px(offset: Float): Float = snapToPhysicalPixel(offset, ppu)

	private fun stagePixelsPerUnit(): Float {
		val stage = stage ?: return labelFont.physicalPixelsPerUnit()
		return physicalPixelsPerStageUnit(stage.width)
	}

	/**
	 * This widget's origin in its parent's coordinates, moved by less than a physical pixel so that it falls on the
	 * pixel grid in stage space. Internal so a test can hold it without a real frame buffer.
	 *
	 * <p>The correction is measured in stage units and applied in the parent's, so it is divided by the scale the
	 * ancestors apply: under a group drawn at 1.5x, a local nudge of one stage pixel moves the draw by one and a half,
	 * and the text lands off the grid again. (MetaLabel adds the stage delta unscaled, which is right only at 1x.)
	 */
	internal fun snappedOrigin(out: Vector2): Vector2 {
		val stage = stage ?: return out.set(x.roundToInt().toFloat(), y.roundToInt().toFloat())
		localToStageCoordinates(stageScratch.set(0f, 0f))
		val originX = stageScratch.x
		val originY = stageScratch.y
		localToStageCoordinates(stageScratch.set(1f, 1f))
		val scaleX = (stageScratch.x - originX).takeIf { it > 1e-4f } ?: 1f
		val scaleY = (stageScratch.y - originY).takeIf { it > 1e-4f } ?: 1f
		val horizontal = physicalPixelsPerStageUnit(stage.width)
		val vertical = if (stage.height > 0f) (Gdx.graphics.backBufferHeight / stage.height).coerceAtLeast(0.01f) else horizontal
		return out.set(
			x + (snapToPhysicalPixel(originX, horizontal) - originX) / scaleX,
			y + (snapToPhysicalPixel(originY, vertical) - originY) / scaleY,
		)
	}

	/** The y to hand `BitmapFont.draw` so the capitals are centred on [centreY]: draw takes the top of the caps. */
	private fun capTop(font: BitmapFont, centreY: Float): Float = centreY + px(font.capHeight * 0.5f)

	private fun drawGlyph(
		batch: Batch, glyph: MetaInputGlyph, text: Text,
		x: Float, y: Float, w: Float, h: Float, alpha: Float,
	) {
		val centreX = x + w * 0.5f
		val centreY = y + px(h * 0.5f)
		when (glyph) {
			is MetaInputGlyph.Key, is MetaInputGlyph.Shoulder -> {
				batch.setColor(1f, 1f, 1f, alpha)
				keycap.draw(batch, x, y, w, h)
				if (glyph is MetaInputGlyph.Key && glyph.arrows) {
					// Up and down side by side, the down one the same texture flipped: one arrow shape, two keys.
					val size = h * 0.42f
					val gap = size * 0.12f
					batch.setColor(KEY_TEXT.r, KEY_TEXT.g, KEY_TEXT.b, alpha)
					// Raised by the keycap's lip, so the symbols sit on the face and not on the whole cap.
					val lift = h * 0.04f
					val arrow = arrowUp.region
					batch.draw(arrow, centreX - size - gap, centreY - size * 0.5f + lift, size, size)
					batch.draw(arrow, centreX + gap, centreY + size * 0.5f + lift, size, -size)
				} else {
					// Nudged up by the lip: the face of the key is above its bottom edge, and the name belongs on it.
					text.draw(batch, x + px((w - text.width) * 0.5f), capTop(keyFont, centreY + px(h * 0.04f)),
						tint.set(KEY_TEXT).also { it.a = alpha })
				}
			}
			is MetaInputGlyph.Face -> {
				batch.setColor(1f, 1f, 1f, alpha)
				disc.draw(batch, x, y, h, h)
				val c = glyph.color
				if (glyph.text != null) {
					text.draw(batch, x + px((w - text.width) * 0.5f), capTop(faceFont, centreY),
						tint.set(c).also { it.a *= alpha })
				} else if (glyph.symbol != null) {
					val region = when (glyph.symbol) {
						MetaInputGlyph.Symbol.CROSS -> cross
						MetaInputGlyph.Symbol.CIRCLE -> ring
						MetaInputGlyph.Symbol.SQUARE -> square
						MetaInputGlyph.Symbol.TRIANGLE -> triangle
					}
					val s = h * SYMBOL_SCALE
					batch.setColor(c.r, c.g, c.b, c.a * alpha)
					batch.draw(region.region, centreX - s * 0.5f, centreY - s * 0.5f, s, s)
				}
			}
			MetaInputGlyph.DPad, MetaInputGlyph.Stick -> {
				batch.setColor(1f, 1f, 1f, alpha)
				disc.draw(batch, x, y, h, h)
				val s = h * SYMBOL_SCALE
				batch.setColor(DPAD_TINT.r, DPAD_TINT.g, DPAD_TINT.b, alpha)
				val region = (if (glyph === MetaInputGlyph.DPad) dpad else stick).region
				batch.draw(region, centreX - s * 0.5f, centreY - s * 0.5f, s, s)
			}
		}
	}

	override fun refreshFont() = fetchFonts()

	/**
	 * One run of text, cached with its own font cache so it can be tinted as it draws. A GlyphLayout bakes the font's
	 * colour when it is laid out, so drawing one with a colour set afterwards ignores the colour - and the fade.
	 */
	private class Text {
		private var cache: BitmapFontCache? = null
		var width = 0f
			private set

		fun set(font: BitmapFont, text: String) {
			val current = cache?.takeIf { it.font === font } ?: font.newFontCache().also { cache = it }
			width = current.setText(text, 0f, 0f).width
		}

		fun clear() {
			cache?.clear()
			width = 0f
		}

		/** [capTop] is the top of the capitals, as `BitmapFont.draw` takes it. */
		fun draw(batch: Batch, x: Float, capTop: Float, color: Color) {
			val current = cache ?: return
			current.setPosition(x, capTop)
			current.tint(color)
			current.draw(batch)
		}
	}

	override fun setStage(stage: Stage?) {
		super.setStage(stage)
		if (stage != null) fontTracker.refreshIfStale(this)
	}
}
