package de.fatox.meta.api.model

import kotlin.test.Test
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
}
