package de.fatox.meta.reactive

import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Verifies the documented guarantees of the reactive core: automatic dependency tracking, lazy/memoized computeds,
 * glitch-free diamond updates, batched effects, untracked reads and effect cleanup/disposal.
 *
 * All nodes are local to each test; the core's shared bookkeeping (current observer, pending-effect queue) is fully
 * drained synchronously after every write, so tests don't interfere even though they share a JVM.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class ReactiveTest {

	// ----------------------------------------------------------------------------------------- signal

	@Test
	fun `signal reads and writes its value`() {
		val count = signal(1)
		assertEquals(1, count.value)
		count.value = 5
		assertEquals(5, count.value)
	}

	@Test
	fun `signal invoke operator returns the value`() {
		val name = signal("a")
		assertEquals("a", name())
		name.value = "b"
		assertEquals("b", name())
	}

	@Test
	fun `signal update transforms the current value`() {
		val count = signal(10)
		count.update { it + 1 }
		count.update { it * 2 }
		assertEquals(22, count.value)
	}

	@Test
	fun `writing an equal value is not a change`() {
		val count = signal(1)
		var runs = 0
		effect { count(); runs++ }
		assertEquals(1, runs)

		count.value = 1 // equal -> no effect re-run
		assertEquals(1, runs)

		count.value = 2 // real change -> re-run
		assertEquals(2, runs)
	}

	@Test
	fun `custom equals decides what counts as a change`() {
		// Only the integer part matters; fractional changes are ignored.
		val n = signal(1.0) { a, b -> a.toInt() == b.toInt() }
		var runs = 0
		effect { n(); runs++ }
		assertEquals(1, runs)

		n.value = 1.9 // same int part -> no change
		assertEquals(1, runs)

		n.value = 2.1 // crosses int boundary -> change
		assertEquals(2, runs)
	}

	// ----------------------------------------------------------------------------------------- effect

	@Test
	fun `effect runs immediately and re-runs on dependency change`() {
		val count = signal(0)
		val seen = mutableListOf<Int>()
		effect { seen.add(count()) }

		assertEquals(listOf(0), seen)
		count.value = 1
		count.value = 2
		assertEquals(listOf(0, 1, 2), seen)
	}

	@Test
	fun `effect tracks multiple dependencies`() {
		val a = signal(1)
		val b = signal(2)
		var sum = 0
		var runs = 0
		effect { sum = a() + b(); runs++ }

		assertEquals(3, sum)
		a.value = 10
		assertEquals(12, sum)
		b.value = 20
		assertEquals(30, sum)
		assertEquals(3, runs)
	}

	@Test
	fun `peek reads without subscribing`() {
		val count = signal(0)
		var runs = 0
		effect { count.peek(); runs++ }
		assertEquals(1, runs)

		count.value = 1 // not subscribed via peek -> no re-run
		assertEquals(1, runs)
	}

	@Test
	fun `disposed effect stops re-running`() {
		val count = signal(0)
		var runs = 0
		val handle = effect { count(); runs++ }
		assertEquals(1, runs)

		count.value = 1
		assertEquals(2, runs)

		handle.dispose()
		count.value = 2
		assertEquals(2, runs) // no further runs
	}

	@Test
	fun `effect re-subscribes to its dynamic dependencies`() {
		val useX = signal(true)
		val x = signal(1)
		val y = signal(2)
		var result = 0
		var runs = 0
		effect { result = if (useX()) x() else y(); runs++ }

		assertEquals(1, result)
		assertEquals(1, runs)

		// y is not a dependency yet.
		y.value = 20
		assertEquals(1, runs)

		x.value = 10
		assertEquals(10, result)

		// Switch the branch: now y is a dependency and x is not.
		useX.value = false
		assertEquals(20, result)

		x.value = 100 // no longer tracked
		val runsAfterX = runs
		y.value = 200
		assertEquals(200, result)
		assertEquals(runsAfterX + 1, runs)
	}

	// ----------------------------------------------------------------------------------------- onCleanup

	@Test
	fun `onCleanup runs before each re-run and on dispose`() {
		val s = signal(0)
		var cleanups = 0
		val handle = effect {
			s()
			onCleanup { cleanups++ }
		}
		assertEquals(0, cleanups)

		s.value = 1
		assertEquals(1, cleanups)
		s.value = 2
		assertEquals(2, cleanups)

		handle.dispose()
		assertEquals(3, cleanups)

		// Disposed: cleanup is not registered again, so further (no-op) state stays put.
		s.value = 3
		assertEquals(3, cleanups)
	}

	// ----------------------------------------------------------------------------------------- computed

	@Test
	fun `computed is lazy and memoized`() {
		val a = signal(2)
		var computeCount = 0
		val doubled = computed { computeCount++; a() * 2 }

		// Not evaluated until first read.
		assertEquals(0, computeCount)

		assertEquals(4, doubled.value)
		assertEquals(1, computeCount)

		// Repeated reads without a dependency change are memoized.
		assertEquals(4, doubled.value)
		assertEquals(1, computeCount)
	}

	@Test
	fun `computed recomputes after a dependency changes`() {
		val a = signal(2)
		var computeCount = 0
		val doubled = computed { computeCount++; a() * 2 }

		assertEquals(4, doubled.value)
		a.value = 5
		assertEquals(10, doubled.value)
		assertEquals(2, computeCount)
	}

	@Test
	fun `effect tracks a computed`() {
		val a = signal(1)
		val plusOne = computed { a() + 1 }
		val seen = mutableListOf<Int>()
		effect { seen.add(plusOne()) }

		assertEquals(listOf(2), seen)
		a.value = 9
		assertEquals(listOf(2, 10), seen)
	}

	@Test
	fun `computed whose result is unchanged does not re-run dependents`() {
		val a = signal(1)
		val isPositive = computed { a() >= 0 }
		var runs = 0
		effect { isPositive(); runs++ }
		assertEquals(1, runs)

		a.value = 5 // still positive -> computed result identical -> no effect re-run
		assertEquals(1, runs)

		a.value = -1 // flips -> effect re-runs
		assertEquals(2, runs)
	}

	@Test
	fun `diamond dependency recomputes the sink exactly once`() {
		val a = signal(1)
		val b = computed { a() + 1 }
		val c = computed { a() + 10 }
		var sinkComputes = 0
		val d = computed { sinkComputes++; b() + c() }

		// Observe d so the graph is live.
		var observed = 0
		effect { observed = d() }

		assertEquals(13, observed) // (1+1) + (1+10)
		assertEquals(1, sinkComputes)

		a.value = 2
		assertEquals(15, observed) // (2+1) + (2+10)
		assertEquals(2, sinkComputes) // one recompute, not two
	}

	// ----------------------------------------------------------------------------------------- batch

	@Test
	fun `batch flushes dependent effects once`() {
		val a = signal(0)
		val b = signal(0)
		var runs = 0
		effect { a(); b(); runs++ }
		assertEquals(1, runs)

		batch {
			a.value = 1
			b.value = 1
		}
		assertEquals(2, runs) // single flush despite two writes

		// Without batch each write flushes separately.
		a.value = 2
		b.value = 2
		assertEquals(4, runs)
	}

	@Test
	fun `batch returns the block result`() {
		val result = batch { 42 }
		assertEquals(42, result)
	}

	// ----------------------------------------------------------------------------------------- untracked

	@Test
	fun `untracked reads do not subscribe the running effect`() {
		val tracked = signal(0)
		val hidden = signal(0)
		var runs = 0
		effect {
			tracked()
			untracked { hidden() }
			runs++
		}
		assertEquals(1, runs)

		hidden.value = 1 // read under untracked -> no re-run
		assertEquals(1, runs)

		tracked.value = 1
		assertEquals(2, runs)
	}

	@Test
	fun `untracked still returns the current value`() {
		val s = signal(7)
		val value = untracked { s() }
		assertEquals(7, value)
	}

	// ----------------------------------------------------------------------------------------- subscribe

	@Test
	fun `subscribe fires on change but not immediately`() {
		val s = signal(0)
		var fired = 0
		s.subscribe { fired++ }
		assertEquals(0, fired) // not called on registration

		s.value = 1
		assertEquals(1, fired)
		s.value = 2
		assertEquals(2, fired)
	}

	@Test
	fun `subscribe can be disposed`() {
		val s = signal(0)
		var fired = 0
		val handle = s.subscribe { fired++ }

		s.value = 1
		assertEquals(1, fired)

		handle.dispose()
		s.value = 2
		assertEquals(1, fired) // no longer notified
	}

	// ----------------------------------------------------------------------------------------- reentrancy

	@Test
	fun `a write inside an effect propagates to dependents`() {
		val a = signal(0)
		val b = signal(0)
		// Effect 1 derives b from a.
		effect { b.value = a() * 2 }
		// Effect 2 observes b.
		var bSeen = -1
		var runs = 0
		effect { bSeen = b(); runs++ }

		assertEquals(0, bSeen)
		assertEquals(1, runs)

		a.value = 5
		assertEquals(10, bSeen) // effect 1 wrote b, effect 2 saw it in the same flush
		assertEquals(2, runs)
	}

	@Test
	fun `effects observe a consistent value with no intermediate glitch`() {
		val a = signal(1)
		val doubled = computed { a() * 2 }
		val seen = mutableListOf<Pair<Int, Int>>()
		effect { seen.add(a() to doubled()) }

		assertEquals(listOf(1 to 2), seen)
		a.value = 4
		// doubled is always consistent with a when the effect observes it.
		assertEquals(4 to 8, seen.last())
		assertTrue(seen.all { (raw, dbl) -> dbl == raw * 2 })
	}

	@Test
	fun `unobserved computed costs nothing until read`() {
		val a = signal(0)
		var computeCount = 0
		val derived = computed { computeCount++; a() }

		// Mutating the source of an unobserved computed must not eagerly recompute it.
		a.value = 1
		a.value = 2
		assertEquals(0, computeCount)

		assertEquals(2, derived.value)
		assertEquals(1, computeCount)
	}

	@Test
	fun `same reactive value can feed several independent effects`() {
		val s = signal(1)
		var x = 0
		var y = 0
		effect { x = s() * 10 }
		effect { y = s() * 100 }

		assertEquals(10, x)
		assertEquals(100, y)

		s.value = 2
		assertEquals(20, x)
		assertEquals(200, y)
	}

	@Test
	fun `disposing one effect leaves others subscribed`() {
		val s = signal(0)
		var a = 0
		var b = 0
		val handleA = effect { s(); a++ }
		effect { s(); b++ }

		s.value = 1
		assertEquals(2, a)
		assertEquals(2, b)

		handleA.dispose()
		s.value = 2
		assertEquals(2, a) // stopped
		assertEquals(3, b) // still live
	}

	@Test
	fun `a signal is usable as a read-only ReactiveValue`() {
		val s = signal("x")
		val ro: ReactiveValue<String> = s
		assertSame(s, ro)
		s.value = "y"
		assertEquals("y", ro.value) // the read-only view reflects writes through the signal
	}
}
