package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

internal class ContextualBottomOverlayTest {
	@Test
	fun `screen overlay is used without a modal`() {
		val screen = Actor()
		assertSame(screen, contextualBottomOverlay(screen, hasModal = false, modalOverlay = null))
	}

	@Test
	fun `modal overlay replaces the screen overlay`() {
		val screen = Actor()
		val modal = Actor()
		assertSame(modal, contextualBottomOverlay(screen, hasModal = true, modalOverlay = modal))
	}

	@Test
	fun `modal without an overlay suppresses the screen context`() {
		assertNull(contextualBottomOverlay(Actor(), hasModal = true, modalOverlay = null))
	}
}
