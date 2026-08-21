package de.fatox.meta.graphics.font

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import de.fatox.meta.api.graphics.PhysicalPixelDensityFont
import de.fatox.meta.api.graphics.physicalPixelsPerUnit
import de.fatox.meta.api.graphics.snapToPhysicalPixel
import kotlin.test.Test
import kotlin.test.assertEquals

internal class FontPixelSnapTest {
	@Test
	fun `snap uses the physical pixel grid at fractional UI scale`() {
		assertEquals(12f / 1.15f, snapToPhysicalPixel(10.2f, 1.15f))
		assertEquals(13f / 1.15f, snapToPhysicalPixel(11.2f, 1.15f))
	}

	@Test
	fun `zero pixel density is safely clamped`() {
		assertEquals(0f, snapToPhysicalPixel(12.4f, 0f))
	}

	@Test
	fun `explicit bitmap density is independent of logical font scaling`() {
		val font = DensityFont(1.5f)
		font.data.setScale(4f)

		assertEquals(1.5f, font.physicalPixelsPerUnit())
	}
}

private class DensityFont(
	override val physicalPixelDensity: Float,
) : BitmapFont(BitmapFont.BitmapFontData(), TextureRegion(), false), PhysicalPixelDensityFont
