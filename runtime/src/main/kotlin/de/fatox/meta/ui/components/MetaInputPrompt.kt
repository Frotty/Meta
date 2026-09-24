package de.fatox.meta.ui.components

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFontCache
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.api.graphics.physicalPixelsPerStageUnit
import de.fatox.meta.api.graphics.physicalPixelsPerUnit
import de.fatox.meta.api.graphics.snapToPhysicalPixel
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.FontGenerationTracker
import de.fatox.meta.ui.FontRefreshable
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaInputGlyphFaces
import de.fatox.meta.ui.MetaType
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A button prompt: Kenney glyphs and what pressing them does - "[A] Select", "[Esc] Back".
 *
 * <p>The glyphs are Kenney's hand-drawn input prompts (see [MetaInputGlyph]); what this widget adds is how they sit
 * next to a word, which is where prompts usually go wrong:
 * <ul>
 *   <li>Size comes from the label. A glyph is a square cell [GLYPH_SCALE] times the label's font size, whatever the
 *       game's type or resolution - so a prompt at any size is the same prompt.</li>
 *   <li>The glyph is rasterized at the cell's physical pixel size and drawn 1:1, so Kenney's lines stay as crisp as
 *       the vector art. Rasterizing large and letting the batch scale it down is what made them soft.</li>
 *   <li>The cell's centre sits on the label's cap-height centre, not on the middle of its line box: a line box
 *       carries the font's ascender and descender, which are not symmetric, and centring on it leaves the words
 *       visibly above or below the button.</li>
 *   <li>The origin is snapped to the pixel grid in stage space, so the text is crisp under any parent offset or
 *       scale.</li>
 * </ul>
 */
