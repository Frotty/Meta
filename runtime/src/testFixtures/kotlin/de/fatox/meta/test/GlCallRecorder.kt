package de.fatox.meta.test

import com.badlogic.gdx.utils.ObjectIntMap

/**
 * Counts the GL calls a frame makes, so a test can assert on them.
 *
 * The counters this produces are **exact and machine-independent**: a frame that binds twelve textures binds twelve
 * on every machine, every run, at every clock speed. That is the whole reason this exists rather than a wall-clock
 * benchmark. Draw-call and bind counts are the numbers that actually decide whether Meta's UI batches, and unlike a
 * timing they can gate a pull request on a noisy shared CI runner without ever flaking.
 *
 * It records through [HeadlessGL20]'s proxy, so it sees precisely the calls libGDX made — no instrumentation of
 * Meta's own code, and nothing to keep in sync when a widget changes how it draws.
 *
 * ```kotlin
 * val counts = GlCallRecorder.record {
 *     stage.draw()
 * }
 * assertTrue(counts.textureBinds <= 4) { "UI stopped batching: ${counts.textureBinds} binds" }
 * ```
 *
 * **Only counts what the stub is asked to do.** Pixels go nowhere (see [HeadlessGL20]), but the call *sequence* is
 * real — `SpriteBatch` flushes on a texture change here exactly as it does against a driver, which is the property
 * these assertions rely on.
 */
object GlCallRecorder {
	private val counts = ObjectIntMap<String>()
	private var recording = false

	/** Called from [HeadlessGL20]'s invocation handler. A single boolean test when not recording. */
	internal fun observe(methodName: String) {
		if (!recording) return
		counts.getAndIncrement(methodName, 0, 1)
	}

	/**
	 * Runs [block] with recording on and returns what it called.
	 *
	 * Not reentrant, and deliberately not: nested recordings would silently attribute one block's calls to another.
	 */
	fun record(block: () -> Unit): GlCallCounts {
		check(!recording) { "GlCallRecorder.record is already running; nested recording would mix the two tallies" }
		counts.clear()
		recording = true
		try {
			block()
		} finally {
			recording = false
		}
		return GlCallCounts(snapshot())
	}

	private fun snapshot(): ObjectIntMap<String> {
		val copy = ObjectIntMap<String>(counts.size.coerceAtLeast(1))
		val keys = counts.keys()
		while (keys.hasNext) {
			val key = keys.next()
			copy.put(key, counts.get(key, 0))
		}
		return copy
	}
}

/**
 * What one recorded block called, by GL entry point.
 *
 * The named properties cover what decides batching; [callsTo] reaches anything else without widening this class
 * every time a test wants a different counter.
 */
class GlCallCounts internal constructor(private val counts: ObjectIntMap<String>) {
	/**
	 * Texture binds — the number that says whether the UI batches.
	 *
	 * `SpriteBatch` flushes whenever the bound texture changes, so this is within one of the draw-call count for a
	 * pure-2D frame, and it is the number a texture atlas is supposed to collapse.
	 */
	val textureBinds: Int get() = callsTo("glBindTexture")

	/** Geometry submissions. Together with [textureBinds] this is the frame's real draw-call cost. */
	val drawCalls: Int get() = callsTo("glDrawArrays") + callsTo("glDrawElements")

	/** Shader switches, which flush the batch just as a texture change does. */
	val shaderSwitches: Int get() = callsTo("glUseProgram")

	/** Texture uploads, so a test can catch a path that re-uploads chrome it should have cached. */
	val textureUploads: Int get() = callsTo("glTexImage2D") + callsTo("glTexSubImage2D")

	/** Calls to any GL entry point by exact method name, e.g. `callsTo("glBindFramebuffer")`. */
	fun callsTo(methodName: String): Int = counts.get(methodName, 0)

	/** Every entry point touched, highest count first — the useful thing to print when an assertion fails. */
	override fun toString(): String {
		val entries = ArrayList<Pair<String, Int>>(counts.size)
		val keys = counts.keys()
		while (keys.hasNext) {
			val key = keys.next()
			entries.add(key to counts.get(key, 0))
		}
		entries.sortWith(compareByDescending { it.second })
		return entries.joinToString(prefix = "GlCallCounts(", postfix = ")") { "${it.first}=${it.second}" }
	}
}
