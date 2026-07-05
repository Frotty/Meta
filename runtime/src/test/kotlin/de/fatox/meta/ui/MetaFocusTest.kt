package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class MetaFocusTest {
	private class FocusableActor(
		override val handlesMetaFocus: Boolean = true,
	) : Actor(), MetaFocusable {
		var focused = false
		val events = ArrayList<Boolean>()

		override fun setMetaFocused(focused: Boolean) {
			this.focused = focused
			events.add(focused)
		}
	}

	@Test
	fun `assign focuses a Meta actor`() {
		val actor = FocusableActor()

		val focused = MetaFocus.assign(null, actor)

		assertSame(actor, focused)
		assertTrue(actor.focused)
		assertEquals(listOf(true), actor.events)
	}

	@Test
	fun `assign switches focus from old actor to new actor`() {
		val old = FocusableActor().apply { setMetaFocused(true) }
		old.events.clear()
		val next = FocusableActor()

		val focused = MetaFocus.assign(old, next)

		assertSame(next, focused)
		assertFalse(old.focused)
		assertTrue(next.focused)
		assertEquals(listOf(false), old.events)
		assertEquals(listOf(true), next.events)
	}

	@Test
	fun `assigning same actor does not churn focus state`() {
		val actor = FocusableActor().apply { setMetaFocused(true) }
		actor.events.clear()

		val focused = MetaFocus.assign(actor, actor)

		assertSame(actor, focused)
		assertTrue(actor.focused)
		assertEquals(emptyList(), actor.events)
	}

	@Test
	fun `assign null clears previous native focus`() {
		val actor = FocusableActor().apply { setMetaFocused(true) }
		actor.events.clear()

		val focused = MetaFocus.assign(actor, null)

		assertNull(focused)
		assertFalse(actor.focused)
		assertEquals(listOf(false), actor.events)
	}

	@Test
	fun `Meta focus can opt out for overlay fallback`() {
		val actor = FocusableActor(handlesMetaFocus = false)

		val focused = MetaFocus.assign(null, actor)

		assertSame(actor, focused)
		assertFalse(actor.focused)
		assertEquals(emptyList(), actor.events)
		assertFalse(MetaFocus.isHandledByActor(actor))
		assertFalse(MetaFocus.isHandledByActor(Actor()))
		assertFalse(MetaFocus.isHandledByActor(null))
	}
}
