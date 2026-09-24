package de.fatox.meta.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * The shapes button prompts are drawn from, generated into the shared skin atlas.
 *
 * <p>Generated rather than shipped, for the same reasons the rest of the skin is: they sit on the atlas page the text
 * beside them uses, so a prompt bar is one draw call; they are anti-aliased by supersampling rather than by whatever
 * a glyph font's hinting made of them at one size; and a game gets prompts that match Meta's chrome without choosing,
 * licensing and rasterizing a glyph face of its own. What varies per button - the letter, the key name - is drawn in
 * Meta's own font on top, so a prompt reads in the same type as the label beside it.
 *
 * <p>Everything but the keycap and the disc is white, and tinted where it is drawn.
 */
internal object MetaInputGlyphSkin {
	const val KEYCAP = "meta.glyph.key"
	const val DISC = "meta.glyph.disc"
	const val CROSS = "meta.glyph.cross"
	const val RING = "meta.glyph.ring"
	const val SQUARE = "meta.glyph.square"
	const val TRIANGLE = "meta.glyph.triangle"
	const val DPAD = "meta.glyph.dpad"
	const val STICK = "meta.glyph.stick"
	const val ARROW_UP = "meta.glyph.arrowUp"

	/** Symbols and the disc are square textures scaled to the glyph, so they are rasterized well above display size. */
	private const val SYMBOL_SIZE = 128
	private const val KEYCAP_SIZE = 48
	private const val KEYCAP_RADIUS = 11f
	private const val SAMPLES = 4

	val KEYCAP_FILL: Color = Color.valueOf("2B2F38FF")
	val KEYCAP_EDGE: Color = Color.valueOf("646B7AFF")
	val KEYCAP_LIP: Color = Color.valueOf("1A1D23FF")
	val DISC_FILL: Color = Color.valueOf("23262EFF")
	val DISC_EDGE: Color = Color.valueOf("5B6170FF")

	/** Queues every glyph drawable through [MetaSkin]'s own install, so they land on the atlas with the chrome. */
	fun addDrawables(pack: (String, Pixmap, IntArray?, Float, Float) -> Unit, defer: (() -> Unit) -> Boolean) {
		fun step(action: () -> Unit) {
			if (!defer(action)) action()
		}
		step { pack(KEYCAP, keycap(), intArrayOf(14, 14, 14, 18), 12f, 12f) }
		step { pack(DISC, disc(), null, 12f, 12f) }
		step { pack(CROSS, symbol(::cross), null, 1f, 1f) }
		step { pack(RING, symbol(::ring), null, 1f, 1f) }
		step { pack(SQUARE, symbol(::square), null, 1f, 1f) }
		step { pack(TRIANGLE, symbol(::triangle), null, 1f, 1f) }
		step { pack(DPAD, symbol(::dpad), null, 1f, 1f) }
		step { pack(STICK, symbol(::stick), null, 1f, 1f) }
		step { pack(ARROW_UP, symbol(::arrowUp), null, 1f, 1f) }
	}

	/**
	 * A keycap: a rounded body with a lighter rim and a darker lip along the bottom, which is what makes it read as a
	 * key pressed down onto a surface rather than a grey rectangle. Nine-patched, so one texture serves "Esc" and
	 * "Space" alike; the bottom split is taller so the lip never stretches.
	 */
	private fun keycap(): Pixmap {
		val size = KEYCAP_SIZE.toFloat()
		val lip = 3f
		val rim = 1.5f
		return paint(KEYCAP_SIZE, KEYCAP_SIZE) { x, y, out ->
			// Pixmap rows run top-down; the lip is the bottom few rows of the silhouette.
			val body = roundRect(x, y, 0f, 0f, size, size, KEYCAP_RADIUS)
			if (!body) return@paint false
			val face = roundRect(x, y, rim, rim, size - rim * 2f, size - rim * 2f - lip, KEYCAP_RADIUS - rim)
			out.set(if (face) KEYCAP_FILL else if (y > size - lip - rim) KEYCAP_LIP else KEYCAP_EDGE)
			true
		}
	}

	/** A face button's disc: dark, with a thin rim, so a coloured letter or symbol on it carries the brand. */
	private fun disc(): Pixmap {
		val c = SYMBOL_SIZE / 2f
		val r = c - 1f
		val rim = SYMBOL_SIZE * 0.045f
		return paint(SYMBOL_SIZE, SYMBOL_SIZE) { x, y, out ->
			val d = dist(x, y, c, c)
			if (d > r) return@paint false
			out.set(if (d > r - rim) DISC_EDGE else DISC_FILL)
			true
		}
	}

