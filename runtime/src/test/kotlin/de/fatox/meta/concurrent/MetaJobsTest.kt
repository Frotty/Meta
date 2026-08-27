package de.fatox.meta.concurrent

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * The guarantees the job system exists to provide, tested as guarantees rather than as plumbing.
 *
 * Each case here is a bug class that async code in a game hits sooner or later: a result landing on a torn-down
 * screen, a worker touching the scene graph, an exception vanishing on a thread nobody watches. They are worth
 * pinning precisely because none of them fails loudly on its own.
 */
class MetaJobsTest {
	@BeforeEach
	fun setUp() {
		MetaThreads.claimMainThread()
		MetaJobs.onJobFailure = null
	}

	@AfterEach
	fun tearDown() {
		MetaJobs.onJobFailure = null
		MetaThreads.releaseMainThread()
	}

	/** Drains until [predicate] holds or the timeout expires, the way a frame loop would. */
	private fun pumpUntil(timeoutMillis: Long = 5_000, predicate: () -> Boolean): Boolean {
		val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
		while (System.nanoTime() < deadline) {
			MetaJobs.drainCompletions()
			if (predicate()) return true
			Thread.sleep(1)
		}
		return predicate()
	}

	@Test
	fun `io work runs off the main thread and its result lands on it`() {
		val main = Thread.currentThread()
		val workThread = AtomicReference<Thread>()
		val doneThread = AtomicReference<Thread>()
		val value = AtomicInteger()

		MetaJobs.io(work = {
			workThread.set(Thread.currentThread())
			21 * 2
		}) {
			doneThread.set(Thread.currentThread())
			value.set(it)
		}

		assertTrue(pumpUntil { value.get() != 0 }, "The io job never completed")
		assertEquals(42, value.get())
		assertTrue(workThread.get() !== main, "Work ran on the main thread; the lane did nothing")
		assertTrue(workThread.get().isVirtual, "The io lane must use virtual threads")
		assertEquals(main, doneThread.get(), "The result must be applied on the main thread")
	}

	@Test
	fun `compute work runs on a bounded platform pool`() {
		val workThread = AtomicReference<Thread>()
		val done = AtomicBoolean()

		MetaJobs.compute(work = { workThread.set(Thread.currentThread()) }) { done.set(true) }

		assertTrue(pumpUntil { done.get() }, "The compute job never completed")
		assertFalse(workThread.get().isVirtual, "CPU work must not run on a virtual thread: it buys no parallelism")
		assertTrue(MetaJobs.computeParallelism >= 1)
	}

	@Test
	fun `a result never lands after its scope is disposed`() {
		val scope = MetaJobScope()
		val released = CountDownLatch(1)
		val applied = AtomicBoolean()

		val job = scope.io(work = {
			// Hold the worker until the test has torn the scope down, so the completion is queued against a
			// scope that is already gone - which is exactly the crash this design exists to prevent.
			released.await(5, TimeUnit.SECONDS)
			"late"
		}) { applied.set(true) }

		scope.dispose()
		released.countDown()

		// Pump well past the point the work finished; the completion must simply never be applied.
		pumpUntil(timeoutMillis = 500) { false }
		assertFalse(applied.get(), "A disposed scope applied a result to a dead owner")
		assertTrue(job.isCancelled)
	}

	@Test
	fun `a result queued before disposal is still suppressed at drain time`() {
		// The narrower half of the previous guarantee, and a genuinely different race: here the work *finishes*
		// and its completion is already sitting in the queue when the scope goes away. The post-work cancellation
		// check cannot help - it has already run - so only the check at drain time can suppress this.
		//
		// Worth its own case because the broader test above passes with that second check deleted, which makes it
		// silently unable to guard the thing it appears to be guarding.
		val scope = MetaJobScope()
		val applied = AtomicBoolean()

		scope.io(work = { "value" }) { applied.set(true) }

		// Wait for the completion to be queued, without draining it.
		val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
		while (MetaJobs.pendingCompletions == 0 && System.nanoTime() < deadline) Thread.sleep(1)
		assertTrue(MetaJobs.pendingCompletions > 0, "The completion was never queued; this test proves nothing")

		scope.dispose()
		MetaJobs.drainCompletions()

		assertFalse(applied.get(), "A completion queued before disposal was applied to a dead owner")
	}

