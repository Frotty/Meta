package de.fatox.meta.ui

import kotlin.test.Test
import kotlin.test.assertEquals

internal class MetaUiScaleTest {
	@Test
	fun `os-scaled desktop is not scaled twice`() {
		assertEquals(1f, suggestedUiScale(2560, 1440, 3840, 2160, density = 1.6f))
	}

	@Test
	fun `unscaled high-density 4k desktop gets conservative larger default`() {
		assertEquals(1.25f, suggestedUiScale(3840, 2160, 3840, 2160, density = 1.6f))
	}

	@Test
	fun `unscaled high-density 5k desktop gets larger default`() {
		assertEquals(1.5f, suggestedUiScale(5120, 2880, 5120, 2880, density = 2.2f))
	}

	@Test
	fun `ambiguous or implausible density remains at one hundred percent`() {
		assertEquals(1f, suggestedUiScale(3840, 2160, 3840, 2160, density = 1f))
		assertEquals(1f, suggestedUiScale(3840, 2160, 3840, 2160, density = 8f))
		assertEquals(1f, suggestedUiScale(2560, 1440, 2560, 1440, density = 1.8f))
	}
}
