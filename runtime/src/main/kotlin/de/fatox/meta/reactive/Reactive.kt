package de.fatox.meta.reactive

/**
 * A tiny, dependency-free fine-grained reactivity core - `signal` / `computed` / `effect` / `batch` / `untracked`
 * - in the spirit of SolidJS / Angular & Preact signals. It exists so the client can have ONE ground truth for a
 * piece of state and let consumers (UI labels, lists) update themselves when it changes, instead of every screen
 * manually re-querying and rebuilding.
 *
 * Semantics:
 *  - **Automatic dependency tracking.** Reading a [signal]/[computed] while a [computed] or [effect] is running
 *    subscribes that observer to it. No manual subscribe/unsubscribe.
 *  - **Lazy computed.** A [computed] only recomputes when read (or pulled by a running effect) *and* a dependency
 *    actually changed. Unobserved computeds cost nothing.
 *  - **Glitch-free.** Uses two-level invalidation (CHECK = "a dependency *might* have changed", DIRTY = "definitely
 *    changed") so a diamond `a -> {b, c} -> d` recomputes `d` once, never with a half-updated input, and an effect
 *    never runs for a derived value that re-evaluated to the same result.
 *  - **Batched effects.** Effects run synchronously after the triggering write; wrap multiple writes in [batch] to
 *    flush dependent effects once.
 *  - **Cycle-safe.** A run-away feedback loop between effects (A writes a signal that re-triggers B which writes a
 *    signal that re-triggers A...) is capped per flush: see [maxEffectRunsPerFlush] / [ReactiveCycleException].
 *    Two milder cases never run away at all: a loop in which *nothing actually changes* is impossible by
 *    construction (an effect only re-runs when a source's value really changed), and an effect that writes a
 *    signal it reads itself does not re-trigger *itself* mid-run (it is already marked dirty), so it converges.
 *
 * **Lifecycle & disposal:** there is no GC-driven teardown. A live [effect] is kept alive (and keeps the actors it
 * captures alive) by the signals it observes, so it runs until you [Disposable.dispose] it. Anything created for a
 * *transient* owner (a screen, a window that is recreated, a temporary view) MUST be disposed with that owner -
 * register it in a [ReactiveScope] and dispose the scope on the owner's teardown (`Screen.dispose`, window close,
 * etc.). Effects that live for the whole app (e.g. on a DI singleton) can simply never be disposed.
 *
 * **Threading:** NOT thread-safe - drive it from one thread (in this app, the libGDX GL/render thread). Setting a
 * signal runs its dependent effects synchronously on the calling thread, so as long as writes happen on the GL
 * thread (e.g. `RestHandler` already dispatches its callbacks there) effects may safely mutate scene2d UI.
 */

/** Read-only reactive value. Reading [value] (or invoking it) inside a [computed]/[effect] subscribes to it. */
interface ReactiveValue<out T> {
	val value: T
	operator fun invoke(): T = value
}

/** A writable reactive cell. */
interface Signal<T> : ReactiveValue<T> {
	override var value: T

	/** Read the current value WITHOUT subscribing the running observer (same as [untracked] for one read). */
	fun peek(): T

	/** Atomically set based on the current value, e.g. `count.update { it + 1 }`. */
	fun update(transform: (T) -> T) {
		value = transform(peek())
	}
}

/** Handle returned by [effect]; call [dispose] to stop it and release its subscriptions. */
fun interface Disposable {
	fun dispose()
}

/** Creates a writable [Signal] with the given [initial] value. [equals] decides whether a write is a real change. */
fun <T> signal(initial: T, equals: (T, T) -> Boolean = { a, b -> a == b }): Signal<T> = SignalNode(initial, equals)

/** Creates a lazily-evaluated, memoized derived value from [compute]. */
fun <T> computed(equals: (T, T) -> Boolean = { a, b -> a == b }, compute: () -> T): ReactiveValue<T> =
	ComputedNode(compute, equals)

/**
 * Runs [run] immediately, tracking every signal/computed it reads, then re-runs it whenever any of those change.
 * Use [onCleanup] inside [run] to release resources (listeners, actors) before each re-run and on [dispose].
 *
 * @param name optional label used only in diagnostics (e.g. the [ReactiveCycleException] message). Free to omit.
 */
