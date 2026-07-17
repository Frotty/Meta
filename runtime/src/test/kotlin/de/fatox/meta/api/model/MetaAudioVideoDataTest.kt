package de.fatox.meta.api.model

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class MetaAudioVideoDataTest {
	@Test
	fun `fullscreen uses borderless windowed presentation`() {
		assertTrue(MetaAudioVideoData(fullscreen = true).usesBorderlessPresentation())
	}

	@Test
	fun `legacy borderless preference uses the same presentation`() {
		assertTrue(MetaAudioVideoData(borderless = true).usesBorderlessPresentation())
	}

	@Test
	fun `decorated window mode is not a borderless presentation`() {
		assertFalse(MetaAudioVideoData().usesBorderlessPresentation())
	}

	@Test
	fun `tiny persisted splash bounds are raised to the supported window minimum`() {
		assertContentEquals(
			intArrayOf(32, 32, MetaWindowBounds.MIN_WIDTH, MetaWindowBounds.MIN_HEIGHT),
			MetaWindowBounds.sanitize(32, 32, 238, 127, 0, 0, 1920, 1080),
		)
	}

	@Test
	fun `valid persisted window bounds are restored unchanged`() {
		assertContentEquals(
			intArrayOf(180, 120, 1280, 720),
			MetaWindowBounds.sanitize(180, 120, 1280, 720, 0, 0, 1920, 1080),
		)
	}

	@Test
	fun `offscreen and oversized persisted bounds are clamped onto their monitor`() {
		assertContentEquals(
			intArrayOf(2080, 0, 1840, 1000),
			MetaWindowBounds.sanitize(5000, -500, 4000, 2000, 2000, 0, 1920, 1080),
		)
	}

	@Test
	fun `small monitors remain usable without invalid clamp ranges`() {
		assertContentEquals(
			intArrayOf(40, 60, 720, 480),
			MetaWindowBounds.initial(0, 0, 800, 600),
		)
	}
}
