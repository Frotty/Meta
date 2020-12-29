package de.fatox.meta.graphics.font

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.utils.IntMap
import de.fatox.meta.Meta
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.injection.Inject

class MetaFontProvider @Inject
constructor() : FontProvider {
	@Inject
	private lateinit var assetProvider: AssetProvider
	@Inject
	private lateinit var spriteBatch: SpriteBatch
	@Inject
	private lateinit var fontInfo: FontInfo

	private val normalFontMap = IntMap<BitmapFont>()
	private val monoFontMap = IntMap<BitmapFont>()
	private var normalGenerator: FreeTypeFontGenerator
	private var monoGenerator: FreeTypeFontGenerator

    init {
        Meta.inject(this)
		normalGenerator = FreeTypeFontGenerator(assetProvider[fontInfo.normalFontPath, FileHandle::class.java])
		monoGenerator = FreeTypeFontGenerator(assetProvider[fontInfo.monoFontPath, FileHandle::class.java])
	}

    override fun getFont(size: Int, mono: Boolean): BitmapFont {
        if (!(if (mono) monoFontMap else normalFontMap).containsKey(size)) {
            generateFont(if (size > 1) size else 5, mono)
        }
        return (if (mono) monoFontMap else normalFontMap).get(size)
    }

    override fun write(x: Float, y: Float, text: String, size: Int, mono: Boolean) {
        spriteBatch.color = Color.WHITE
        spriteBatch.enableBlending()
        spriteBatch.shader = null
        spriteBatch.begin()
        getFont(size, mono).draw(spriteBatch, text, x, y)
        spriteBatch.end()
    }

    private fun generateFont(size: Int, mono: Boolean) {
        val param = defaultFontParam()
        param.size = size
        val value = (if (mono) monoGenerator else normalGenerator).generateFont(param)
		(if (mono) monoFontMap else normalFontMap).put(size, value)
    }

    private fun defaultFontParam(): FreeTypeFontGenerator.FreeTypeFontParameter {
        val param = FreeTypeFontGenerator.FreeTypeFontParameter()
        param.incremental = true
        param.minFilter = Texture.TextureFilter.Linear
        param.magFilter = Texture.TextureFilter.Linear
        param.hinting = FreeTypeFontGenerator.Hinting.Medium
        return param
    }
}
