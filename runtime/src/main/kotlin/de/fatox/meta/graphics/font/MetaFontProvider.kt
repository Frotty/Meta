package de.fatox.meta.graphics.font

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import java.util.EnumMap
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
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import kotlin.math.roundToInt

internal const val FONT_ATLAS_OVERSAMPLE = 2f
private val log = MetaLoggerFactory.logger {}

class MetaFontProvider : FontProvider {
	/**
	 * Resolved now, not lazily, because [prepareFaces] runs on a worker and these are what it reads.
	 *
	 * `MetaInject` keeps its singletons in unsynchronized maps and hands out `LazyThreadSafetyMode.NONE`
	 * delegates, so a worker resolving either of these while the GL thread resolves something else can build two
	 * providers or publish a half-initialized one. Resolving at construction moves that onto whichever thread
	 * builds the provider, and [de.fatox.meta.api.SplashScreen] makes sure that is the GL thread before it starts
	 * its worker. This is also what the constructor did before faces became lazy — it opened all four here, which
	 * read both of these.
	 */
	private val assetProvider: AssetProvider = inject()
	private val fontInfo: FontInfo = inject()

	/** Still lazy: only [write] and [physicalUiScale] read these, and both are GL-thread paths. */
	private val spriteBatch: SpriteBatch by lazyInject()
	private val uiRenderer: UIRenderer by lazyInject()

	private val normalFontMap = IntMap<BitmapFont>()
	private val monoFontMap = IntMap<BitmapFont>()
	private val boldFontMap = IntMap<BitmapFont>()
	private val iconFontMap = IntMap<BitmapFont>()
	/** Failed sources may still back live incremental fonts, so they cannot be disposed until all font caches are. */
	private val retiredGenerators = Array<FreeTypeFontGenerator>()
	/**
	 * Face sources, opened on first request rather than in the constructor.
	 *
	 * A slot present with a null source means "tried and failed" — the emergency font stands in and there is no
	 * point retrying every draw. A slot that is absent has not been tried.
	 *
	 * Lazy because opening a face reads and parses the whole file, and an application typically draws two of the
	 * four. A game supplying its own regular and bold used to have Meta open `RobotoMono.ttf` and
	 * `remixicon.ttf` — 684 KB of TrueType — during startup for faces it never draws.
	 */
	private val generatorSlots = EnumMap<FontType, GeneratorSlot>(FontType::class.java)

	/** Holds a source that may legitimately be null, so absence in the map still means "not yet attempted". */
	private class GeneratorSlot(val source: FontGeneratorSource?)

	/**
	 * Guards [generatorSlots]. [prepareFaces] fills it from a worker thread while the GL thread may be reading it,
	 * and opening a face is once-per-type work, so a lock costs nothing that matters.
	 */
	private val generatorLock = Any()

	private fun sourceFor(type: FontType): FontGeneratorSource? {
		synchronized(generatorLock) {
			if (disposed) return null
			generatorSlots[type]?.let { return it.source }
		}
		// Opened outside the lock. Parsing a face takes tens of milliseconds — Meta's icon face is 599 KB — and the
		// GL thread asking for a different face must not wait behind it.
		val opened = createGenerator(type)
		return synchronized(generatorLock) {
			// Disposal can land while we were parsing: the preparation worker outlives the phase that started it, so
			// an application closed during startup reaches exactly this. Release what we opened rather than storing
			// it in a map nothing will ever clear again.
			if (disposed) {
				opened?.generator?.dispose()
				return null
			}
			val existing = generatorSlots[type]
			if (existing != null) {
				// Another thread opened this type while we were parsing. Keep theirs, since callers may already hold
				// fonts from it, and release ours rather than leaking a FreeType face.
				opened?.generator?.dispose()
				existing.source
			} else {
				generatorSlots[type] = GeneratorSlot(opened)
				opened
			}
		}
	}

	/** Set under [generatorLock]; stops [sourceFor] from opening or storing a face after teardown. */
	private var disposed = false

	/**
	 * How many faces have been opened. Zero until something asks for one.
	 *
	 * Exists for the test that pins the laziness: which files the engine reads is otherwise invisible, and the
	 * regression it guards against — going back to opening all four in the constructor — is silent.
	 */
	internal val openFaceCount: Int get() = synchronized(generatorLock) { generatorSlots.size }

	/**
	 * Opens every configured face, off the GL thread.
	 *
	 * Reading and parsing a TrueType file is the expensive half of a face and needs no graphics device — Meta's own
	 * icon face is 599 KB — so [de.fatox.meta.api.SplashScreen] calls this on its preparation worker and the GL
	 * thread finds the files already parsed. Rasterizing glyphs and uploading the atlas stays on the GL thread,
	 * because the upload has to.
	 *
	 * Safe to call more than once, and safe to skip: a face not opened here is opened on first use instead.
	 */
	override fun prepareFaces() {
		val types = FontType.entries
		for (i in types.indices) sourceFor(types[i])
	}

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
		synchronized(generatorLock) {
			disposed = true
			val types = FontType.entries
			for (i in types.indices) generatorSlots[types[i]]?.source?.generator?.dispose()
			generatorSlots.clear()
		}
		for (i in 0 until retiredGenerators.size) retiredGenerators.get(i).dispose()
		retiredGenerators.clear()
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
		val source = sourceFor(type) ?: return generateBitmapFallback(size, type)
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
		// Generated fonts are incremental and retain this generator for glyphs first encountered during later draws.
		// Keep it alive until dispose(), which releases every cached font before the retired generator list.
		retiredGenerators.add(source.generator)
		setGenerator(type, null)
	}

	private fun setGenerator(type: FontType, source: FontGeneratorSource?) {
		synchronized(generatorLock) { generatorSlots[type] = GeneratorSlot(source) }
	}

	private fun createGenerator(type: FontType): FontGeneratorSource? {
		val configuredPath = when (type) {
			FontType.REGULAR -> fontInfo.normalFontPath
			FontType.BOLD -> fontInfo.boldFontPath
			FontType.MONO -> fontInfo.monoFontPath
			FontType.ICON -> fontInfo.iconFontPath
		}
		val fallbackPath = bundledPath(type)
		val role = when (type) {
			FontType.REGULAR -> "regular"
			FontType.BOLD -> "bold"
			FontType.MONO -> "monospace"
			FontType.ICON -> "icon"
		}
		return openGenerator(configuredPath, fallbackPath, role)
	}

	private fun openGenerator(
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

	private fun createBundledGenerator(type: FontType): FontGeneratorSource? =
		tryCreateGenerator(bundledPath(type))?.let { FontGeneratorSource(it, true) }

	private fun bundledPath(type: FontType): String = when (type) {
		FontType.REGULAR -> FontInfo.DEFAULT_REGULAR_FONT_PATH
		FontType.BOLD -> FontInfo.DEFAULT_BOLD_FONT_PATH
		FontType.MONO -> FontInfo.DEFAULT_MONO_FONT_PATH
		FontType.ICON -> FontInfo.DEFAULT_ICON_FONT_PATH
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
