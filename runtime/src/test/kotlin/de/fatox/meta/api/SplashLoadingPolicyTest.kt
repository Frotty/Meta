package de.fatox.meta.api

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SplashLoadingPolicyTest {
	@Test
	fun `fast frames receive bounded loading time`() {
		assertEquals(2, SplashLoadingPolicy.updateBudgetMillis(1f / 120f))
		assertEquals(2, SplashLoadingPolicy.updateBudgetMillis(0f))
	}

	@Test
	fun `frames at or below target speed only advance one asset-manager step`() {
		assertEquals(0, SplashLoadingPolicy.updateBudgetMillis(1f / 60f))
		assertEquals(0, SplashLoadingPolicy.updateBudgetMillis(1f / 30f))
	}

	@Test
	fun `fade easing is clamped and smooth`() {
		assertEquals(0f, SplashLoadingPolicy.smoothStep(-1f))
		assertEquals(0.5f, SplashLoadingPolicy.smoothStep(0.5f))
		assertEquals(1f, SplashLoadingPolicy.smoothStep(2f))
	}

	@Test
	fun `spinner texture only paints the circular ring`() {
		val center = 31
		assertEquals(0f, SplashRingTexturePainter.alphaAt(center, center, 64))
		assertEquals(0f, SplashRingTexturePainter.alphaAt(4, 4, 64))
		assertTrue(SplashRingTexturePainter.alphaAt(center, 9, 64) > 0f)
	}

	@Test
	fun `spinner texture has a soft highlighted arc`() {
		val topArc = SplashRingTexturePainter.alphaAt(26, 10, 64)
		val sideRing = SplashRingTexturePainter.alphaAt(54, 31, 64)
		assertTrue(topArc > sideRing)
		assertTrue(sideRing > 0f)
	}
}