	@Test
	fun `registering into a disposed scope cancels immediately`() {
		val scope = MetaJobScope()
		scope.dispose()
		val applied = AtomicBoolean()

		val job = scope.io(work = { "value" }) { applied.set(true) }

		pumpUntil(timeoutMillis = 300) { false }
		assertTrue(job.isCancelled, "Late async wiring must not outlive the scope it was registered into")
		assertFalse(applied.get())
	}

	@Test
	fun `a failing job reports on the main thread instead of dying silently`() {
		val main = Thread.currentThread()
		val reported = AtomicReference<Throwable>()
		val reportedOn = AtomicReference<Thread>()
		MetaJobs.onJobFailure = {
			reported.set(it)
			reportedOn.set(Thread.currentThread())
		}
		val applied = AtomicBoolean()

		MetaJobs.io<String>(work = { error("boom") }) { applied.set(true) }

		assertTrue(pumpUntil { reported.get() != null }, "The failure never surfaced")
		assertEquals("boom", reported.get().message)
		assertEquals(main, reportedOn.get(), "Failures must be reported on the main thread")
		assertFalse(applied.get(), "A failed job must not run its completion")
	}

	@Test
	fun `the drain budget bounds how much work one frame absorbs`() {
		val applied = AtomicInteger()
		val jobCount = 20
		repeat(jobCount) { MetaJobs.io(work = { it }) { applied.incrementAndGet() } }

		// Wait for the workers to finish without draining, so everything is queued at once.
		val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
		while (MetaJobs.pendingCompletions < jobCount && System.nanoTime() < deadline) Thread.sleep(1)

		val firstFrame = MetaJobs.drainCompletions(maxCompletions = 5)
		assertEquals(5, firstFrame, "A frame must apply only its budget, not the whole queue")
		assertEquals(5, applied.get())

		assertTrue(pumpUntil { applied.get() == jobCount }, "Remaining completions were dropped rather than deferred")
	}

	@Test
	fun `the thread guard names both threads and does not fire on the owner`() {
		MetaThreads.assertMain("write a signal")

		val thrown = AtomicReference<Throwable>()
		val worker = Thread({
			thrown.set(runCatching { MetaThreads.assertMain("write a signal") }.exceptionOrNull())
		}, "probe-worker")
		worker.start()
		worker.join(5_000)

		val failure = thrown.get()
		assertNotNull(failure, "Touching Meta state off the main thread must throw")
		assertTrue(failure is WrongThreadException)
		assertTrue(failure.message!!.contains("probe-worker"), "The message must name the offending thread")
		assertTrue(failure.message!!.contains("write a signal"), "The message must name the attempted action")
	}

	@Test
	fun `the guard is silent when no owner has been claimed`() {
		MetaThreads.releaseMainThread()
		val thrown = AtomicReference<Throwable>()
		val worker = Thread({
			thrown.set(runCatching { MetaThreads.assertMain("write a signal") }.exceptionOrNull())
		}, "unclaimed-worker")
		worker.start()
		worker.join(5_000)

		// Engine code runs in tests that never boot an application; a guard firing there would make Meta
		// untestable rather than safe.
		assertNull(thrown.get())
	}

	@Test
	fun `a scope does not accumulate finished jobs`() {
		val scope = MetaJobScope()
		val applied = AtomicInteger()
		repeat(30) { index ->
			scope.io(work = { index }) { applied.incrementAndGet() }
			pumpUntil(timeoutMillis = 1_000) { applied.get() == index + 1 }
		}

		// Registering sweeps finished jobs, so a screen submitting one per frame does not hold every job it ever ran.
		assertTrue(scope.activeCount < 30) { "Scope retained ${scope.activeCount} finished jobs" }
		scope.dispose()
	}

