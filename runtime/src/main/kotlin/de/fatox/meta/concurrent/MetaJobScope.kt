package de.fatox.meta.concurrent

import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference

/**
 * A handle on one piece of background work.
 *
 * Cancellation here means "stop, and do not apply your result". Both halves matter: a job that finished while its
 * screen was being torn down must not run its completion, or it applies to actors that are already disposed. That
 * is the single most common async crash in a game UI, and [MetaJobScope] exists to make it structurally impossible
 * rather than something every screen has to remember.
 */
class MetaJob internal constructor() {
	private val state = AtomicReference(State.RUNNING)
	private val future = AtomicReference<Future<*>?>(null)

	/** Set by whichever lane accepted this job, so [cancel] can interrupt work already in flight. */
	internal fun attach(future: Future<*>) {
		this.future.set(future)
		// Lost the race: cancelled before the executor handed back a future, so honour it now.
		if (state.get() == State.CANCELLED) future.cancel(true)
	}

	/** Why it stopped, if it failed. Null while running, when complete, or when cancelled. */
	@Volatile
	var failure: Throwable? = null
		private set

	val isCancelled: Boolean get() = state.get() == State.CANCELLED
	val isComplete: Boolean get() = state.get() == State.COMPLETE
	val isFailed: Boolean get() = state.get() == State.FAILED

	/** True while the job may still do work or apply a result. */
	val isActive: Boolean get() = state.get() == State.RUNNING

	/**
	 * Stops the job and suppresses its result.
	 *
	 * Safe from any thread and safe to call repeatedly. Interrupts the worker if it is running, which a blocking
	 * read will notice; a tight CPU loop will not, so long compute jobs should check [isCancelled] themselves.
	 */
	fun cancel() {
		// Only RUNNING may become CANCELLED. getAndSet overwrote COMPLETE and FAILED, and that happened routinely:
		// the most recently registered job stays tracked until another registration sweeps it, so disposing a scope
		// re-labelled the job that had just succeeded. Any handle still held then stopped reporting what happened.
		if (!state.compareAndSet(State.RUNNING, State.CANCELLED)) return
		future.get()?.cancel(true)
	}

	internal fun markComplete() {
		state.compareAndSet(State.RUNNING, State.COMPLETE)
	}

	internal fun markCancelled() {
		state.set(State.CANCELLED)
	}

	/**
	 * Records [cause] and reports whether this job actually became FAILED.
	 *
	 * The return value is the only race-free answer to "should this failure be counted and reported". Checking
	 * `isCancelled` first and then failing is a TOCTOU: `cancel()` can land between the two, leaving the job
	 * CANCELLED while the caller has already decided to report. Only the CAS knows which one won.
	 *
	 * [failure] is set either way, so a job cancelled out of a blocking call still carries what it threw on the way
	 * out - useful when diagnosing a teardown, and never surfaced as an error because reporting follows the CAS.
	 */
	internal fun markFailed(cause: Throwable): Boolean {
		failure = cause
		return state.compareAndSet(State.RUNNING, State.FAILED)
	}

	private enum class State { RUNNING, COMPLETE, CANCELLED, FAILED }
}

/**
 * Owns background work for one lifecycle and cancels all of it together.
 *
 * The same shape as [de.fatox.meta.reactive.ReactiveScope], deliberately: a screen already disposes one of those,
 * so disposing this alongside it is a habit that already exists rather than a new one to teach. Where the reactive
 * scope stops effects from firing at dead actors, this stops job results from being applied to them.
 *
 * ```kotlin
 * class LevelScreen : ReactiveScreenAdapter() {
 *     private val jobs = MetaJobScope()
 *
 *     override fun onShown() {
 *         MetaJobs.io(jobs, work = { levelIndex.scan() }) { levels -> showLevels(levels) }
 *     }
 *
 *     override fun onHidden() = jobs.dispose()   // in-flight scans stop; late results never land
 * }
 * ```
 *
 * ### Why not StructuredTaskScope
 *
 * `java.util.concurrent.StructuredTaskScope` expresses this well and is usable here - Kotlin compiles against it
 * without `--enable-preview`. But it is built around a *blocking join*: the owner waits inside a `use` block until
 * its forks finish. A game's owner is a screen that must keep rendering, so there is no thread to park. The
 * discipline it encodes - scoped ownership, cancellation propagating to children - is what matters, and that is
 * what this implements. A fan-out that genuinely wants to wait (a loader assembling several files before it can
 * report anything) can open a real `StructuredTaskScope` *inside* a single [MetaJobs.io] job, where blocking is
 * free because it is a virtual thread.
 */
class MetaJobScope : de.fatox.meta.reactive.Disposable {
	private val jobs = ArrayList<MetaJob>()
	private var disposed = false

	/** True once [dispose] has run; a disposed scope cancels anything newly registered. */
	val isDisposed: Boolean get() = disposed

	/** How many jobs this scope is still tracking. Drops as finished ones are swept. */
	val activeCount: Int get() = jobs.size

	/**
	 * Takes ownership of [job] and returns it.
	 *
	 * Registering into an already-disposed scope cancels the newcomer immediately, so a result that arrives from a
	 * job submitted during teardown is still suppressed.
	 */
	fun register(job: MetaJob): MetaJob {
		if (disposed) {
			job.cancel()
			return job
		}
		// Sweep finished jobs on the way in. A screen that submits one job per frame would otherwise grow this
		// list for the life of the screen, holding every completed job it ever ran.
		sweepFinished()
		jobs.add(job)
		return job
	}

	/** Runs blocking work owned by this scope. See [MetaJobs.io]. */
	fun <T> io(work: () -> T, onDone: (T) -> Unit): MetaJob = MetaJobs.io(this, work, onDone)

	/** Runs CPU work owned by this scope. See [MetaJobs.compute]. */
	fun <T> compute(work: () -> T, onDone: (T) -> Unit): MetaJob = MetaJobs.compute(this, work, onDone)

	private fun sweepFinished() {
		var index = jobs.size - 1
		while (index >= 0) {
			if (!jobs[index].isActive) jobs.removeAt(index)
			index--
		}
	}

	/** Cancels every job this scope owns. Idempotent, and safe to call from the owner's teardown path. */
	override fun dispose() {
		if (disposed) return
		disposed = true
		// Reverse order for symmetry with ReactiveScope, so a job spawned by another is cancelled first.
		for (index in jobs.indices.reversed()) jobs[index].cancel()
		jobs.clear()
	}
}
