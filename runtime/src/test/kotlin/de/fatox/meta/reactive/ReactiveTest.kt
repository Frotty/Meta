package de.fatox.meta.reactive

import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

	@Test
	fun `basic primitive signals track reads suppress equals and batch`() {
		val enabled = booleanSignal(false)
		val count = intSignal(1)
		val timestamp = longSignal(2L)
		val ratio = doubleSignal(3.0)
		var runs = 0
		effect {
			enabled.booleanValue
			count.intValue
			timestamp.longValue
			ratio.doubleValue
			runs++
		}

		enabled.booleanValue = false
		count.intValue = 1
		timestamp.longValue = 2L
		ratio.doubleValue = 3.0
		assertEquals(1, runs)

		batch {
			enabled.booleanValue = true
			count.intValue = 4
			timestamp.longValue = 5L
			ratio.doubleValue = 6.0
		}
		assertEquals(2, runs)
		assertTrue(enabled.peekBoolean())
		assertEquals(4, count.peekInt())
		assertEquals(5L, timestamp.peekLong())
		assertEquals(6.0, ratio.peekDouble())
	}

	@Test
	fun `basic primitive signals support primitive equality and transforms`() {
		val enabled = booleanSignal(false, BooleanEquality { _, _ -> true })
		val toggle = booleanSignal(false)
		val count = intSignal(12, IntEquality { current, next -> current / 10 == next / 10 })
		val timestamp = longSignal(120L, LongEquality { current, next -> current / 100L == next / 100L })
		val ratio = doubleSignal(1.0, DoubleEquality { current, next -> kotlin.math.abs(current - next) < 0.1 })

		enabled.booleanValue = true
		toggle.updateBoolean { !it }
		count.intValue = 19
		timestamp.longValue = 199L
		ratio.doubleValue = 1.05
		assertEquals(false, enabled.peekBoolean())
		assertTrue(toggle.peekBoolean())
		assertEquals(12, count.peekInt())
		assertEquals(120L, timestamp.peekLong())
		assertEquals(1.0, ratio.peekDouble())

		count.updateInt { it + 10 }
		timestamp.updateLong { it + 100L }
		ratio.updateDouble { it + 0.2 }
		assertEquals(22, count.peekInt())
		assertEquals(220L, timestamp.peekLong())
		assertEquals(1.2, ratio.peekDouble(), 0.0001)
	}

	@Test
	fun `basic primitive signals preserve generic compatibility`() {
		val enabled = booleanSignal(false)
		val count = intSignal(1)
		val timestamp = longSignal(2L)
		val ratio = doubleSignal(3.0)
		val genericEnabled: Signal<Boolean> = enabled
		val genericCount: Signal<Int> = count
		val genericTimestamp: Signal<Long> = timestamp
		val genericRatio: Signal<Double> = ratio

		genericEnabled.value = true
		genericCount.value = 4
		genericTimestamp.value = 5L
		genericRatio.value = 6.0
		assertTrue(enabled.peekBoolean())
		assertEquals(4, count.peekInt())
		assertEquals(5L, timestamp.peekLong())
		assertEquals(6.0, ratio.peekDouble())
	}

	@Test
	fun `reactive scope owns every specialized primitive subscription`() {
		val enabled = booleanSignal(false)
		val count = intSignal(0)
		val timestamp = longSignal(0L)
		val position = floatSignal(0f)
		val ratio = doubleSignal(0.0)
		val scope = ReactiveScope()
		var callbacks = 0
		scope.subscribe(enabled) { callbacks++ }
		scope.subscribe(count) { callbacks++ }
		scope.subscribe(timestamp) { callbacks++ }
		scope.subscribe(position) { callbacks++ }
		scope.subscribe(ratio) { callbacks++ }

		enabled.booleanValue = true
		count.intValue = 1
		timestamp.longValue = 1L
		position.floatValue = 1f
		ratio.doubleValue = 1.0
		assertEquals(5, callbacks)

		scope.dispose()
		enabled.booleanValue = false
		count.intValue = 2
		timestamp.longValue = 2L
		position.floatValue = 2f
		ratio.doubleValue = 2.0
		assertEquals(5, callbacks)
	}

	@Test
	fun `basic primitive signal contracts and storage use JVM primitives`() {
		assertPrimitiveSignal(
			booleanSignal(false), BooleanSignal::class.java, "Boolean", Boolean::class.javaPrimitiveType,
			BooleanEquality::class.java, BooleanTransform::class.java,
		)
		assertPrimitiveSignal(
			intSignal(0), IntSignal::class.java, "Int", Int::class.javaPrimitiveType,
			IntEquality::class.java, IntTransform::class.java,
		)
		assertPrimitiveSignal(
			longSignal(0L), LongSignal::class.java, "Long", Long::class.javaPrimitiveType,
			LongEquality::class.java, LongTransform::class.java,
		)
		assertPrimitiveSignal(
			floatSignal(0f), FloatSignal::class.java, "Float", Float::class.javaPrimitiveType,
			FloatEquality::class.java, FloatTransform::class.java,
		)
		assertPrimitiveSignal(
			doubleSignal(0.0), DoubleSignal::class.java, "Double", Double::class.javaPrimitiveType,
			DoubleEquality::class.java, DoubleTransform::class.java,
		)
	}

	@Test
	fun `double signal exact equality handles NaN and signed zero consistently`() {
		val value = doubleSignal(Double.NaN)
		var runs = 0
		effect { value.doubleValue; runs++ }

		value.doubleValue = Double.NaN
		assertEquals(1, runs)
		value.doubleValue = -0.0
		value.doubleValue = 0.0
		assertEquals(3, runs)
	}

	@Test
	fun `float signal tracks primitive reads and suppresses equal writes`() {
		val position = floatSignal(1f)
		var observed = 0f
		var runs = 0
		effect {
			observed = position.floatValue
			runs++
		}

		assertEquals(1f, observed)
		position.floatValue = 1f
		assertEquals(1, runs)

		position.floatValue = 2.5f
		assertEquals(2.5f, observed)
		assertEquals(2, runs)
	}

	@Test
	fun `float signal custom equality and primitive update avoid generic operations`() {
		val position = floatSignal(1f, FloatEquality { current, next ->
			kotlin.math.abs(current - next) < 0.1f
		})
		var runs = 0
		effect { position.floatValue; runs++ }

		position.floatValue = 1.05f
		assertEquals(1, runs)

		position.updateFloat(FloatTransform { it + 0.2f })
		assertEquals(1.2f, position.peekFloat(), 0.0001f)
		assertEquals(2, runs)
	}

	@Test
	fun `float signal preserves generic signal compatibility`() {
		val primitive = floatSignal(3f)
		val generic: Signal<Float> = primitive

		generic.value = 4f
		assertEquals(4f, primitive.peekFloat())
		assertEquals(4f, generic.peek())
	}

	@Test
	fun `float signal batches effects and specialized subscriptions`() {
		val first = floatSignal(0f)
		val second = floatSignal(0f)
		var effectRuns = 0
		var subscriptionRuns = 0
		effect { first.floatValue + second.floatValue; effectRuns++ }
		val subscription = first.subscribe { subscriptionRuns++ }

		batch {
			first.floatValue = 1f
			second.floatValue = 2f
		}
		assertEquals(2, effectRuns)
		assertEquals(1, subscriptionRuns)

		subscription.dispose()
		first.floatValue = 3f
		assertEquals(3, effectRuns)
		assertEquals(1, subscriptionRuns)
	}

	@Test
	fun `float signal primitive transform failure leaves state unchanged`() {
		val value = floatSignal(2f)
		assertFailsWith<IllegalStateException> {
			value.updateFloat(FloatTransform { error("failed transform") })
		}
		assertEquals(2f, value.peekFloat())
	}

	@Test
	fun `float signal exact equality handles NaN and signed zero consistently`() {
		val value = floatSignal(Float.NaN)
		var runs = 0
		effect { value.floatValue; runs++ }

		value.floatValue = Float.NaN
		assertEquals(1, runs)

		value.floatValue = -0f
		value.floatValue = 0f
		assertEquals(3, runs, "Signed zeroes have distinct Float value representations")
	}

	private fun assertPrimitiveSignal(
		signal: Any,
		contract: Class<*>,
		suffix: String,
		primitiveType: Class<*>?,
		equality: Class<*>,
		transform: Class<*>,
	) {
		assertSame(primitiveType, contract.getMethod("get${suffix}Value").returnType)
		assertSame(primitiveType, contract.getMethod("set${suffix}Value", primitiveType).parameterTypes[0])
		assertSame(primitiveType, signal.javaClass.getDeclaredField("current").type)
		val equalityMethod = equality.getMethod("areEqual", primitiveType, primitiveType)
		assertSame(Boolean::class.javaPrimitiveType, equalityMethod.returnType)
		assertTrue(equalityMethod.parameterTypes.all { it == primitiveType })
		val transformMethod = transform.getMethod("apply", primitiveType)
		assertSame(primitiveType, transformMethod.returnType)
		assertSame(primitiveType, transformMethod.parameterTypes[0])
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
	fun `effect that throws during initial run releases subscriptions`() {
		val count = signal(0)
		var runs = 0
		var cleanups = 0

		assertFailsWith<IllegalStateException> {
			effect("failingInitialEffect") {
				runs++
				count()
				onCleanup { cleanups++ }
				error("initial failure")
			}
		}
		assertEquals(1, runs)
		assertEquals(1, cleanups)

		count.value = 1
		assertEquals(1, runs, "A failed initial effect must not remain subscribed")
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

	@Test
	fun `an effect disposing itself mid-run does not re-register`() {
		val s = signal(0)
		var runs = 0
		var handle: Disposable? = null
		handle = effect {
			runs++
			handle?.dispose() // null on the initial run; disposes itself on the first re-run
			s() // reads after the self-dispose must not re-subscribe
		}
		assertEquals(1, runs)

		s.value = 1 // triggers the re-run in which the effect disposes itself
		assertEquals(2, runs)

		s.value = 2 // fully unsubscribed - no further runs
		assertEquals(2, runs)
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

	@Test
	fun `multiple onCleanup callbacks run in registration order`() {
		val s = signal(0)
		val order = mutableListOf<String>()
		val handle = effect {
			s()
			onCleanup { order.add("first") }
			onCleanup { order.add("second") }
		}
		assertEquals(emptyList<String>(), order)

		s.value = 1 // before the re-run
		assertEquals(listOf("first", "second"), order)

		handle.dispose() // and again on dispose
		assertEquals(listOf("first", "second", "first", "second"), order)
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

	@Test
	fun `nested batches flush once at the outermost close`() {
		val a = signal(0)
		val b = signal(0)
		var runs = 0
		effect { a(); b(); runs++ }
		assertEquals(1, runs)

		batch {
			a.value = 1
			batch { b.value = 1 }
			assertEquals(1, runs) // the inner batch closing does not flush while the outer is still open
		}
		assertEquals(2, runs) // single flush at the outermost close
	}

	@Test
	fun `batch propagates the block's exception and still flushes prior writes`() {
		val s = signal(0)
		var seen = 0
		effect { seen = s() }

		val ex = assertFailsWith<IllegalStateException> {
			batch {
				s.value = 5
				throw IllegalStateException("block failed")
			}
		}
		assertEquals("block failed", ex.message)
		assertEquals(5, seen) // the write committed before the throw still reached effects
	}

	@Test
	fun `batch keeps the block's exception when the flush also throws`() {
		val s = signal(0)
		effect { if (s() == 5) throw UnsupportedOperationException("effect failed") }

		val ex = assertFailsWith<IllegalStateException> {
			batch {
				s.value = 5
				throw IllegalStateException("block failed")
			}
		}
		assertEquals("block failed", ex.message) // the block's exception wins...
		assertTrue(ex.suppressed.any { it is UnsupportedOperationException }) // ...the flush failure rides along
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

	@Test
	fun `subscribe callback reads do not become extra triggers`() {
		val tracked = signal(0)
		val other = signal(0)
		var fired = 0
		tracked.subscribe { other(); fired++ }

		tracked.value = 1
		assertEquals(1, fired)

		other.value = 1 // read inside the callback, but untracked -> not a trigger
		assertEquals(1, fired)

		tracked.value = 2
		assertEquals(2, fired)
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

	// ----------------------------------------------------------------------------------------- cycle safety

	@Test
	fun `a self-feeding effect settles instead of looping forever`() {
		withMaxRuns(50) {
			val a = signal(0)
			// Writing the very signal it reads would seem to loop, but an effect that is mid-run is already
			// marked dirty and does not reschedule itself, so it runs exactly once rather than freezing.
			effect("self-feeder") { a.value = a() + 1 }
			assertEquals(1, a.value)
		}
	}

	@Test
	fun `two effects ping-ponging signals trip the cycle guard`() {
		withMaxRuns(50) {
			val a = signal(0)
			val b = signal(0)
			effect("A: a-from-b") { a.value = b() + 1 }
			effect("B: b-from-a") { b.value = a() + 1 }
			// Creation converges; perturbing the loop makes each effect keep re-triggering the other.
			val ex = assertFailsWith<ReactiveCycleException> { b.value = 100 }
			assertTrue(
				ex.message!!.contains("A:") || ex.message!!.contains("B:"),
				"message should name the culprit effect",
			)
		}
	}

	@Test
	fun `the reactive system stays usable after a cycle is caught`() {
		withMaxRuns(50) {
			val a = signal(0)
			val b = signal(0)
			effect { a.value = b() + 1 }
			effect { b.value = a() + 1 }
			assertFailsWith<ReactiveCycleException> { b.value = 100 }

			// A fresh, well-behaved graph still works (the queue was cleared and the flushing flag reset).
			val c = signal(1)
			var seen = 0
			effect { seen = c() }
			assertEquals(1, seen)
			c.value = 2
			assertEquals(2, seen)
		}
	}

	@Test
	fun `a legitimate multi-step cascade does not trip the guard`() {
		withMaxRuns(50) {
			val a = signal(0)
			val b = signal(0)
			val c = signal(0)
			effect { b.value = a() + 1 } // a -> b
			effect { c.value = b() + 1 } // b -> c (chain, not a loop)
			var result = 0
			effect { result = c() }

			a.value = 10
			assertEquals(12, result) // 10 -> 11 -> 12, each effect ran once
		}
	}

	// ----------------------------------------------------------------------------------------- error recovery

	@Test
	fun `an effect whose body throws stays runnable and does not wedge others`() {
		val s = signal(0)
		var shouldThrow = true
		var badRuns = 0
		var goodRuns = 0
		effect("good") { s(); goodRuns++ }
		// Registered last -> notified (and thus flushed) FIRST, so "good" is genuinely queued behind the thrower.
		effect("bad") {
			s()
			badRuns++
			if (shouldThrow && s.peek() == 1) throw IllegalStateException("boom")
		}
		assertEquals(1, badRuns)
		assertEquals(1, goodRuns)

		val ex = assertFailsWith<IllegalStateException> { s.value = 1 }
		assertEquals("boom", ex.message)
		assertEquals(2, badRuns)
		assertEquals(2, goodRuns) // the innocent effect queued behind the throwing one still ran

		shouldThrow = false
		s.value = 2 // the throwing effect recovered: it re-runs on the next change
		assertEquals(3, badRuns)
		assertEquals(3, goodRuns)
	}

	@Test
	fun `effects unrelated to a cycle keep updating after it is caught`() {
		withMaxRuns(50) {
			val unrelated = signal(0)
			var seen = -1
			effect { seen = unrelated() }

			val a = signal(0)
			val b = signal(0)
			effect { a.value = b() + 1 }
			effect { b.value = a() + 1 }
			assertFailsWith<ReactiveCycleException> { b.value = 100 }

			unrelated.value = 7 // the pre-existing, well-behaved effect still updates
			assertEquals(7, seen)
		}
	}

	// ----------------------------------------------------------------------------------------- ReactiveScope

	@Test
	fun `scope disposes every registered subscription at once`() {
		val s = signal(0)
		val scope = ReactiveScope()
		var effectRuns = 0
		var subRuns = 0
		scope.effect { s(); effectRuns++ }
		scope.subscribe(s) { subRuns++ }

		s.value = 1
		assertEquals(2, effectRuns)
		assertEquals(1, subRuns)

		scope.dispose()
		s.value = 2
		assertEquals(2, effectRuns) // both torn down
		assertEquals(1, subRuns)
	}

	@Test
	fun `registering in an already-disposed scope disposes immediately`() {
		val scope = ReactiveScope()
		scope.dispose()

		val s = signal(0)
		var runs = 0
		scope.effect { s(); runs++ }
		assertEquals(1, runs) // the effect's initial run already happened, but it is then disposed

		s.value = 1
		assertEquals(1, runs) // no further runs
	}

	@Test
	fun `disposing a scope does not affect effects outside it`() {
		val s = signal(0)
		val scope = ReactiveScope()
		var inside = 0
		var outside = 0
		scope.effect { s(); inside++ }
		effect { s(); outside++ }

		s.value = 1
		assertEquals(2, inside)
		assertEquals(2, outside)

		scope.dispose()
		s.value = 2
		assertEquals(2, inside) // stopped
		assertEquals(3, outside) // still live
	}

	private inline fun withMaxRuns(limit: Int, block: () -> Unit) {
		val previous = maxEffectRunsPerFlush
		maxEffectRunsPerFlush = limit
		try {
			block()
		} finally {
			maxEffectRunsPerFlush = previous
		}
	}
}
