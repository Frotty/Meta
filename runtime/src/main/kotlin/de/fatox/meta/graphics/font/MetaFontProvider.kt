package de.fatox.meta.graphics.font

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.IntMap
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.extensions.getOrPut
import de.fatox.meta.api.extensions.use
import de.fatox.meta.api.get
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import kotlin.math.roundToInt

class MetaFontProvider : FontProvider {
	private val assetProvider: AssetProvider by lazyInject()
	private val spriteBatch: SpriteBatch by lazyInject()
	private val fontInfo: FontInfo by lazyInject()
	private val uiRenderer: UIRenderer by lazyInject()

	private val normalFontMap = IntMap<BitmapFont>()
	private val monoFontMap = IntMap<BitmapFont>()
	private val boldFontMap = IntMap<BitmapFont>()
	private val iconFontMap = IntMap<BitmapFont>()
	private val normalGenerator: FreeTypeFontGenerator = FreeTypeFontGenerator(assetProvider[fontInfo.normalFontPath])
	private val boldGenerator: FreeTypeFontGenerator = FreeTypeFontGenerator(assetProvider[fontInfo.boldFontPath])
	private val monoGenerator: FreeTypeFontGenerator = FreeTypeFontGenerator(assetProvider[fontInfo.monoFontPath])
	private val iconGenerator: FreeTypeFontGenerator = FreeTypeFontGenerator(assetProvider[fontInfo.iconFontPath])

	/** The UI scale the currently-cached fonts were rasterized for; if it changes, fonts are regenerated crisply. */
	private var generationScale = 1f

	/**
	 * Fonts dropped from the caches by a scale change. Live widgets may still reference them until they refresh
	 * (see [de.fatox.meta.ui.FontRefreshable]); the UI renderer calls [disposeOrphanedFonts] after its stage walk.
	 */
	private val orphanedFonts = Array<BitmapFont>()

	override var fontGeneration: Int = 0
		private set

	override fun getFont(size: Int, type: FontType): BitmapFont {
		refreshScaleIfChanged()
		val bitmapFonts = when(type) {
			FontType.REGULAR -> normalFontMap
			FontType.BOLD -> boldFontMap
			FontType.MONO -> monoFontMap
			FontType.ICON -> iconFontMap
		}
		// Clamp BEFORE the cache lookup so all degenerate sizes (<= 1) share one cached font entry.
		val clampedSize = if (size > 1) size else 5
		return bitmapFonts.getOrPut(clampedSize) { generateFont(clampedSize, type) }
	}

	/**
	 * When the UI scale changes, move the cached fonts aside so the next [getFont] re-rasterizes at the new physical
	 * resolution. Old fonts are still referenced by live widgets until those refresh, so they are only parked in
	 * [orphanedFonts] here and disposed later via [disposeOrphanedFonts] (after the renderer's refresh walk).
	 */
	private fun refreshScaleIfChanged() {
		val scale = physicalUiScale()
		if (scale != generationScale) {
			generationScale = scale
			fontGeneration++
			orphanFonts(normalFontMap)
			orphanFonts(boldFontMap)
			orphanFonts(monoFontMap)
			orphanFonts(iconFontMap)
		}
	}

	private fun orphanFonts(fontMap: IntMap<BitmapFont>) {
		for (entry in fontMap.entries()) orphanedFonts.add(entry.value)
		fontMap.clear()
	}

	override fun disposeOrphanedFonts() {
		// Force the scale check even if no widget called getFont since the change (e.g. an empty stage): anything
		// still cached for a stale scale is unreferenced by on-stage widgets at this point and safe to release.
		refreshScaleIfChanged()
		for (i in 0 until orphanedFonts.size) disposeFont(orphanedFonts.get(i))
		orphanedFonts.clear()
	}

