package de.fatox.meta.graphics.font

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
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

	override fun getFont(size: Int, type: FontType): BitmapFont {
		refreshScaleIfChanged()
		val bitmapFonts = when(type) {
			FontType.REGULAR -> normalFontMap
			FontType.BOLD -> boldFontMap
			FontType.MONO -> monoFontMap
			FontType.ICON -> iconFontMap
		}
		return bitmapFonts.getOrPut(size) { generateFont(if (size > 1) size else 5, type) }
	}

	/**
	 * When the UI scale changes, drop the cached fonts so the next [getFont] re-rasterizes at the new physical
	 * resolution. (Old fonts are still referenced by live labels until those rebuild - we don't dispose them here.)
	 */
	private fun refreshScaleIfChanged() {
		val scale = uiRenderer.uiScale.peek()
		if (scale != generationScale) {
			generationScale = scale
			normalFontMap.clear()
			boldFontMap.clear()
			monoFontMap.clear()
			iconFontMap.clear()
		}
	}

	override fun write(x: Float, y: Float, text: String, size: Int, type: FontType) {
		spriteBatch.color = Color.WHITE
		spriteBatch.enableBlending()
		spriteBatch.shader = null
		spriteBatch.use { getFont(size, type).draw(spriteBatch, text, x, y) }
	}

	private fun generateFont(size: Int, type: FontType): BitmapFont {
		// Rasterize at PHYSICAL pixels (logical size x UI scale) so the glyph atlas is native-resolution, then scale
		// the font DOWN by 1/scale so it still measures/lays out at the logical size. The scaled viewport then draws
		// it 1:1 instead of magnifying a low-res atlas -> crisp text on HiDPI instead of blurry upscaling. At scale
		// 1.0 this is a no-op (physical == logical, no setScale), so non-HiDPI rendering is unchanged.
		val scale = generationScale.coerceAtLeast(0.01f)
		val physicalSize = (size * scale).roundToInt().coerceAtLeast(1)
		val font = (when(type) {
			FontType.REGULAR -> normalGenerator
			FontType.BOLD -> boldGenerator
			FontType.MONO -> monoGenerator
			FontType.ICON -> iconGenerator
		}).generateFont(defaultFontParam(physicalSize))
		if (scale != 1f) {
			font.data.setScale(1f / scale)
		}
		// Integer positions snap every glyph advance to whole pixels. That looks especially bad on kerning-sensitive
		// pairs ("VA", "To") and gets worse once UI scale is applied by the viewport.
		font.setUseIntegerPositions(false)
		return font
	}

	private fun defaultFontParam(requestedSize: Int): FreeTypeFontGenerator.FreeTypeFontParameter {
		return FreeTypeFontGenerator.FreeTypeFontParameter().apply {
			incremental = true
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
			hinting = FreeTypeFontGenerator.Hinting.Slight
			kerning = true
			size = requestedSize
		}
	}
}
