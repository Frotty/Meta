package de.fatox.meta.concurrent

import com.badlogic.gdx.Gdx
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.error
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private val log = MetaLoggerFactory.logger {}

/**
 * Where background work runs, and how its results get back.
 *
 * Meta had no answer to this at all: two hand-rolled `Thread(...)` calls in the whole runtime, no pool, no scheduler,
 * and no dispatch path but raw `Gdx.app.postRunnable`. Startup was handled beautifully by
 * [de.fatox.meta.api.SplashScreen] and everything after it was left to each game to reinvent.
 *
 * ### The shape: parallelism at the edges, single-threaded at the core
 *
 * Games get burned by threads when they try to parallelise the *simulation*. None of the wins Meta needs require
 * that. A job takes a value, computes, and returns a value; the result is applied on the main thread by whoever
 * asked for it. Workers never touch scene2d, the reactive graph or the injection graph - and [MetaThreads] makes
 * that a thrown exception rather than a convention.
 *
 * ### Two lanes, because the workloads are not alike
 *
 * [io] runs on **virtual threads**. File scans, archive reads, font parsing, save writes and network calls spend
 * their time asleep, and a virtual thread parked on a blocking read costs almost nothing. Thousands are fine.
 *
 * [compute] runs on a **bounded pool of platform threads**, sized [computeParallelism]. Virtual threads buy nothing
 * for CPU-bound work - the carrier is busy either way - so pixmap decodes, mip generation and mesh building want
 * real parallelism with a ceiling. The ceiling leaves headroom for the render thread and the driver's own threads;
 * saturating every core is how a loading screen makes the frame it is animating stutter.
 *
 * ### Results come back through one path
 *
 * A worker never runs a caller's callback. Completions are queued and drained by [drainCompletions] on the main
 * thread, at a point the frame chooses - so a result can safely mutate UI, and there is exactly one place that
 * bounds how much completion work a frame absorbs.
 */
object MetaJobs {
	/**
	 * How many CPU-bound jobs may run at once.
	 *
	 * Two cores are held back deliberately: one for the render thread, one for the driver, audio mixer and GC
	 * threads that are already competing for time. A pool sized to every core makes background loading fight the
	 * frame it is supposed to be hiding behind. Never below one, so a dual-core machine still makes progress.
	 */
	val computeParallelism: Int = (Runtime.getRuntime().availableProcessors() - 2).coerceAtLeast(1)

	private val ioThreadCount = AtomicInteger()
	private val computeThreadCount = AtomicInteger()

	/** Blocking work. Virtual threads: unbounded is correct when the thread is asleep rather than busy. */
	private val ioExecutor: ExecutorService by lazy {
		Executors.newThreadPerTaskExecutor(
			Thread.ofVirtual().name("meta-io-", 0).factory(),
		)
	}

	/** CPU work. Bounded, daemon, low-ish priority so a background decode yields to the frame. */
	private val computeExecutor: ExecutorService by lazy {
		Executors.newFixedThreadPool(computeParallelism, ComputeThreadFactory)
	}

	/**
	 * Finished work waiting to be applied on the main thread.
	 *
	 * A queue rather than `Gdx.app.postRunnable` per result: posting hands control of *when* to libGDX, which
	 * drains its whole queue in one go at a point the frame does not choose. Draining here lets a frame take what
	 * fits and leave the rest, which is the difference between smoothing a spike and moving it.
	 */
	private val completions = ConcurrentLinkedQueue<Runnable>()

	private val submitted = AtomicLong()
	private val completed = AtomicLong()
	private val failed = AtomicLong()

	/** Jobs submitted, finished and failed since startup. For the profiler and for tests. */
	val submittedCount: Long get() = submitted.get()
	val completedCount: Long get() = completed.get()
	val failedCount: Long get() = failed.get()

	/** Results waiting for the main thread. A number that keeps climbing means the drain budget is too small. */
	val pendingCompletions: Int get() = completions.size

	/**
	 * Runs [work] on a virtual thread, then [onDone] on the main thread.
	 *
	 * For work that blocks: reading a file, walking a directory, parsing a face, an HTTP call. [work] must not
	 * touch scene2d, signals or the injection graph; [onDone] may do all three.
	 */
	fun <T> io(scope: MetaJobScope? = null, work: () -> T, onDone: (T) -> Unit): MetaJob =
		submit(ioExecutor, scope, work, onDone)

