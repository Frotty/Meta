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
    private val bitmapFontMap = IntMap<BitmapFont>()
    private val generator: FreeTypeFontGenerator by lazy {
        FreeTypeFontGenerator(assetProvider.get("Montserrat.ttf", FileHandle::class.java))
    }
    @Inject
    private lateinit var assetProvider: AssetProvider
    @Inject
    private lateinit var spriteBatch: SpriteBatch

    init {
        Meta.inject(this)
    }

    override fun getFont(size: Int): BitmapFont {
        if (!bitmapFontMap.containsKey(size)) {
            generateFont(if (size > 1) size else 5)
        }
        return bitmapFontMap.get(size)
    }

    override fun write(x: Float, y: Float, text: String, size: Int) {
        spriteBatch.color = Color.WHITE
        spriteBatch.enableBlending()
        spriteBatch.shader = null
        spriteBatch.begin()
        getFont(size).draw(spriteBatch, text, x, y)
        spriteBatch.end()
    }

    private fun generateFont(size: Int) {
        val param = defaultFontParam()
        param.size = size
        val value = generator.generateFont(param)
        bitmapFontMap.put(size, value)
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
