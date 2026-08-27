package de.fatox.meta.concurrent

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Hammers the job state machine from several threads at once and asserts its invariants rather than its outputs.
 *
 * ### Why this exists
 *
 * Five rounds of review found five separate check-then-act races in this component - `isCancelled` then fail,
 * `isCancelled` then apply, `getAndSet` over a terminal state - each in a different method, each individually
 * plausible, each caught only by someone reading the code very carefully. Point tests written after the fact
 * cover the case that was found and say nothing about the one that was not.
 *
 * So this does not test a scenario. It states what must be true of *every* interleaving and then generates
 * interleavings until something disagrees. The invariants are the contract:
 *
 *  1. A job reaches exactly one terminal state.
 *  2. A completion callback runs at most once, and only for a job that did not end cancelled.
 *  3. `failedCount` equals the number of jobs that ended failed - the counter and the handles never disagree.
 *  4. A cancelled job never applied its result to its owner.
 *
 * Randomised rather than exhaustive, and seeded so a failure is reproducible: the seed is printed and can be
 * pinned. This is the cheap approximation of a model checker, and it is what turns "we fixed the races we found"
 * into "we would notice a new one".
 */
class MetaJobStateFuzzTest {
	@BeforeEach
	fun setUp() {
		MetaThreads.claimMainThread()
		MetaJobs.onJobFailure = { }
	}

	@AfterEach
	fun tearDown() {
		MetaJobs.onJobFailure = null
		MetaThreads.releaseMainThread()
	}

	@Test
	fun `no interleaving of cancel, completion and failure violates the state contract`() {
		val seed = SEED
		val random = Random(seed)
		val failuresAtStart = MetaJobs.failedCount

		var applied = 0
		var cancelledJobs = 0
		var failedJobs = 0
		var completedJobs = 0
		val appliedFor = HashSet<MetaJob>()

		repeat(ROUNDS) { round ->
			val scope = MetaJobScope()
			val jobs = ArrayList<MetaJob>()
			val appliedCount = AtomicInteger()
			val started = CountDownLatch(JOBS_PER_ROUND)
			val appliedJobs = java.util.Collections.synchronizedSet(HashSet<MetaJob>())

			for (index in 0 until JOBS_PER_ROUND) {
				// A spread of shapes: work that returns, work that throws, work that blocks long enough to be
				// interrupted, and work that finishes before anything can cancel it.
				val behaviour = random.nextInt(4)
				val holdMicros = random.nextLong(0, 400)
				lateinit var job: MetaJob
				job = scope.io(work = {
					started.countDown()
					if (holdMicros > 0) Thread.sleep(0, (holdMicros * 1_000).toInt().coerceAtMost(999_999))
					when (behaviour) {
						0 -> "value"
						1 -> throw IllegalStateException("worker failed")
						2 -> throw java.nio.channels.ClosedByInterruptException()
						else -> "value"
					}
				}) {
					appliedCount.incrementAndGet()
					appliedJobs.add(job)
					if (behaviour == 3) throw IllegalStateException("completion failed")
				}
				jobs.add(job)
			}

			started.await(10, TimeUnit.SECONDS)

			// Cancel from foreign threads *continuously, while the main thread drains*. This is the whole point:
			// the races found in review all live in the nanoseconds between a check and the act that follows it,
			// inside drainCompletions. An earlier version of this test cancelled in one burst before draining and
			// missed every one of them - the two phases never overlapped, so the window was never open.
			//
			// Several cancellers rather than one, each spinning rather than sleeping, to widen the odds that a
			// cancel lands inside a drain rather than between drains.
			val racing = java.util.concurrent.atomic.AtomicBoolean(true)
			val cancellers = (0 until CANCELLER_THREADS).map { thread ->
				Thread({
					val local = Random(seed + round * 31L + thread)
					while (racing.get()) {
						val job = jobs[local.nextInt(jobs.size)]
						if (local.nextInt(4) == 0) job.cancel()
						Thread.onSpinWait()
					}
				}, "fuzz-canceller-$round-$thread").apply { start() }
			}

			val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
			while (System.nanoTime() < deadline) {
				MetaJobs.drainCompletions(maxCompletions = random.nextInt(1, 5))
				if (jobs.none { it.isActive } && MetaJobs.pendingCompletions == 0) break
				Thread.onSpinWait()
			}
			racing.set(false)
			cancellers.forEach { it.join(10_000) }
			scope.dispose()
			MetaJobs.drainCompletions(maxCompletions = Int.MAX_VALUE)

			for (job in jobs) {
				val terminal = listOf(job.isComplete, job.isCancelled, job.isFailed).count { it }
				assertEquals(1, terminal) {
					"seed=$seed round=$round: job settled in $terminal terminal states " +
						"(complete=${job.isComplete} cancelled=${job.isCancelled} failed=${job.isFailed})"
				}
				if (job.isCancelled) {
					assertFalse(appliedJobs.contains(job)) {
						"seed=$seed round=$round: a cancelled job applied its result to its owner"
					}
				}
				if (job.isComplete) completedJobs++
				if (job.isCancelled) cancelledJobs++
				if (job.isFailed) failedJobs++
			}
			applied += appliedCount.get()
			appliedJobs.forEach { appliedFor.add(it) }
			assertEquals(appliedJobs.size, appliedCount.get()) {
				"seed=$seed round=$round: a completion ran more than once"
			}
		}

		val countedFailures = MetaJobs.failedCount - failuresAtStart
		assertEquals(failedJobs.toLong(), countedFailures) {
			"seed=$seed: failedCount reported $countedFailures but $failedJobs jobs ended failed"
		}
		// A run where nothing was cancelled, or nothing completed, would pass every assertion above while
		// exercising none of the interesting interleavings.
		assertTrue(cancelledJobs > 0) { "seed=$seed: no job was cancelled; the race was never exercised" }
		assertTrue(completedJobs + failedJobs > 0) { "seed=$seed: no job settled; nothing was tested" }
		println("[fuzz] seed=$seed complete=$completedJobs cancelled=$cancelledJobs failed=$failedJobs applied=$applied")
	}

	private companion object {
		/** Fixed so a failure reproduces. Change it locally to explore, and pin any seed that finds something. */
		const val SEED = 20260827L
		const val ROUNDS = 40
		const val JOBS_PER_ROUND = 12
		/** More than one, so a cancel can land inside a drain rather than only between drains. */
		const val CANCELLER_THREADS = 3
	}
}