class MetaInputPrompt @JvmOverloads constructor(
	glyphs: List<MetaInputGlyph>,
	label: String,
	fontSize: Int = MetaType.BODY,
	private val labelColor: Color = MetaColor.TEXT_MUTED,
	private val glyphColor: Color = GLYPH_TINT,
) : Widget(), FontRefreshable {
	companion object {
		/**
		 * Every text face a prompt at [fontSize] draws with, as (size, type) - the label's, which is also what a key
		 * Kenney does not draw is named in. For an application that rasterizes its faces during startup.
		 */
		@JvmStatic
		fun requiredFonts(fontSize: Int): List<Pair<Int, FontType>> = listOf(fontSize to FontType.REGULAR)

		/**
		 * Opens, rasterizes and uploads [glyphs] as a prompt at [fontSize] draws them at [physicalPixelsPerUnit], so the
		 * first frame that shows them only looks them up. Without it that frame opens the face, generates the font and
		 * uploads a page inside `draw()` - a visible hitch, and again whenever a new UI scale makes a new size.
		 *
		 * @param physicalPixelsPerUnit physical pixels per stage unit on the stage the prompt will be on; see
		 *   `physicalPixelsPerStageUnit`
		 */
		@JvmStatic
		fun prewarm(glyphs: List<MetaInputGlyph>, fontSize: Int, physicalPixelsPerUnit: Float) {
			val cellPixels = cellPixels(fontSize, physicalPixelsPerUnit)
			for (i in glyphs.indices) {
				val glyph = glyphs[i]
				if (glyph.name.isNotEmpty()) MetaInputGlyphFaces.region(glyph.set, glyph.name, cellPixels)
			}
		}

		private fun cellSize(fontSize: Int): Float = (fontSize * GLYPH_SCALE).roundToInt().toFloat()

		/** The physical height a glyph cell is rasterized at; [draw] and [prewarm] must agree on it exactly. */
		private fun cellPixels(fontSize: Int, ppu: Float): Int = (cellSize(fontSize) * ppu).roundToInt().coerceAtLeast(1)

		/** Cell height over the label's font size. Kenney's art fills nearly the whole cell. */
		private const val GLYPH_SCALE = 1.6f
		private const val GLYPH_GAP = 0.08f
		private const val LABEL_GAP = 0.3f
		/** Light, not white: the art is solid, and full white beside muted text outshouts it. */
		private val GLYPH_TINT: Color = Color.valueOf("E4E7EEFF")
	}

	private val fontProvider: FontProvider by lazyInject()
	private val fontTracker = FontGenerationTracker()
	private var glyphs: List<MetaInputGlyph> = glyphs
	private var label: String = label
	private var fontSize: Int = fontSize

	private lateinit var labelFont: BitmapFont
	private val labelText = Text()
	/** Per glyph: the text standing in for a key Kenney does not draw, or empty. */
	private val glyphTexts = ArrayList<Text>()
	private val tint = Color()
	private val origin = Vector2()
	private val stageScratch = Vector2()
	private var ppu = 1f

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

	private val cell: Float get() = cellSize(fontSize)

	private fun fetchFonts() {
		labelFont = fontProvider.getFont(fontSize, FontType.REGULAR)
		fontTracker.markFresh()
		measure()
	}

	private fun measure() {
		labelText.set(labelFont, label)
		while (glyphTexts.size < glyphs.size) glyphTexts.add(Text())
		for (i in glyphs.indices) {
			val text = glyphs[i].text
			if (glyphs[i].name.isEmpty() && text != null) glyphTexts[i].set(labelFont, text) else glyphTexts[i].clear()
		}
		invalidateHierarchy()
	}

	/** A glyph's width: its square cell, or the width of the text standing in for it. */
	private fun glyphWidth(i: Int, c: Float): Float = if (glyphTexts[i].width > 0f) glyphTexts[i].width else c

	override fun getPrefWidth(): Float {
		val c = cell
		var width = max(0, glyphs.size - 1) * c * GLYPH_GAP
		for (i in glyphs.indices) width += glyphWidth(i, c)
		if (label.isNotEmpty()) width += (if (glyphs.isEmpty()) 0f else c * LABEL_GAP) + labelText.width
		return width
	}

	override fun getPrefHeight(): Float = max(cell, labelFont.lineHeight)

	override fun draw(batch: Batch, parentAlpha: Float) {
		fontTracker.refreshIfStale(this)
		validate()
		snappedOrigin(origin)
		ppu = stagePixelsPerUnit()
		val c = cell
		val centreY = origin.y + px(height * 0.5f)
		val cellBottom = origin.y + px(height * 0.5f - c * 0.5f)
		val cellPixels = cellPixels(fontSize, ppu)
		var cursor = origin.x
		val previous = batch.packedColor
		val alpha = color.a * parentAlpha
		for (i in glyphs.indices) {
			if (i > 0) cursor += px(c * GLYPH_GAP)
			val text = glyphTexts[i]
			if (text.width > 0f) {
				text.draw(batch, cursor, centreY + px(labelFont.capHeight * 0.5f), tint.set(glyphColor).also { it.a *= alpha })
			} else {
				drawGlyph(batch, glyphs[i], cursor, cellBottom, c, cellPixels, alpha)
			}
			cursor += px(glyphWidth(i, c))
		}
		if (label.isNotEmpty()) {
			if (glyphs.isNotEmpty()) cursor += px(c * LABEL_GAP)
			labelText.draw(batch, cursor, centreY + px(labelFont.capHeight * 0.5f),
				tint.set(labelColor).also { it.a *= alpha })
		}
		batch.packedColor = previous
	}

	private fun drawGlyph(
		batch: Batch, glyph: MetaInputGlyph,
		x: Float, y: Float, c: Float, cellPixels: Int, alpha: Float,
	) {
		val region = MetaInputGlyphFaces.region(glyph.set, glyph.name, cellPixels) ?: return
		// The bitmap is the glyph's ink, rasterized for this cell: drawn at its own pixel size it is 1:1, and centred
		// in the cell it keeps Kenney's wide-and-short keys (Space, Shift) level with the square ones beside them.
		val w = region.regionWidth / ppu
		val h = region.regionHeight / ppu
		val gx = x + px((c - w) * 0.5f)
		val gy = y + px((c - h) * 0.5f)
		batch.setColor(glyphColor.r, glyphColor.g, glyphColor.b, glyphColor.a * alpha)
		batch.draw(region, gx, gy, w, h)
	}

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

	override fun refreshFont() = fetchFonts()

	override fun setStage(stage: Stage?) {
		super.setStage(stage)
		if (stage != null) fontTracker.refreshIfStale(this)
	}

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
}
