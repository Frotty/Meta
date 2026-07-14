package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.utils.Align
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.api.graphics.physicalPixelsPerUnit
import de.fatox.meta.api.graphics.snapToPhysicalPixel
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

/**
 * Created by Frotty on 05.06.2016.
 */
class TextWidget(private val text: String) : Widget() {
	private val fontProvider: FontProvider by lazyInject()

	override fun draw(batch: Batch, parentAlpha: Float) {
		super.draw(batch, parentAlpha)
		val font = fontProvider.getFont(80, FontType.REGULAR)
		val pixelsPerUnit = font.physicalPixelsPerUnit()
		font.draw(
			batch,
			text,
			snapToPhysicalPixel(x, pixelsPerUnit),
			snapToPhysicalPixel(y + height / 2, pixelsPerUnit),
			width,
			Align.center,
			false,
		)
	}
}