fun effect(name: String? = null, run: () -> Unit): Disposable = EffectNode(run, name).also { it.runInitial() }

/** Registers a callback to run before the current [effect] re-runs and when it is disposed. No-op outside an effect. */
fun onCleanup(block: () -> Unit) {
	(currentObserver as? EffectNode)?.addCleanup(block)
}

/** Defers dependent-effect execution until [block] returns, so a burst of writes flushes effects once. */
fun <T> batch(block: () -> T): T {
	batchDepth++
	var thrown: Throwable? = null
	try {
		return block()
	} catch (t: Throwable) {
		thrown = t
		throw t
	} finally {
		batchDepth--
		if (batchDepth == 0) {
			// Writes made before a throw are already committed, so still flush - but never let a flush failure
			// swallow the block's own exception.
			try {
				flushEffects()
			} catch (flushError: Throwable) {
				if (thrown != null) thrown.addSuppressed(flushError) else throw flushError
			}
		}
	}
}

/** Runs [block] without subscribing the currently-running observer to anything read inside it. */
fun <T> untracked(block: () -> T): T {
	val prev = currentObserver
	currentObserver = null
	try {
		return block()
	} finally {
		currentObserver = prev
	}
}

/**
 * Subscribes [block] to this reactive value, running it on every change AFTER the current one (NOT immediately).
 * This is the listener/observer shape - register once, get called whenever the value changes - implemented on top
 * of [effect]. Returns a [Disposable] to unsubscribe.
 */
fun ReactiveValue<*>.subscribe(block: () -> Unit): Disposable {
	var primed = false
	return effect {
		value // read to subscribe
		// The callback runs untracked so signals it happens to read don't become extra triggers.
		if (primed) untracked(block) else primed = true
	}
}

/**
 * Thrown by a signal write (from inside [flushEffects]) when a single effect runs more than [maxEffectRunsPerFlush]
 * times during one synchronous flush - the signature of a feedback loop between effects. It is a normal exception,
 * so a game can catch it at its top-level loop / `RestHandler` callback and recover (log, reset state, show a toast)
 * instead of freezing. The offending effect's [effect] name is included when one was supplied.
 */
class ReactiveCycleException(message: String) : RuntimeException(message)

/**
 * Safety cap: the maximum number of times any one effect may execute within a single flush before a
 * [ReactiveCycleException] is raised. Generous by default - a healthy effect runs once (occasionally a few times) per
 * flush - so hitting it means a genuine cycle. Tune per game if you have a legitimate deep cascade.
 */
var maxEffectRunsPerFlush: Int = 1000

/**
 * Owns a set of [Disposable]s (effects, [subscribe]s, nested scopes) and tears them all down together. Use one per
 * transient lifecycle - a `Screen`, a recreated window, a view - and call [dispose] from that owner's teardown so its
 * effects stop running and release the actors they captured. Mirrors Angular's "effects die with their context".
 *
 * Registering after the scope is already disposed immediately disposes the newcomer, so late async wiring is safe.
 */
class ReactiveScope : Disposable {
	private val disposables = ArrayList<Disposable>()
	private var disposed = false

	/** True once [dispose] has run; a disposed scope immediately disposes anything newly [register]ed. */
	val isDisposed: Boolean get() = disposed

	/** Registers [disposable] for later teardown and returns it. */
	fun <D : Disposable> register(disposable: D): D {
		if (disposed) disposable.dispose() else disposables.add(disposable)
		return disposable
	}

	/** Creates an [effect] owned by this scope. */
	fun effect(name: String? = null, run: () -> Unit): Disposable = register(de.fatox.meta.reactive.effect(name, run))

	/** [subscribe]s to [value] within this scope. */
	fun subscribe(value: ReactiveValue<*>, block: () -> Unit): Disposable = register(value.subscribe(block))

	override fun dispose() {
		if (disposed) return
		disposed = true
		// Dispose in reverse so dependents tear down before the things they depend on.
		for (i in disposables.indices.reversed()) disposables[i].dispose()
		disposables.clear()
	}
}

// ---------------------------------------------------------------------------------------------------------------
// Internals
// ---------------------------------------------------------------------------------------------------------------

private enum class NodeState { CLEAN, CHECK, DIRTY }

