package de.fatox.meta.api

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SplashLoadingPolicyTest {
	@Test
	fun `fast frames receive bounded loading time`() {
		assertEquals(6, SplashLoadingPolicy.updateBudgetMillis(1f / 120f))
		assertEquals(6, SplashLoadingPolicy.updateBudgetMillis(0f))
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
}