	@Test
	fun `a signal written from a worker thread throws instead of corrupting the graph`() {
		// The protection MetaThreads advertises, wired to the funnel it names. Without it a worker's write runs the
		// dependent effects on that worker, mutating scene2d from off the render thread with nothing to show for it
		// until something breaks frames later somewhere else.
		val signal = de.fatox.meta.reactive.signal(0)
		val primitive = de.fatox.meta.reactive.intSignal(0)
		val thrown = AtomicReference<Throwable>()
		val primitiveThrown = AtomicReference<Throwable>()

		val worker = Thread({
			thrown.set(runCatching { signal.value = 1 }.exceptionOrNull())
			primitiveThrown.set(runCatching { primitive.intValue = 1 }.exceptionOrNull())
		}, "signal-writer")
		worker.start()
		worker.join(5_000)

		assertTrue(thrown.get() is WrongThreadException, "A generic signal write from a worker must throw")
		assertTrue(primitiveThrown.get() is WrongThreadException, "A primitive signal write from a worker must throw")
		// The values must be untouched: the guard has to reject before mutating, not after.
		assertEquals(0, signal.value)
		assertEquals(0, primitive.intValue)
	}

	@Test
	fun `a completion that throws still reaches a terminal state`() {
		// Otherwise the job stays RUNNING forever, isActive never clears, and its scope retains it through every
		// later sweep - a leak in the code whose job is preventing leaks.
		val scope = MetaJobScope()
		MetaJobs.onJobFailure = { }
		val job = scope.io(work = { "value" }) { error("completion blew up") }

		assertTrue(pumpUntil { !job.isActive }, "Job never left RUNNING after its completion threw")
		assertTrue(job.isFailed)
		assertNotNull(job.failure)
		assertEquals("completion blew up", job.failure!!.message)
		scope.dispose()
	}

	@Test
	fun `onMainThread queues rather than running on the caller`() {
		// A helper named onMainThread that executes on whichever worker called it is worse than one that throws.
		val ranOn = AtomicReference<Thread>()
		val worker = Thread({ onMainThread { ranOn.set(Thread.currentThread()) } }, "poster")
		worker.start()
		worker.join(5_000)

		assertNull(ranOn.get(), "The block ran on the posting thread instead of waiting for the main thread")
		MetaJobs.drainCompletions()
		assertEquals(Thread.currentThread(), ranOn.get(), "The block must run on the main thread once drained")
	}

	@Test
	fun `cancellation surfacing as an IOException is not reported as a failure`() {
		// Interruption does not always arrive as InterruptedException: cancelling a job blocked in NIO throws
		// ClosedByInterruptException, an IOException. Reporting that fires the game's failure handler during
		// ordinary teardown - a spurious error every time a scope with a pending read is disposed.
		val scope = MetaJobScope()
		val reported = AtomicReference<Throwable>()
		MetaJobs.onJobFailure = { reported.set(it) }
		val entered = CountDownLatch(1)
		val failuresBefore = MetaJobs.failedCount

		val job = scope.io(work = {
			entered.countDown()
			try {
				Thread.sleep(10_000)
			} catch (interrupted: InterruptedException) {
				// Re-thrown as the checked exception NIO would raise, so the generic catch handles it.
				throw java.nio.channels.ClosedByInterruptException()
			}
			"never"
		}) { }

		assertTrue(entered.await(5, TimeUnit.SECONDS), "The job never started")
		scope.dispose()

		pumpUntil(timeoutMillis = 1_000) { false }
		assertNull(reported.get(), "Cancelling a blocked job reported a spurious failure")
		assertEquals(failuresBefore, MetaJobs.failedCount, "Cancellation must not count as a failure")
		assertTrue(job.isCancelled)
	}

	@Test
	fun `draining from a worker thread is refused`() {
		val thrown = AtomicReference<Throwable>()
		val worker = Thread({ thrown.set(runCatching { MetaJobs.drainCompletions() }.exceptionOrNull()) }, "drainer")
		worker.start()
		worker.join(5_000)
		assertTrue(thrown.get() is WrongThreadException, "Completions must only be applied on the main thread")
	}
}
