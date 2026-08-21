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
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.error
import de.fatox.meta.api.extensions.forEachEntryReentrant
import de.fatox.meta.api.extensions.getOrPut
import de.fatox.meta.api.extensions.use
import de.fatox.meta.api.extensions.warn
import de.fatox.meta.api.get
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.api.graphics.physicalPixelsPerUnit
import de.fatox.meta.api.graphics.snapToPhysicalPixel
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import kotlin.math.roundToInt

internal const val FONT_ATLAS_OVERSAMPLE = 2f
private val log = MetaLoggerFactory.logger {}

class MetaFontProvider : FontProvider {
	private val assetProvider: AssetProvider by lazyInject()
	private val spriteBatch: SpriteBatch by lazyInject()
	private val fontInfo: FontInfo by lazyInject()
	private val uiRenderer: UIRenderer by lazyInject()

	private val normalFontMap = IntMap<BitmapFont>()
	private val monoFontMap = IntMap<BitmapFont>()
	private val boldFontMap = IntMap<BitmapFont>()
	private val iconFontMap = IntMap<BitmapFont>()
	private var normalGenerator = createGenerator(
		fontInfo.normalFontPath,
		FontInfo.DEFAULT_REGULAR_FONT_PATH,
		"regular",
	)
	private var boldGenerator = createGenerator(
		fontInfo.boldFontPath,
		FontInfo.DEFAULT_BOLD_FONT_PATH,
		"bold",
	)
	private var monoGenerator = createGenerator(
		fontInfo.monoFontPath,
		FontInfo.DEFAULT_MONO_FONT_PATH,
		"monospace",
	)
	private var iconGenerator = createGenerator(
		fontInfo.iconFontPath,
		FontInfo.DEFAULT_ICON_FONT_PATH,
		"icon",
	)

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
		fontMap.forEachEntryReentrant { _, font -> orphanedFonts.add(font) }
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
		normalGenerator?.generator?.dispose()
		boldGenerator?.generator?.dispose()
		monoGenerator?.generator?.dispose()
		iconGenerator?.generator?.dispose()
		normalGenerator = null
		boldGenerator = null
		monoGenerator = null
		iconGenerator = null
	}

	private fun disposeAll(fontMap: IntMap<BitmapFont>) {
		fontMap.forEachEntryReentrant { _, font -> disposeFont(font) }
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
		val font = getFont(size, type)
		val pixelsPerUnit = font.physicalPixelsPerUnit()
		spriteBatch.use {
			font.draw(spriteBatch, text, snapToPhysicalPixel(x, pixelsPerUnit), snapToPhysicalPixel(y, pixelsPerUnit))
		}
	}

	private fun generateFont(size: Int, type: FontType): BitmapFont {
		// Rasterize at physical pixels (Meta UI scale x OS/backbuffer scale) so the glyph atlas is native-resolution,
		// then scale the font down by the same factor so it still measures/lays out in logical UI units.
		val scale = generationScale.coerceAtLeast(0.01f)
		val rasterScale = scale * FONT_ATLAS_OVERSAMPLE
		val physicalSize = (size * rasterScale).roundToInt().coerceAtLeast(1)
		val source = when(type) {
			FontType.REGULAR -> normalGenerator
			FontType.BOLD -> boldGenerator
			FontType.MONO -> monoGenerator
			FontType.ICON -> iconGenerator
		}
		if (source == null) return generateBitmapFallback(size, type)
		val params = defaultFontParam(physicalSize, type)
		val font = generateFreeTypeFont(type, source, params)
			?: return generateBitmapFallback(size, type)
		configureGeneratedFont(font, rasterScale)
		return font
	}

	private fun generateFreeTypeFont(
		type: FontType,
		source: FontGeneratorSource,
		params: FreeTypeFontGenerator.FreeTypeFontParameter,
	): BitmapFont? {
		try {
			return if (type == FontType.ICON) {
				source.generator.generateFont(params, iconFontData())
			} else {
				source.generator.generateFont(params)
			}
		} catch (failure: RuntimeException) {
			disableGenerator(type, source)
			if (!source.isBundledFallback) {
				log.warn {
					"Could not rasterize configured ${type.name.lowercase()} font; retrying Meta's bundled face"
				}
				val fallback = createBundledGenerator(type)
				setGenerator(type, fallback)
				if (fallback != null) return generateFreeTypeFont(type, fallback, params)
			}
			log.error(failure) {
				"Could not generate the ${type.name.lowercase()} font; " +
					"using Meta's code-embedded emergency font for this session"
			}
			return null
		}
	}

	private fun configureGeneratedFont(font: BitmapFont, rasterScale: Float) {
		if (rasterScale != 1f) {
			font.data.setScale(1f / rasterScale)
		}
		for (i in 0 until font.regions.size) {
			font.regions[i].texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
		}
		// The atlas is intentionally oversampled, so a whole logical unit is not one atlas texel. Let the
		// physical-pixel snap helpers control the draw origin instead of rounding glyph positions to logical units.
		font.setUseIntegerPositions(false)
	}

	private fun generateBitmapFallback(size: Int, type: FontType): BitmapFont {
		if (type == FontType.ICON) {
			log.warn { "The icon font is unavailable; icons may render as missing glyphs" }
		}
		return createEmergencyBitmapFont(size, physicalUiScale())
	}

	private fun disableGenerator(type: FontType, source: FontGeneratorSource) {
		source.generator.dispose()
		setGenerator(type, null)
	}

	private fun setGenerator(type: FontType, source: FontGeneratorSource?) {
		when(type) {
			FontType.REGULAR -> normalGenerator = source
			FontType.BOLD -> boldGenerator = source
			FontType.MONO -> monoGenerator = source
			FontType.ICON -> iconGenerator = source
		}
	}

	private fun createGenerator(
		configuredPath: String,
		fallbackPath: String,
		role: String,
	): FontGeneratorSource? {
		tryCreateGenerator(configuredPath)?.let {
			return FontGeneratorSource(it, configuredPath == fallbackPath)
		}
		if (configuredPath != fallbackPath) {
			log.warn {
				"Could not load configured $role font '$configuredPath'; using Meta fallback '$fallbackPath'"
			}
			tryCreateGenerator(fallbackPath)?.let { return FontGeneratorSource(it, true) }
		}
		log.error {
			"Could not load $role font '$configuredPath' or Meta fallback '$fallbackPath'; " +
				"using Meta's code-embedded emergency font"
		}
		return null
	}

	private fun createBundledGenerator(type: FontType): FontGeneratorSource? {
		val path = when(type) {
			FontType.REGULAR -> FontInfo.DEFAULT_REGULAR_FONT_PATH
			FontType.BOLD -> FontInfo.DEFAULT_BOLD_FONT_PATH
			FontType.MONO -> FontInfo.DEFAULT_MONO_FONT_PATH
			FontType.ICON -> FontInfo.DEFAULT_ICON_FONT_PATH
		}
		return tryCreateGenerator(path)?.let { FontGeneratorSource(it, true) }
	}

	private fun tryCreateGenerator(path: String): FreeTypeFontGenerator? {
		return try {
			FreeTypeFontGenerator(assetProvider[path])
		} catch (_: RuntimeException) {
			null
		}
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
			// FreeType rasterizes anti-aliased coverage into the oversampled atlas. Linear filtering preserves that
			// coverage when the atlas is reduced back to logical size or enlarged by the world camera.
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

private class FontGeneratorSource(
	val generator: FreeTypeFontGenerator,
	val isBundledFallback: Boolean,
)
