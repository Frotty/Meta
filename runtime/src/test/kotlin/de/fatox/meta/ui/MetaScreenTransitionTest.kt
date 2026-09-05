package de.fatox.meta.ui

import de.fatox.meta.reactive.ReactiveScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class MetaScreenTransitionTest {
	private enum class Destination { GAME, COOP, QUIT }

	private class Watcher(transition: MetaScreenTransition<Destination>) {
		val scope = ReactiveScope()
		var runs = 0
		var lastSeen: Destination? = null

		init {
			scope.effect("watch transition") {
				lastSeen = transition.finished()
				runs++
			}
		}
	}

	@Test
	fun `starts idle and transparent`() {
		val transition = MetaScreenTransition<Destination>(0.5f)

		assertEquals(MetaScreenTransition.Phase.IDLE, transition.phase.value)
		assertEquals(0f, transition.alpha())
		assertNull(transition.pending.value)
		assertNull(transition.finished.value)
		assertFalse(transition.busy.value)
	}

	@Test
	fun `fade out publishes its typed destination only after the screen is covered`() {
		val transition = MetaScreenTransition<Destination>(0.5f)

		assertTrue(transition.fadeOutTo(Destination.COOP))
		assertEquals(MetaScreenTransition.Phase.FADING_OUT, transition.phase.value)
		assertEquals(Destination.COOP, transition.pending.value)
		assertNull(transition.finished.value)
		transition.advance(0.25f)
		assertEquals(0.5f, transition.alpha())
		assertNull(transition.finished.value)

		transition.advance(0.25f)
		assertEquals(MetaScreenTransition.Phase.COVERED, transition.phase.value)
		assertEquals(1f, transition.alpha())
		assertEquals(Destination.COOP, transition.finished.value)
		assertFalse(transition.busy.value)
	}

	@Test
	fun `a destination already in flight cannot be replaced`() {
		val transition = MetaScreenTransition<Destination>(0.5f)

		assertTrue(transition.fadeOutTo(Destination.GAME))
		assertFalse(transition.fadeOutTo(Destination.QUIT))
		transition.advance(0.5f)
		assertFalse(transition.fadeOutTo(Destination.QUIT))

		assertEquals(Destination.GAME, transition.finished.value)
	}

	@Test
	fun `fade out waits for an entry fade to become idle`() {
		val transition = MetaScreenTransition<Destination>(0.5f)
		transition.fadeIn()

		assertFalse(transition.fadeOutTo(Destination.GAME))
		assertNull(transition.pending.value)
		transition.advance(0.5f)
		assertTrue(transition.fadeOutTo(Destination.GAME))
	}

	@Test
	fun `finished observers wake only when the published destination changes`() {
		val transition = MetaScreenTransition<Destination>(0.5f)
		val watcher = Watcher(transition)
		assertEquals(1, watcher.runs)

		transition.fadeOutTo(Destination.GAME)
		assertEquals(1, watcher.runs, "the destination is not finished while the fade is moving")
		transition.advance(0.5f)
		assertEquals(2, watcher.runs)
		assertEquals(Destination.GAME, watcher.lastSeen)

		transition.fadeIn()
		assertEquals(3, watcher.runs)
		assertNull(watcher.lastSeen)
		watcher.scope.dispose()
	}

	@Test
	fun `hold and cancel never publish a destination`() {
		val transition = MetaScreenTransition<Destination>(0.5f)

		transition.holdCovered()
		assertEquals(MetaScreenTransition.Phase.COVERED, transition.phase.value)
		assertEquals(1f, transition.alpha())
		assertNull(transition.finished.value)

		transition.cancel()
		assertEquals(MetaScreenTransition.Phase.IDLE, transition.phase.value)
		assertEquals(0f, transition.alpha())
		assertNull(transition.pending.value)
	}

	@Test
	fun `fade in reveals the screen and ignores invalid deltas`() {
		val transition = MetaScreenTransition<Destination>(0.5f)
		transition.fadeIn()

		transition.advance(Float.NaN)
		transition.advance(-1f)
		assertEquals(1f, transition.alpha())
		assertTrue(transition.busy.value)

		transition.advance(0.5f)
		assertEquals(MetaScreenTransition.Phase.IDLE, transition.phase.value)
		assertEquals(0f, transition.alpha())
		assertFalse(transition.busy.value)
	}

	@Test
	fun `duration must be positive and finite`() {
		assertFailsWith<IllegalArgumentException> { MetaScreenTransition<Destination>(0f) }
		assertFailsWith<IllegalArgumentException> { MetaScreenTransition<Destination>(Float.POSITIVE_INFINITY) }
	}

	@Test
	fun `disposed scopes stop observing`() {
		val transition = MetaScreenTransition<Destination>(0.5f)
		val watcher = Watcher(transition)
		val runsBefore = watcher.runs
		watcher.scope.dispose()

		transition.fadeOutTo(Destination.GAME)
		transition.advance(0.5f)

		assertEquals(runsBefore, watcher.runs)
	}
}
