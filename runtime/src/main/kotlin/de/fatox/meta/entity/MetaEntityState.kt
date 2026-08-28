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
 *         fuse = floats[0]
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

	/**
	 * Reads them back. Called once per entity during [MetaEntityWorld.restoreFrom].
	 *
	 * Set your own fields and nothing else: adding or removing an entity from here throws. The pass walks slots,
	 * and removing an entity swaps the last one into a slot already visited, which would leave a live entity
	 * silently never handed its state back. If a restored value means an entity should go, record that here and
	 * act on it once [MetaEntityWorld.restoreFrom] has returned - [onRestored] is no good either, for the same
	 * reason.
	 *
	 * Throwing from here is allowed and leaves the world structurally sound; only custom state is half-applied.
	 */
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
	 *
	 * Only the low [INTS] bits are read; anything above is ignored, so `-1` and `0xFFFF` mean the same thing and
	 * hash the same way.
	 */
	val digestExcludedInts: Int get() = 0

	/** Float counterpart to [digestExcludedInts]; bit `i` excludes `floats[i]`. */
	val digestExcludedFloats: Int get() = 0

	companion object {
		/** How many ints an entity may contribute. */
		const val INTS: Int = 16

		/** How many floats an entity may contribute. */
		const val FLOATS: Int = 8

		/**
		 * The bits of an exclusion mask that name a field. Everything above is meaningless and normalised away.
		 *
		 * Without this, `-1` and `0xFFFF` are two spellings of "exclude every int" that hash differently, and two
		 * peers whose authoritative state agrees perfectly report a desync over bits naming no field at all.
		 *
		 * Correct while the counts stay below 32; `1 shl 32` is `1` on the JVM, not `0`.
		 */
		internal const val INT_MASK: Int = (1 shl INTS) - 1
		internal const val FLOAT_MASK: Int = (1 shl FLOATS) - 1
	}
}