private var currentObserver: ReactiveNode? = null
private var batchDepth = 0
private val pendingEffects = ArrayDeque<EffectNode>()
private var flushing = false

/** Monotonic id of the current flush; effects use it to reset their per-flush run counter (see cycle detection). */
private var flushId = 0

/**
 * Base for every node. A node can play two roles: a SOURCE (observable - [observers]/[version]) and an OBSERVER
 * (reads other sources - [sources]/[sourceVersions]/[state]). A [SignalNode] is only a source, an [EffectNode]
 * only an observer, and a [ComputedNode] is both.
 */
private abstract class ReactiveNode {
	// Source role.
	@JvmField val observers = ArrayList<ReactiveNode>()
	@JvmField var version = 0

	// Observer role.
	@JvmField val sources = ArrayList<ReactiveNode>()
	@JvmField val sourceVersions = com.badlogic.gdx.utils.IntArray(4) // primitive ints - no boxing per tracked read
	@JvmField var state = NodeState.CLEAN

	/** True once an [EffectNode] is disposed; a disposed observer must never re-register anywhere. */
	@JvmField var disposed = false

	/** Subscribe the running observer (if any) to this source. */
	fun trackRead() {
		val obs = currentObserver ?: return
		if (obs === this || obs.disposed || obs.sources.contains(this)) return
		obs.sources.add(this)
		obs.sourceVersions.add(version)
		observers.add(obs)
	}

	/** Drop all of this observer's subscriptions (called before a re-run and on dispose). */
	fun clearSources() {
		for (i in sources.indices) sources[i].observers.remove(this)
		sources.clear()
		sourceVersions.clear()
	}

	/** Tell observers this source changed ([NodeState.DIRTY]) or might have ([NodeState.CHECK]). */
	fun notifyObservers(newState: NodeState) {
		var i = observers.size - 1
		while (i >= 0) {
			// observers list only grows during a run (never shrinks mid-notify), so reverse index is safe.
			if (i < observers.size) observers[i].markStale(newState)
			i--
		}
	}

	private fun markStale(newState: NodeState) {
		val wasClean = state == NodeState.CLEAN
		if (state.ordinal < newState.ordinal) state = newState
		// onStale runs even when the state did not upgrade: an effect can be stale-but-unqueued (e.g. after its
		// body threw during a flush) and must be able to re-enqueue itself on the next source change.
		onStale(wasClean)
	}

	/** ComputedNode propagates CHECK downstream (once); EffectNode schedules itself (if not already queued). */
	protected open fun onStale(wasClean: Boolean) {}

	/** Recompute / run if stale. Default: nothing (a plain SignalNode is never stale). */
	open fun updateIfNecessary() {}

	/** Pull each source up to date, then report whether any actually changed since we last read it. */
	protected fun anySourceChanged(): Boolean {
		for (i in sources.indices) {
			sources[i].updateIfNecessary()
			if (sources[i].version != sourceVersions[i]) return true
		}
		return false
	}

	protected fun runTracked(body: () -> Unit) {
		clearSources()
		val prev = currentObserver
		currentObserver = this
		try {
			body()
		} finally {
			currentObserver = prev
		}
	}
}

private class SignalNode<T>(initial: T, private val equals: (T, T) -> Boolean) : ReactiveNode(), Signal<T> {
	private var current: T = initial

	override var value: T
		get() {
			trackRead()
			return current
		}
		set(newValue) {
			if (equals(current, newValue)) return
			current = newValue
			version++
			notifyObservers(NodeState.DIRTY)
			if (batchDepth == 0) flushEffects()
		}

	override fun peek(): T = current
}

private val UNSET = Any()

