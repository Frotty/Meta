package de.fatox.meta.api.graphics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Actor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Rounds [value] (in the caller's coordinate space) to the nearest whole physical device pixel.
 *
 * [FontProvider]-generated glyph atlases are rasterized 1:1 with device pixels, so they only stay crisp when drawn
 * at a position that lands on that same pixel grid - anything fractional forces the GPU to bilinearly blend across
 * texel edges, blurring the glyph even though the underlying texture is native resolution. [physicalPixelsPerUnit]
 * is how many device pixels one unit of the caller's space covers; see [BitmapFont.physicalPixelsPerUnit] and
 * [physicalPixelsPerStageUnit] for the two ways to obtain it. Every text/glyph draw call should snap through this -
 * this is the one canonical implementation other than call sites that can't reuse it (none should exist).
 */
fun snapToPhysicalPixel(value: Float, physicalPixelsPerUnit: Float): Float {
	val ppu = physicalPixelsPerUnit.coerceAtLeast(0.01f)
	return (value * ppu).roundToInt() / ppu
}

/**
 * Physical device pixels per unit of whatever space a [FontProvider]-generated font measures in, derived from the
 * font's own render scale (it is rasterized at physical pixels, then `data.setScale(1 / scale)` so it still lays
 * out in logical units - see [FontProvider]). Self-contained: needs no `Stage`/`Gdx.graphics` lookup, so it stays
 * correct even for a style/cache that isn't currently attached to a stage.
 */
fun BitmapFont.physicalPixelsPerUnit(): Float = (1f / scaleX).coerceAtLeast(0.01f)

/** Physical device pixels per stage/UI unit, from the live backbuffer-to-logical-size ratio. */
fun physicalPixelsPerStageUnit(stageWidthInUnits: Float): Float {
	if (stageWidthInUnits <= 0f) return 1f
	return max(1f, Gdx.graphics.backBufferWidth / stageWidthInUnits)
}

/**
 * Draws this actor with its position momentarily snapped to the physical pixel grid, then restores the exact
 * original position. For wrapping a vanilla scene2d widget's own `draw()` (`TextField`, `TextArea`, ...) whose
 * internal `BitmapFontCache` positioning can't otherwise be intercepted without reimplementing it. Safe to call
 * every frame: outside of this call, `setPosition` has no observable side effect scene2d's own `draw()` doesn't
 * already redo from scratch (offsets/carets are recalculated from `x`/`y` on every call, not cached across frames).
 */
inline fun Actor.drawPixelSnapped(batch: Batch, parentAlpha: Float, physicalPixelsPerUnit: Float, superDraw: (Batch, Float) -> Unit) {
	val origX = x
	val origY = y
	setPosition(snapToPhysicalPixel(origX, physicalPixelsPerUnit), snapToPhysicalPixel(origY, physicalPixelsPerUnit))
	try {
		superDraw(batch, parentAlpha)
	} finally {
		setPosition(origX, origY)
	}
}
