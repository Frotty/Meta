package de.fatox.meta.test

import java.lang.management.ManagementFactory

/**
 * Measures how many bytes a block allocates on the calling thread.
 *
 * Meta's performance contract is written in allocations — `AGENTS.md` requires that `draw`, `act` and `layout`
 * allocate nothing — and until now nothing checked it. This makes that contract testable.
 *
 * Bytes allocated is the right statistic to gate CI on for the same reason a bind count is: it is **a count, not a
 * duration**. A shared runner's clock speed, its neighbours and its thermal state change how long a frame takes and
 * do not change how many bytes it allocated. An allocation-free path reports the same zero on a loaded CI box as on
 * an idle workstation.
 *
 * ```kotlin
 * val bytes = AllocationProbe.measure(warmup = 200, iterations = 50) {
 *     stage.act(1f / 60f)
 * }
 * assertTrue(bytes <= 0) { "act() allocated $bytes bytes per frame" }
 * ```
 *
 * ### What it is honest about
 *
 * The reported figure is the **minimum** across the measured iterations, not the mean. Noise here is one-sided:
 * background JIT compilation, a safepoint or a lazily-initialised class can only ever *add* bytes to an iteration,
 * never remove them. The minimum is therefore the closest available reading of what the code itself allocates, and
 * it is what makes a `<= 0` assertion stable rather than flaky.
 *
 * [warmup] is not a formality. A cold path allocates for reasons that have nothing to do with it — resolving a
 * lambda's call site, inflating a `Class`, growing a collection to its steady-state capacity — so a measurement
 * taken before the path is warm reports the cost of starting, not the cost of running.
 */
object AllocationProbe {
	private val threadMxBean = ManagementFactory.getThreadMXBean() as? com.sun.management.ThreadMXBean

	/**
	 * True when this JVM can report per-thread allocation.
	 *
	 * A HotSpot-family JVM can; the check exists so a test on an exotic one skips rather than fails, since a
	 * performance gate that cannot run is not the same thing as a performance gate that failed.
	 */
	val isSupported: Boolean get() = threadMxBean?.isThreadAllocatedMemorySupported == true

	/**
	 * Runs [block] [warmup] times, then [iterations] more, and returns the fewest bytes any measured iteration
	 * allocated on this thread.
	 *
	 * Returns -1 when [isSupported] is false, so a caller can skip rather than assert against a number that does not
	 * exist.
	 */
	fun measure(warmup: Int = 100, iterations: Int = 25, block: () -> Unit): Long {
		val bean = threadMxBean ?: return -1
		if (!bean.isThreadAllocatedMemorySupported) return -1
		if (!bean.isThreadAllocatedMemoryEnabled) bean.isThreadAllocatedMemoryEnabled = true

		require(warmup >= 0) { "Warm-up count must not be negative" }
		require(iterations > 0) { "Measurement needs at least one iteration" }

		for (index in 0 until warmup) block()

		val threadId = Thread.currentThread().threadId()
		var lowest = Long.MAX_VALUE
		for (index in 0 until iterations) {
			val before = bean.getThreadAllocatedBytes(threadId)
			block()
			val after = bean.getThreadAllocatedBytes(threadId)
			// The probe's own two readings are themselves allocation-free, so the difference is the block's.
			val used = after - before
			if (used in 0 until lowest) lowest = used
		}
		return if (lowest == Long.MAX_VALUE) -1 else lowest
	}
}
