package de.fatox.meta

import kotlin.test.Test
import kotlin.test.assertEquals

internal class ExceptionHandlerTest {
	@Test
	fun `application storage names use one case independent directory name`() {
		assertEquals("oxrox", canonicalAppStorageName("OxRox"))
		assertEquals("oxrox", canonicalAppStorageName("oxrox"))
		assertEquals("my_game-1.2", canonicalAppStorageName("My Game-1.2"))
	}
}
