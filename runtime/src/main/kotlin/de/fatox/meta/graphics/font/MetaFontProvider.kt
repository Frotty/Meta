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
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

class MetaFontProvider : FontProvider {
	private val assetProvider: AssetProvider by lazyInject()
	private val spriteBatch: SpriteBatch by lazyInject()
	private val fontInfo: FontInfo by lazyInject()

	private val normalFontMap = IntMap<BitmapFont>()
	private val monoFontMap = IntMap<BitmapFont>()
	private val normalGenerator: FreeTypeFontGenerator = FreeTypeFontGenerator(assetProvider[fontInfo.normalFontPath])
	private val monoGenerator: FreeTypeFontGenerator = FreeTypeFontGenerator(assetProvider[fontInfo.monoFontPath])

	override fun getFont(size: Int, mono: Boolean): BitmapFont {
		val bitmapFonts = if (mono) monoFontMap else normalFontMap
		return bitmapFonts.getOrPut(size) { generateFont(if (size > 1) size else 5, mono) }
	}

	override fun write(x: Float, y: Float, text: String, size: Int, mono: Boolean) {
		spriteBatch.color = Color.WHITE
		spriteBatch.enableBlending()
		spriteBatch.shader = null
		spriteBatch.use { getFont(size, mono).draw(spriteBatch, text, x, y) }
	}

	private fun generateFont(size: Int, mono: Boolean): BitmapFont {
		val param = defaultFontParam(size)
		return (if (mono) monoGenerator else normalGenerator).generateFont(param)
	}

	private fun defaultFontParam(requestedSize: Int): FreeTypeFontGenerator.FreeTypeFontParameter {
		return FreeTypeFontGenerator.FreeTypeFontParameter().apply {
			incremental = true
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
			hinting = FreeTypeFontGenerator.Hinting.Medium
			size = requestedSize
		}
	}
}
