package de.fatox.meta.api.model

import kotlin.test.Test
import kotlin.test.assertEquals

internal class MetaWindowDataTest {
	@Test
	fun `anchored window position follows resized viewport edges`() {
		val data = MetaWindowData(
			width = 160f,
			height = 120f,
			horizontalAnchor = "right",
			verticalAnchor = "top",
			horizontalDistance = 24f,
			verticalDistance = 32f,
		)

		assertEquals(1016f, data.resolveHorizontal(viewportWidth = 1200f, windowWidth = 160f))
		assertEquals(648f, data.resolveVertical(viewportHeight = 800f, windowHeight = 120f))
	}

	@Test
	fun `legacy absolute window position is still used when no anchors are saved`() {
		val data = MetaWindowData(x = 420f, y = 180f, width = 160f, height = 120f)

		assertEquals(420f, data.resolveHorizontal(viewportWidth = 1200f, windowWidth = 160f))
		assertEquals(180f, data.resolveVertical(viewportHeight = 800f, windowHeight = 120f))
	}

	@Test
	fun `window position is clamped into smaller viewport`() {
		val data = MetaWindowData(
			width = 160f,
			height = 120f,
			horizontalAnchor = "right",
			verticalAnchor = "top",
			horizontalDistance = 24f,
			verticalDistance = 32f,
		)

		assertEquals(16f, data.resolveHorizontal(viewportWidth = 200f, windowWidth = 160f))
		assertEquals(0f, data.resolveVertical(viewportHeight = 150f, windowHeight = 120f))
	}
}