	override fun dispose() {
		disposeAll(normalFontMap)
		disposeAll(boldFontMap)
		disposeAll(monoFontMap)
		disposeAll(iconFontMap)
		for (i in 0 until orphanedFonts.size) disposeFont(orphanedFonts.get(i))
		orphanedFonts.clear()
		normalGenerator.dispose()
		boldGenerator.dispose()
		monoGenerator.dispose()
		iconGenerator.dispose()
	}

	private fun disposeAll(fontMap: IntMap<BitmapFont>) {
		for (entry in fontMap.entries()) disposeFont(entry.value)
		fontMap.clear()
	}

	private fun disposeFont(font: BitmapFont) {
		// The font owns its glyph atlas textures (we never pass a shared packer); incremental FreeType font data
		// additionally holds a PixmapPacker whose pixmaps must be released separately.
		font.dispose()
		(font.data as? Disposable)?.dispose()
	}

	override fun write(x: Float, y: Float, text: String, size: Int, type: FontType) {
		spriteBatch.color = Color.WHITE
		spriteBatch.enableBlending()
		spriteBatch.shader = null
		spriteBatch.use { getFont(size, type).draw(spriteBatch, text, x, y) }
	}

	private fun generateFont(size: Int, type: FontType): BitmapFont {
		// Rasterize at physical pixels (Meta UI scale x OS/backbuffer scale) so the glyph atlas is native-resolution,
		// then scale the font down by the same factor so it still measures/lays out in logical UI units.
		val scale = generationScale.coerceAtLeast(0.01f)
		val physicalSize = (size * scale).roundToInt().coerceAtLeast(1)
		val generator = when(type) {
			FontType.REGULAR -> normalGenerator
			FontType.BOLD -> boldGenerator
			FontType.MONO -> monoGenerator
			FontType.ICON -> iconGenerator
		}
		val params = defaultFontParam(physicalSize, type)
		val font = if (type == FontType.ICON) {
			generator.generateFont(params, iconFontData())
		} else {
			generator.generateFont(params)
		}
		if (scale != 1f) {
			font.data.setScale(1f / scale)
		}
		// Integer positions round glyph positions to whole *UI units*. At scale 1 that IS the physical pixel grid,
		// so keep it for crisp, stable text. At any other scale a whole UI unit is a fractional (125%/150%) or
		// coarser (200%) number of physical pixels, so rounding would blur or mis-space glyphs; disable it there —
		// glyph advances derive from physically-integer FreeType metrics scaled by 1/scale, and MetaLabel snaps the
		// draw origin to the physical pixel grid, so unrounded positions land on physical pixels naturally.
		font.setUseIntegerPositions(scale == 1f)
		return font
	}

	private fun physicalUiScale(): Float {
		val graphics = Gdx.graphics
		val contentScale = if (graphics.width > 0) {
			graphics.backBufferWidth.toFloat() / graphics.width
		} else {
			1f
		}
		return (uiRenderer.uiScale.peek() * contentScale).coerceAtLeast(0.01f)
	}

	private fun defaultFontParam(
		requestedSize: Int,
		type: FontType,
	): FreeTypeFontGenerator.FreeTypeFontParameter {
		return FreeTypeFontGenerator.FreeTypeFontParameter().apply {
			incremental = true
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
			hinting = if (type == FontType.ICON) {
				FreeTypeFontGenerator.Hinting.Slight
			} else {
				FreeTypeFontGenerator.Hinting.Full
			}
			kerning = true
			size = requestedSize
			if (type == FontType.ICON) {
				characters = ICON_MEASURE_STRING
			}
		}
	}

	private fun iconFontData(): FreeTypeFontGenerator.FreeTypeBitmapFontData {
		return FreeTypeFontGenerator.FreeTypeBitmapFontData().apply {
			xChars = ICON_MEASURE_CHARS
			capChars = ICON_MEASURE_CHARS
		}
	}

	private companion object {
		private val ICON_MEASURE_CHARS = Character.toChars(0xEA13)
		private val ICON_MEASURE_STRING = String(ICON_MEASURE_CHARS)
	}
}
