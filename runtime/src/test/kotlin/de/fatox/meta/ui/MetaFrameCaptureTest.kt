package de.fatox.meta.ui

import com.badlogic.gdx.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class MetaFrameCaptureTest {
	@Test
	fun `capture bounds convert logical top-left coordinates to framebuffer bottom-left coordinates`() {
		val bounds = metaFrameCaptureBounds(
			bottomLeftScreen = Vector2(100f, 300f),
			topRightScreen = Vector2(356f, 44f),
			logicalWidth = 800,
			logicalHeight = 600,
			backBufferWidth = 1600,
			backBufferHeight = 1200,
		)

		assertEquals(MetaFrameCaptureBounds(200, 600, 512, 512), bounds)
	}

	@Test
	fun `capture bounds clamp an actor region to the physical framebuffer`() {
		val bounds = metaFrameCaptureBounds(
			bottomLeftScreen = Vector2(-20f, 630f),
			topRightScreen = Vector2(850f, -30f),
			logicalWidth = 800,
			logicalHeight = 600,
			backBufferWidth = 1600,
			backBufferHeight = 1200,
		)

		assertEquals(MetaFrameCaptureBounds(0, 0, 1600, 1200), bounds)
	}

	@Test
	fun `capture bounds reject unavailable framebuffer dimensions`() {
		assertFailsWith<IllegalArgumentException> {
			metaFrameCaptureBounds(Vector2.Zero, Vector2(10f, 10f), 0, 600, 1600, 1200)
		}
	}

	@Test
	fun `capture bounds reject a region entirely outside the framebuffer`() {
		assertFailsWith<IllegalArgumentException> {
			metaFrameCaptureBounds(Vector2(900f, 700f), Vector2(950f, 650f), 800, 600, 1600, 1200)
		}
	}
}
