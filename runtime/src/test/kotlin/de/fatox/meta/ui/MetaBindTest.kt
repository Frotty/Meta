package de.fatox.meta.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.reactive.signal
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Headless tests for the widget bindings that don't need a font/GL (plain [Actor] visibility/color). Text bindings
 * ([MetaLabel.bindText]) need a rasterized TTF font and are exercised at runtime, not here.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class MetaBindTest {

	@Test
	fun `bindVisible runs immediately, tracks changes, and stops on dispose`() {
		val visible = signal(true)
		val actor = Actor().apply { isVisible = false }

		val binding = actor.bindVisible { visible.value }
		assertTrue(actor.isVisible) // applied immediately

		visible.value = false
		assertFalse(actor.isVisible)
		visible.value = true
		assertTrue(actor.isVisible)

		binding.dispose()
		visible.value = false
		assertTrue(actor.isVisible) // no longer tracking
	}

	@Test
	fun `bindColor copies the value instead of aliasing the source`() {
		val tint = signal(Color(1f, 0f, 0f, 1f))
		val actor = Actor()

		actor.bindColor { tint.value }
		assertEquals(Color(1f, 0f, 0f, 1f), actor.color)

		// Mutating the actor's own color must not corrupt the signal's color (it was copied, not shared).
		actor.color.set(0f, 1f, 0f, 1f)
		assertEquals(Color(1f, 0f, 0f, 1f), tint.value)

		tint.value = Color(0f, 0f, 1f, 1f)
		assertEquals(Color(0f, 0f, 1f, 1f), actor.color)
	}

	@Test
	fun `a ReactiveScope tears down its bindings`() {
		val visible = signal(true)
		val actor = Actor()
		val scope = ReactiveScope()

		scope.bindVisible(actor) { visible.value }
		assertTrue(actor.isVisible)

		scope.dispose()
		visible.value = false
		assertTrue(actor.isVisible) // binding disposed with the scope
	}
}
