package de.fatox.meta.graphics.font

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
}
