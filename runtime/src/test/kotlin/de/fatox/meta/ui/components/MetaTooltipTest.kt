package de.fatox.meta.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class MetaTooltipTest {
	@Test
	fun `tooltip text width uses content width until max is reached`() {
		val short = MetaTooltip.resolveTextWidth(contentWidth = 72f, maxWidth = 280f)
		assertEquals(72f, short.width)
		assertFalse(short.wrap)

		val long = MetaTooltip.resolveTextWidth(contentWidth = 520f, maxWidth = 280f)
		assertEquals(280f, long.width)
		assertTrue(long.wrap)

		val uncapped = MetaTooltip.resolveTextWidth(contentWidth = 520f, maxWidth = 0f)
		assertEquals(520f, uncapped.width)
		assertFalse(uncapped.wrap)
	}
}