	/**
	 * Runs [work] on the bounded CPU pool, then [onDone] on the main thread.
	 *
	 * For work that computes: decoding a pixmap, building a mip chain, generating a chunk, culling. Same rule -
	 * [work] sees only what it was given.
	 */
	fun <T> compute(scope: MetaJobScope? = null, work: () -> T, onDone: (T) -> Unit): MetaJob =
		submit(computeExecutor, scope, work, onDone)

	private fun <T> submit(
		executor: ExecutorService,
		scope: MetaJobScope?,
		work: () -> T,
		onDone: (T) -> Unit,
	): MetaJob {
		val job = MetaJob()
		scope?.register(job)
		submitted.incrementAndGet()
		val future = executor.submit {
			if (job.isCancelled) return@submit
			try {
				val result = work()
				// Checked again after the work rather than only before it: a screen can be torn down while its
				// job runs, and applying the result then would touch actors that are already disposed.
				if (job.isCancelled) return@submit
				completions.add {
					if (!job.isCancelled) {
						onDone(result)
						job.markComplete()
					}
				}
				completed.incrementAndGet()
			} catch (interrupted: InterruptedException) {
				Thread.currentThread().interrupt()
				job.markCancelled()
			} catch (failure: Throwable) {
				failed.incrementAndGet()
				job.markFailed(failure)
				// Reported on the main thread, not swallowed on a worker where nothing would ever see it.
				completions.add { reportFailure(failure) }
			}
		}
		job.attach(future)
		return job
	}

	/**
	 * Applies finished work on the main thread, up to [maxCompletions] of them.
	 *
	 * Called once per frame by the renderer, before the stage acts, so a result may rebuild UI and the frame that
	 * follows sees it consistently. The bound is what stops a burst of finished jobs turning into one long frame:
	 * whatever does not fit waits, which is the entire point of having a queue rather than posting each result.
	 *
	 * @return how many completions were applied.
	 */
	fun drainCompletions(maxCompletions: Int = DEFAULT_DRAIN_BUDGET): Int {
		MetaThreads.assertMain("apply a job result")
		var applied = 0
		while (applied < maxCompletions) {
			val completion = completions.poll() ?: break
			applied++
			try {
				completion.run()
			} catch (failure: Throwable) {
				// One bad completion must not strand the ones queued behind it.
				reportFailure(failure)
			}
		}
		return applied
	}

	private fun reportFailure(failure: Throwable) {
		val handler = onJobFailure
		if (handler != null) handler(failure) else log.error(failure) { "Background job failed" }
	}

	/**
	 * Where job failures go, or null to log them.
	 *
	 * Always invoked on the main thread. A game sets this to raise a toast, mark a level load failed, or rethrow
	 * into its own crash path.
	 */
	@JvmStatic
	var onJobFailure: ((Throwable) -> Unit)? = null

	/**
	 * Stops both lanes and drops queued completions. Called from `Meta.dispose`.
	 *
	 * Interrupts running jobs rather than waiting: a game closing does not want to block on a directory walk, and
	 * every lane thread is a daemon so none of them keeps the JVM alive either way.
	 */
	fun shutdown() {
		runCatching { ioExecutor.shutdownNow() }
		runCatching { computeExecutor.shutdownNow() }
		completions.clear()
	}

	private object ComputeThreadFactory : ThreadFactory {
		override fun newThread(runnable: Runnable): Thread =
			Thread(runnable, "meta-compute-${computeThreadCount.getAndIncrement()}").apply {
				isDaemon = true
				// Below normal: background loading should lose to the frame, not compete with it.
				priority = Thread.NORM_PRIORITY - 1
			}
	}

	/** Completions applied per frame by default. Chosen to bound the frame, not to finish the queue. */
	const val DEFAULT_DRAIN_BUDGET: Int = 8
}

/** Convenience for the common case: work whose result the caller does not need. */
fun MetaJobs.ioAndForget(scope: MetaJobScope? = null, work: () -> Unit): MetaJob =
	io(scope, work) { }

/** Convenience for the common case: CPU work whose result the caller does not need. */
fun MetaJobs.computeAndForget(scope: MetaJobScope? = null, work: () -> Unit): MetaJob =
	compute(scope, work) { }

/** Posts [block] to run on the main thread on a later frame, through the same drain path as job results. */
fun onMainThread(block: () -> Unit) {
	if (Gdx.app != null) Gdx.app.postRunnable(block) else block()
}