	private fun symbol(shape: (Float, Float, Float) -> Boolean): Pixmap =
		paint(SYMBOL_SIZE, SYMBOL_SIZE) { x, y, out ->
			if (!shape(x, y, SYMBOL_SIZE.toFloat())) return@paint false
			out.set(Color.WHITE)
			true
		}

	// Each shape takes a sample point and the texture size, and says whether the point is inside. Proportions are the
	// symbol's share of the disc it is drawn on, so a cross and a square read as the same weight.
	private fun cross(x: Float, y: Float, s: Float): Boolean {
		val u = x / s - 0.5f
		val v = y / s - 0.5f
		val half = 0.34f
		val thick = 0.055f
		return (abs(u - v) < thick * 1.414f || abs(u + v) < thick * 1.414f) && abs(u) < half && abs(v) < half
	}

	private fun ring(x: Float, y: Float, s: Float): Boolean {
		val d = dist(x, y, s / 2f, s / 2f) / s
		return d in 0.26f..0.37f
	}

	private fun square(x: Float, y: Float, s: Float): Boolean {
		val u = abs(x / s - 0.5f)
		val v = abs(y / s - 0.5f)
		val outer = 0.32f
		val inner = outer - 0.11f
		return u < outer && v < outer && (u > inner || v > inner)
	}

	private fun triangle(x: Float, y: Float, s: Float): Boolean =
		insideTriangle(x / s, y / s, 0.40f) && !insideTriangle(x / s, y / s, 0.25f)

	/** An upward equilateral triangle of circumradius [r] centred a little low, so it looks centred on the disc. */
	private fun insideTriangle(u: Float, v: Float, r: Float): Boolean {
		val cx = 0.5f
		val cy = 0.54f
		val top = cy - r
		val bottom = cy + r * 0.5f
		if (v < top || v > bottom) return false
		// Zero at the apex, the full half-base (r·√3/2) at the base.
		val halfWidth = (v - top) / (bottom - top) * r * 0.866f
		return abs(u - cx) <= halfWidth
	}

	/** A d-pad: a plus, well inside its disc, so it reads as a cross-shaped pad and not as a filled circle. */
	private fun dpad(x: Float, y: Float, s: Float): Boolean {
		val u = abs(x / s - 0.5f)
		val v = abs(y / s - 0.5f)
		val arm = 0.33f
		val width = 0.105f
		return (u < width && v < arm) || (v < width && u < arm)
	}

	/** A stick: a ring with a dot, the thumb seen from above. */
	private fun stick(x: Float, y: Float, s: Float): Boolean {
		val d = dist(x, y, s / 2f, s / 2f) / s
		return d < 0.2f || d in 0.33f..0.43f
	}

	private fun arrowUp(x: Float, y: Float, s: Float): Boolean = insideTriangle(x / s, y / s, 0.36f)

	private fun dist(x: Float, y: Float, cx: Float, cy: Float): Float {
		val dx = x - cx
		val dy = y - cy
		return sqrt(dx * dx + dy * dy)
	}

	private fun roundRect(px: Float, py: Float, left: Float, top: Float, w: Float, h: Float, radius: Float): Boolean {
		if (px < left || py < top || px > left + w || py > top + h) return false
		val r = radius.coerceAtMost(w * 0.5f).coerceAtMost(h * 0.5f)
		val cx = px.coerceIn(left + r, left + w - r)
		val cy = py.coerceIn(top + r, top + h - r)
		val dx = px - cx
		val dy = py - cy
		return dx * dx + dy * dy <= r * r
	}

	/**
	 * Rasterizes a shape by supersampling: each pixel averages [SAMPLES]² point tests, colour and coverage alike, so
	 * every edge is anti-aliased the same way whatever the shape.
	 */
	private inline fun paint(width: Int, height: Int, sample: (Float, Float, Color) -> Boolean): Pixmap {
		val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
		pixmap.setBlending(Pixmap.Blending.None)
		val tmp = Color()
		val acc = Color()
		val total = SAMPLES * SAMPLES
		for (y in 0 until height) {
			for (x in 0 until width) {
				var hits = 0
				acc.set(0f, 0f, 0f, 0f)
				for (sy in 0 until SAMPLES) {
					for (sx in 0 until SAMPLES) {
						if (sample(x + (sx + 0.5f) / SAMPLES, y + (sy + 0.5f) / SAMPLES, tmp)) {
							hits++
							acc.r += tmp.r
							acc.g += tmp.g
							acc.b += tmp.b
							acc.a += tmp.a
						}
					}
				}
				if (hits == 0) continue
				pixmap.drawPixel(x, y, Color.rgba8888(acc.r / hits, acc.g / hits, acc.b / hits, acc.a / total))
			}
		}
		return pixmap
	}
}
