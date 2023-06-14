package de.fatox.meta.graphics.font

import com.badlogic.gdx.graphics.g2d.BitmapFont
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

/**
 * Created by Frotty on 23.10.2016.
 */
class DistanceFieldFontProvider : FontProvider {
	private val distanceFieldFont: BitmapFont? = TODO()

	private val assetProvider: AssetProvider by lazyInject()

	override fun getFont(size: Int, type: FontType): BitmapFont {
		TODO("Not yet implemented")
	}

	override fun write(x: Float, y: Float, text: String, size: Int, type: FontType) {
		TODO("Not yet implemented")
	}
}