package de.fatox.meta.concurrent

/**
 * Which thread owns Meta's mutable state, and a guard that says so out loud.
 *
 * Almost everything in Meta is single-threaded by design and always has been: scene2d, the reactive graph, libGDX's
 * collections, the injection graph. That is not a limitation to be fixed - it is what makes the rest of
 * [de.fatox.meta.concurrent] safe, because a worker that cannot touch shared state cannot race with anything.
 *
 * The problem was never the rule, it was that breaking it was **silent**. A signal written from a worker corrupts the
 * reactive graph without throwing: an effect stops firing, or fires for the wrong reason, three frames later and a
 * hundred lines away. That is the single worst class of bug a game framework can have, because the symptom never
 * points at the cause.
 *
 * This converts it into the best class instead - an immediate exception, on the offending call, naming both threads.
 *
 * ```kotlin
 * // In a signal setter, or anywhere that mutates state the render thread reads:
 * MetaThreads.assertMain("write a signal")
 * ```
 *
 * ### Cost
 *
 * One reference comparison against a `@Volatile` field. It is cheap enough to leave on in a shipped game, and it is
 * on by default for exactly that reason: a guard that only runs in development does not catch the race that only
 * happens on a player's twelve-core machine. Turn it off with `-Dmeta.threads.strict=false` if a profile ever
 * justifies it.
 */
object MetaThreads {
	/**
	 * The thread that owns Meta's state, captured by [claimMainThread].
	 *
	 * Volatile because a worker reads it to decide whether it is allowed to act, and that read must see the value
	 * the main thread published at startup rather than a stale null.
	 */
	@Volatile
	private var mainThread: Thread? = null

	/** Off only by explicit opt-out; see the class docs for why the default is on. */
	private val strict: Boolean = System.getProperty("meta.threads.strict") != "false"

	/** The owning thread, or null before [claimMainThread] has run. Mostly useful in diagnostics. */
	val owner: Thread? get() = mainThread

	/** True once an owner has been claimed. A harness that never claims one disables the guard by omission. */
	val isClaimed: Boolean get() = mainThread != null

	/**
	 * Marks the calling thread as the owner of Meta's state. Called from `Meta.create`, on the render thread.
	 *
	 * Idempotent for the same thread. Re-claiming from a *different* thread is allowed and replaces the owner,
	 * because a test suite legitimately runs cases on different threads and libGDX's headless backend does not use
	 * the thread that started it.
	 */
	fun claimMainThread() {
		mainThread = Thread.currentThread()
	}

	/** Forgets the owner, disabling the guard. For test teardown, so one case cannot fail the next. */
	fun releaseMainThread() {
		mainThread = null
	}

	/**
	 * Throws unless the caller is the owning thread.
	 *
	 * Silent when no owner has been claimed: engine code runs in unit tests that never boot an application, and a
	 * guard that fired there would make the framework untestable rather than safe.
	 *
	 * @param action what the caller was about to do, named so the message says why it is a problem.
	 */
	fun assertMain(action: String) {
		if (!strict) return
		val owner = mainThread ?: return
		val current = Thread.currentThread()
		if (current === owner) return
		throw WrongThreadException(
			"Tried to $action from thread '${current.name}', but Meta's state is owned by '${owner.name}'. " +
				"Scene2d, the reactive graph and the injection graph are single-threaded by design. Do the work " +
				"on a MetaJobs lane and apply the result on the main thread - see de.fatox.meta.concurrent.",
		)
	}

	/** True when the caller owns Meta's state, or when nothing has claimed it. For a branch rather than a throw. */
	fun isMain(): Boolean {
		val owner = mainThread ?: return true
		return Thread.currentThread() === owner
	}
}

/**
 * Thrown by [MetaThreads.assertMain] when shared state is touched from the wrong thread.
 *
 * Deliberately not an `IllegalStateException`: this is the one failure a game must never catch and continue from,
 * and a distinct type makes that greppable and makes a broad `catch (IllegalStateException)` unable to swallow it.
 */
class WrongThreadException(message: String) : RuntimeException(message)