private class ComputedNode<T>(
	private val compute: () -> T,
	private val equals: (T, T) -> Boolean,
) : ReactiveNode(), ReactiveValue<T> {
	private var cached: Any? = UNSET

	init {
		state = NodeState.DIRTY // first read forces a compute
	}

	override val value: T
		get() {
			// Bring ourselves up to date FIRST, then subscribe - so the version a dependent records is our final,
			// post-recompute version. (Recording it before recompute would make every recheck look like a change.)
			updateIfNecessary()
			trackRead()
			@Suppress("UNCHECKED_CAST")
			return cached as T
		}

	override fun onStale(wasClean: Boolean) {
		// Our cached value may now be out of date - tell downstream observers to re-check (not necessarily re-run).
		if (wasClean) notifyObservers(NodeState.CHECK)
	}

	override fun updateIfNecessary() {
		when (state) {
			NodeState.CLEAN -> return
			NodeState.CHECK -> if (anySourceChanged()) recompute() else state = NodeState.CLEAN
			NodeState.DIRTY -> recompute()
		}
	}

	private fun recompute() {
		// Inlined runTracked: capturing the result through a lambda would allocate a closure + ref per recompute.
		clearSources()
		val prev = currentObserver
		currentObserver = this
		val next: T
		try {
			next = compute()
		} finally {
			currentObserver = prev
		}
		@Suppress("UNCHECKED_CAST")
		if (cached === UNSET || !equals(cached as T, next)) {
			cached = next
			version++ // observers compare against this to know we really changed
		}
		state = NodeState.CLEAN
	}
}

private class EffectNode(private val run: () -> Unit, private val name: String?) : ReactiveNode(), Disposable {
	private val cleanups = ArrayList<() -> Unit>()

	/** True while this effect sits in [pendingEffects] - tracked explicitly so it is never enqueued twice. */
	@JvmField var queued = false

	/** True while [execute] runs the body; a mid-run self-write must not reschedule the running effect. */
	private var running = false

	// Cycle detection: how many times this effect has executed within the current flush ([flushId]).
	private var lastFlushId = -1
	private var runsThisFlush = 0

	fun runInitial() {
		state = NodeState.DIRTY
		execute()
	}

	fun addCleanup(block: () -> Unit) {
		cleanups.add(block)
	}

	override fun onStale(wasClean: Boolean) {
		if (!queued && !running && !disposed) {
			queued = true
			pendingEffects.addLast(this)
		}
	}

	override fun updateIfNecessary() {
		if (disposed) return
		when (state) {
			NodeState.CLEAN -> return
			NodeState.CHECK -> if (anySourceChanged()) execute() else state = NodeState.CLEAN
			NodeState.DIRTY -> execute()
		}
	}

	private fun execute() {
		guardAgainstCycle()
		runCleanups()
		running = true
		try {
			runTracked(run)
		} finally {
			running = false
			// Reset even when the body threw, so the effect stays runnable: the next change to a source it did
			// read re-marks it stale and onStale re-enqueues it, instead of wedging it DIRTY-but-unqueued forever.
			state = NodeState.CLEAN
		}
	}

	/** Trips when this effect keeps re-executing within one flush - i.e. it is part of a feedback loop. */
	private fun guardAgainstCycle() {
		if (lastFlushId != flushId) {
			lastFlushId = flushId
			runsThisFlush = 0
		}
		if (++runsThisFlush > maxEffectRunsPerFlush) {
			throw ReactiveCycleException(
				"Reactive cycle detected: effect '${name ?: "<anonymous>"}' ran $runsThisFlush times in a single " +
					"flush (limit maxEffectRunsPerFlush=$maxEffectRunsPerFlush). Two or more effects are likely " +
					"writing signals that re-trigger one another. Name your effects to identify the culprit.",
			)
		}
	}

	private fun runCleanups() {
		for (i in cleanups.indices) cleanups[i]()
		cleanups.clear()
	}

	override fun dispose() {
		if (disposed) return
		disposed = true
		runCleanups()
		clearSources()
		if (queued) {
			queued = false
			pendingEffects.remove(this)
		}
	}
}

private fun flushEffects() {
	if (flushing) return // a write inside an effect just queues more work; the loop below drains it
	flushing = true
	flushId++ // new flush: effects reset their per-flush run counters on first execution
	var thrown: Throwable? = null
	try {
		while (pendingEffects.isNotEmpty()) {
			val next = pendingEffects.removeFirst()
			next.queued = false
			try {
				next.updateIfNecessary()
			} catch (t: Throwable) {
				// One bad effect must not wedge the innocent ones queued behind it: keep draining, remember the
				// first failure and rethrow it afterwards (later ones ride along as suppressed). A tripped cycle
				// cannot spin here - its guard rethrows without running the body, and its peers are capped too.
				if (thrown == null) thrown = t else thrown.addSuppressed(t)
			}
		}
	} finally {
		flushing = false
	}
	thrown?.let { throw it }
}
