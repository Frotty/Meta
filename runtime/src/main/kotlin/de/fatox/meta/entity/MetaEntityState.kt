package de.fatox.meta.entity

/**
 * Lets an entity's own fields ride along in a [MetaWorldSnapshot], beside its transform.
 *
 * A rollback engine has to restore `health`, a state machine's current state, an ammo count - everything the
 * transform columns know nothing about. Implement this and [MetaEntityWorld.captureInto] carries it, so a game
 * keeps one history rather than two that can fall out of step with each other.
 *
 * Deliberately primitive and fixed-size. Capture runs every frame, so it may not serialize, reflect, or walk an
 * object graph; writing named fields into two small arrays is the whole design, and the ceiling is what makes the
 * cost predictable. An entity needing more than fits should hold an index into a table the game snapshots itself.
 *
 * ```kotlin
 * class Rock(var health: Int, var fuse: Float) : MetaEntity(), MetaEntityState {
 *     override fun captureState(ints: IntArray, floats: FloatArray) {
 *         ints[0] = health
 *         floats[0] = fuse
 *     }
 *
 *     override fun restoreState(ints: IntArray, floats: FloatArray) {
 *         health = ints[0]
 *         floats[0].let { fuse = it }
 *     }
 * }
 * ```
 *
 * The arrays handed in are scratch buffers of exactly [INTS] and [FLOATS] entries, zeroed before each entity and
 * copied out afterwards. They are reused, so keep no reference to them; write and read indices from zero.
 *
 * [captureState] must be **pure** - it is called by [MetaEntityWorld.digest] as well as by a capture, and an
 * implementation with side effects would make hashing the world change it.
 */
interface MetaEntityState {
	/** Writes this entity's rollback-relevant fields into the two buffers. Must not have side effects. */
	fun captureState(ints: IntArray, floats: FloatArray)

	/** Reads them back. Called once per entity during [MetaEntityWorld.restoreFrom]. */
	fun restoreState(ints: IntArray, floats: FloatArray)

	/**
	 * Re-derives presentation state, after **every** entity in the world has been restored.
	 *
	 * A second pass on purpose: anything that reads its neighbours - a health bar that follows the entity it
	 * belongs to, a chain that spans several - would otherwise run while half the world is still the future.
	 *
	 * The world is **readable but not restructurable** here. Adding or removing an entity during this pass throws,
	 * because the walk is by slot and a removal swaps the last entity into a slot already visited - which would
	 * leave a live entity silently unreconciled, holding exactly the stale state this call exists to refresh.
	 */
	fun onRestored() {}

	/**
	 * Which of the [INTS] this entity contributes to a snapshot but **not** to a digest, as a bitmask: bit `i`
	 * excludes `ints[i]`.
	 *
	 * The same buffers feed the snapshot, which needs everything to restore correctly, and the digest, which may
	 * only cover state two peers are required to agree on bit-for-bit. Per-machine configuration or presentation
	 * captured here would otherwise be a false desync waiting to happen. A bitmask rather than a list of indices
	 * because a digest reads it per entity, and one `and` is cheaper than walking an array.
	 */
	val digestExcludedInts: Int get() = 0

	/** Float counterpart to [digestExcludedInts]; bit `i` excludes `floats[i]`. */
	val digestExcludedFloats: Int get() = 0

	companion object {
		/** How many ints an entity may contribute. */
		const val INTS: Int = 16

		/** How many floats an entity may contribute. */
		const val FLOATS: Int = 8
	}
}
