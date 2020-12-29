package de.fatox.meta.graphics.font

import com.badlogic.gdx.graphics.g2d.BitmapFont
import de.fatox.meta.Meta.Companion.inject
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.injection.Inject

/**
 * Created by Frotty on 23.10.2016.
 */
class DistanceFieldFontProvider : FontProvider {
    private val distanceFieldFont: BitmapFont? = null

    @Inject
    private val assetProvider: AssetProvider? = null
    override fun getFont(size: Int, mono: Boolean): BitmapFont {
        return null
    }

    override fun write(x: Float, y: Float, text: String, size: Int, mono: Boolean) {}

    init {
        inject(this)
    }
}