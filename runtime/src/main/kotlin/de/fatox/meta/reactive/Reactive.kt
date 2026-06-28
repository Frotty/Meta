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
 *
 * **Threading:** NOT thread-safe - drive it from one thread (in this app, the libGDX GL/render thread). Setting a
 * signal runs its dependent effects synchronously on the calling thread, so as long as writes happen on the GL
 * thread (e.g. `RestHandler` already dispatches its callbacks there) effects may safely mutate scene2d/VisUI.
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
 */
fun effect(run: () -> Unit): Disposable = EffectNode(run).also { it.runInitial() }

/** Registers a callback to run before the current [effect] re-runs and when it is disposed. No-op outside an effect. */
fun onCleanup(block: () -> Unit) {
	(currentObserver as? EffectNode)?.addCleanup(block)
}

/** Defers dependent-effect execution until [block] returns, so a burst of writes flushes effects once. */
fun <T> batch(block: () -> T): T {
	batchDepth++
	try {
		return block()
	} finally {
		batchDepth--
		if (batchDepth == 0) flushEffects()
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
		if (primed) block() else primed = true
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
	@JvmField val sourceVersions = ArrayList<Int>()
	@JvmField var state = NodeState.CLEAN

	/** Subscribe the running observer (if any) to this source. */
	fun trackRead() {
		val obs = currentObserver ?: return
		if (obs === this || obs.sources.contains(this)) return
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
		if (state.ordinal >= newState.ordinal) return
		val wasClean = state == NodeState.CLEAN
		state = newState
		onStale(wasClean)
	}

	/** ComputedNode propagates CHECK downstream; EffectNode schedules itself. */
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
		var next: T? = null
		runTracked { next = compute() }
		@Suppress("UNCHECKED_CAST")
		if (cached === UNSET || !equals(cached as T, next as T)) {
			cached = next
			version++ // observers compare against this to know we really changed
		}
		state = NodeState.CLEAN
	}
}

private class EffectNode(private val run: () -> Unit) : ReactiveNode(), Disposable {
	private var disposed = false
	private val cleanups = ArrayList<() -> Unit>()

	fun runInitial() {
		state = NodeState.DIRTY
		execute()
	}

	fun addCleanup(block: () -> Unit) {
		cleanups.add(block)
	}

	override fun onStale(wasClean: Boolean) {
		if (wasClean && !disposed) pendingEffects.addLast(this)
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
		runCleanups()
		runTracked(run)
		state = NodeState.CLEAN
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
		pendingEffects.remove(this)
	}
}

private fun flushEffects() {
	if (flushing) return // a write inside an effect just queues more work; the loop below drains it
	flushing = true
	try {
		while (pendingEffects.isNotEmpty()) {
			pendingEffects.removeFirst().updateIfNecessary()
		}
	} finally {
		flushing = false
	}
}
