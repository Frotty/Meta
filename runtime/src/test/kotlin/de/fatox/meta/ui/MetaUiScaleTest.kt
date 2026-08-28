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

	@Test
	fun `windows display scaling is followed when the framebuffer is not scaled`() {
		// The case that was broken. A DPI-aware process on Windows gets a framebuffer exactly the size it asked
		// for, so backbuffer/logical is 1.0 whatever the display settings say - and the old heuristic fell through
		// to "no scaling". The window was then physically tiny on a dense laptop panel while every other
		// application on the desktop honoured the user's 150%.
		assertEquals(1.5f, suggestedUiScale(1280, 900, 1280, 900, density = 1.18f, osContentScale = 1.5f))
		assertEquals(2f, suggestedUiScale(1920, 1080, 1920, 1080, density = 1.18f, osContentScale = 2f))
	}

	@Test
	fun `retina scaling is not applied twice`() {
		// macOS expresses the same intent the opposite way: the framebuffer is already double the window, so the
		// application is drawing more pixels without being asked. GLFW reports a content scale there too, and
		// following it as well would double an interface that is already correct.
		assertEquals(1f, suggestedUiScale(1440, 900, 2880, 1800, density = 2.2f, osContentScale = 2f))
	}

	@Test
	fun `an unscaled display still falls through to the resolution heuristic`() {
		// No OS scaling to follow, so the previous behaviour has to survive unchanged.
		assertEquals(1f, suggestedUiScale(1920, 1080, 1920, 1080, density = 1.18f, osContentScale = 1f))
		assertEquals(1.25f, suggestedUiScale(3840, 2160, 3840, 2160, density = 2f, osContentScale = 1f))
		assertEquals(1.5f, suggestedUiScale(5120, 2880, 5120, 2880, density = 2.4f, osContentScale = 1f))
	}

	@Test
	fun `an implausible platform reading cannot make the interface unusable`() {
		assertEquals(4f, suggestedUiScale(1280, 900, 1280, 900, density = 1.18f, osContentScale = 99f))
		assertEquals(1f, suggestedUiScale(1280, 900, 1280, 900, density = 1.18f, osContentScale = 0f))
		assertEquals(1f, suggestedUiScale(1280, 900, 1280, 900, density = 1.18f, osContentScale = Float.NaN))
	}
}
