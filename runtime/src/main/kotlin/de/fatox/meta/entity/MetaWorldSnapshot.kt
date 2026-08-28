package de.fatox.meta.entity

/**
 * Which transform columns a digest covers.
 *
 * Rollback restores everything; a digest should not. A digest is what two peers compare to decide they have
 * desynced, so a column that is presentation rather than simulation - a scale a game tweens for a hit flash, a
 * rotation it derives from the camera - makes the check fire on machines that agree about the game perfectly well.
 * Excluding such a column is not a weakening; including it is a false positive waiting to happen.
 *
 * Combine with `or`. [SIMULATION] is the usual answer.
 */
object MetaTransformColumns {
	const val POSITION: Int = 1
	const val VELOCITY: Int = 2
	const val ROTATION: Int = 4
	const val SCALE: Int = 8

	/** Position and velocity: what almost always decides the outcome, and nothing that only decides the look. */
	const val SIMULATION: Int = POSITION or VELOCITY

	/** Every column. Correct when a game simulates rotation and scale rather than deriving them for display. */
	const val ALL: Int = POSITION or VELOCITY or ROTATION or SCALE
}

/**
 * A complete, reusable copy of a [MetaEntityWorld]'s transforms and slot bindings.
 *
 * Sized once and refilled, because rollback netcode captures every frame and keeps a window of them: a snapshot
 * that allocated would put the whole scene's transforms into the nursery sixty times a second, and the resulting
 * collection pause is indistinguishable from a network stall to everyone playing.
 *
 * ### What it holds, and what it deliberately does not
 *
 * The columns, the live count, and **which entity occupies which slot**. That last part is not bookkeeping:
 * [MetaTransformStore] frees a slot by swapping the last entity into it, so slot assignment is a function of the
 * whole add/remove history and cannot be re-derived from positions. Restore it and every entity keeps its own
 * transform; skip it and entities silently trade places, each reading a neighbour's position.
 *
 * An entity's own fields - `health`, a state machine, an ammo count - come too, but only through
 * [MetaEntityState]: sixteen ints and eight floats per entity, written by hand. The ceiling is the point. A
 * library capturing those fields automatically would have to serialize or reflect over the object graph every
 * frame, which is exactly what a rollback engine cannot afford, so an entity needing more than fits should hold
 * an index into a table the game snapshots itself.
 *
 * ### It keeps entities alive on purpose
 *
 * Slots hold strong references, so an entity removed after this snapshot was taken is retained by it. That is
 * required rather than incidental - rolling back to this frame has to be able to put that entity back - but it
 * means a window of N snapshots pins every entity removed within those N frames. Bounded and intended, though
 * worth knowing before wondering why a scene's footprint does not drop the instant something dies.
 * [releaseRetainedEntities] drops them when a window is retired.
 */
class MetaWorldSnapshot(initialCapacity: Int = MetaTransformStore.DEFAULT_CAPACITY) {
	/** How many entities the snapshot holds. */
	var count: Int = 0
		internal set

	/** The frame this was taken at. Meta never reads it; it is here so a caller's ring buffer need not parallel it. */
	var frame: Int = -1

	internal var x = FloatArray(initialCapacity)
	internal var y = FloatArray(initialCapacity)
	internal var z = FloatArray(initialCapacity)
	internal var vx = FloatArray(initialCapacity)
	internal var vy = FloatArray(initialCapacity)
	internal var vz = FloatArray(initialCapacity)
	internal var rotation = FloatArray(initialCapacity)
	internal var scale = FloatArray(initialCapacity)
	internal var owners = arrayOfNulls<MetaEntity>(initialCapacity)

	/**
	 * Per-entity state from [MetaEntityState], laid out as `slot * MetaEntityState.INTS + index`.
	 *
	 * Empty until a world containing at least one such entity is captured, so a game that does not use the hook
	 * carries none of the footprint - a window of snapshots would otherwise reserve it per frame for nothing.
	 */
	internal var customInts = IntArray(0)
	internal var customFloats = FloatArray(0)

	/**
	 * Each entity's exclusion masks as they stood at capture, one entry per slot.
	 *
	 * Captured rather than read back off the entity, so [digest] is a function of this snapshot alone. Nothing
	 * stops an implementation deriving a mask from mutable state, and if one does, reading it live would change
	 * the hash of an already-retained snapshot - which turns a stored history into something that answers
	 * differently depending on when it is asked, and makes a late desync check report on a world that never was.
	 */
	internal var customExcludedInts = IntArray(0)
	internal var customExcludedFloats = IntArray(0)

	/** Whether [customInts] and [customFloats] describe the captured world. */
	internal var hasCustomState = false

	/** How many entities this can hold before it has to grow. */
	val capacity: Int get() = x.size

	/** Brings the custom-state buffers into existence, or up to the column capacity they trail. */
	internal fun ensureCustomCapacity() {
		if (customInts.size >= x.size * MetaEntityState.INTS) return
		customInts = customInts.copyOf(x.size * MetaEntityState.INTS)
		customFloats = customFloats.copyOf(x.size * MetaEntityState.FLOATS)
		customExcludedInts = customExcludedInts.copyOf(x.size)
		customExcludedFloats = customExcludedFloats.copyOf(x.size)
	}

	internal fun ensureCapacity(required: Int) {
		if (required <= x.size) return
		val grown = maxOf(required, x.size * 2, MetaTransformStore.DEFAULT_CAPACITY)
		x = x.copyOf(grown)
		y = y.copyOf(grown)
		z = z.copyOf(grown)
		vx = vx.copyOf(grown)
		vy = vy.copyOf(grown)
		vz = vz.copyOf(grown)
		rotation = rotation.copyOf(grown)
		scale = scale.copyOf(grown)
		owners = owners.copyOf(grown)
		// Only if they already exist: a game that does not use the hook never pays for them.
		if (customInts.isNotEmpty()) ensureCustomCapacity()
	}

	/**
	 * Replaces this snapshot's contents with [other]'s, for a caller promoting one frame of a window to another
	 * without re-reading the world.
	 */
	fun copyFrom(other: MetaWorldSnapshot) {
		ensureCapacity(other.count)
		val previousCount = count
		System.arraycopy(other.x, 0, x, 0, other.count)
		System.arraycopy(other.y, 0, y, 0, other.count)
		System.arraycopy(other.z, 0, z, 0, other.count)
		System.arraycopy(other.vx, 0, vx, 0, other.count)
		System.arraycopy(other.vy, 0, vy, 0, other.count)
		System.arraycopy(other.vz, 0, vz, 0, other.count)
		System.arraycopy(other.rotation, 0, rotation, 0, other.count)
		System.arraycopy(other.scale, 0, scale, 0, other.count)
		System.arraycopy(other.owners, 0, owners, 0, other.count)
		// Past the new live range, so copying a smaller state in cannot leave this pinning entities it no longer
		// describes - a leak that would only show up as a scene whose footprint never falls.
		if (previousCount > other.count) java.util.Arrays.fill(owners, other.count, previousCount, null)
		hasCustomState = other.hasCustomState
		if (other.hasCustomState) {
			ensureCustomCapacity()
			System.arraycopy(other.customInts, 0, customInts, 0, other.count * MetaEntityState.INTS)
			System.arraycopy(other.customFloats, 0, customFloats, 0, other.count * MetaEntityState.FLOATS)
			System.arraycopy(other.customExcludedInts, 0, customExcludedInts, 0, other.count)
			System.arraycopy(other.customExcludedFloats, 0, customExcludedFloats, 0, other.count)
		}
		count = other.count
		frame = other.frame
	}

	/** Drops the entity references this is keeping alive, without giving up the buffers. */
	fun releaseRetainedEntities() {
		java.util.Arrays.fill(owners, 0, count, null)
		count = 0
	}

	/**
	 * A 64-bit hash of the simulation state held here, over the chosen [columns].
	 *
	 * Same algorithm as a digest taken straight off the world, so a snapshot and the world it was captured from
	 * hash identically. See [MetaEntityWorld.digest].
	 */
	fun digest(columns: Int = MetaTransformColumns.SIMULATION, seed: Long = FNV_OFFSET_64): Long {
		var hash = digestColumns(columns, seed, count, x, y, z, vx, vy, vz, rotation, scale)
		if (!hasCustomState) return hash
		// Values and masks both from the capture, so this depends on nothing outside the snapshot. Only whether a
		// slot holds a stateful entity is read live, and that is its type, which cannot change under us.
		//
		// Read straight out of the flat arrays at an offset rather than copied into a window first. A peer check
		// hashes retained snapshots every frame, so a per-call pair of scratch arrays would be steady garbage in
		// the one path this class exists to keep allocation-free.
		for (slot in 0 until count) {
			if (owners[slot] !is MetaEntityState) continue
			hash = mixCustomWindow(
				hash,
				slot,
				customInts,
				slot * MetaEntityState.INTS,
				customFloats,
				slot * MetaEntityState.FLOATS,
				customExcludedInts[slot],
				customExcludedFloats[slot],
			)
		}
		return hash
	}

	companion object {
		/** FNV-1a 64-bit offset basis. */
		const val FNV_OFFSET_64: Long = -3750763034362895579L

		/** FNV-1a 64-bit prime. */
		const val FNV_PRIME_64: Long = 1099511628211L
	}
}

/**
 * FNV-1a over the raw IEEE-754 bits of the selected columns, in slot order.
 *
 * **Raw bits, not values**, so the check is bit-exact: a `Math.sin` where the other peer reached for `StrictMath`,
 * or any other one-ulp divergence, changes the digest rather than hiding under an epsilon. That is the entire job.
 *
 * **Slot order, and nothing else.** Two peers running the same simulation from the same inputs perform the same
 * allocations and removals, so their slot assignments agree - which is what makes slot order comparable at all. It
 * also means a bug that reorders slots on one peer shows up here, which is wanted.
 *
 * The count is mixed first, so two states agreeing entity-for-entity over a common prefix still differ.
 */
private fun digestColumns(
	columns: Int,
	seed: Long,
	count: Int,
	x: FloatArray,
	y: FloatArray,
	z: FloatArray,
	vx: FloatArray,
	vy: FloatArray,
	vz: FloatArray,
	rotation: FloatArray,
	scale: FloatArray,
): Long {
	var hash = seed
	hash = hash xor count.toLong()
	hash *= MetaWorldSnapshot.FNV_PRIME_64

	// Column at a time: each is a contiguous scan, and the test against `columns` is hoisted out of the per-entity
	// loop instead of being retested for every slot.
	if (columns and MetaTransformColumns.POSITION != 0) {
		hash = mixFloats(hash, x, count)
		hash = mixFloats(hash, y, count)
		hash = mixFloats(hash, z, count)
	}
	if (columns and MetaTransformColumns.VELOCITY != 0) {
		hash = mixFloats(hash, vx, count)
		hash = mixFloats(hash, vy, count)
		hash = mixFloats(hash, vz, count)
	}
	if (columns and MetaTransformColumns.ROTATION != 0) hash = mixFloats(hash, rotation, count)
	if (columns and MetaTransformColumns.SCALE != 0) hash = mixFloats(hash, scale, count)
	return hash
}

private fun mixFloats(start: Long, column: FloatArray, count: Int): Long {
	var hash = start
	for (slot in 0 until count) {
		// floatToRawIntBits, not floatToIntBits: the latter collapses every NaN to one canonical pattern, which
		// would hide a peer that produced a different NaN - and a NaN turning up on one side only is exactly the
		// divergence worth catching.
		hash = hash xor java.lang.Float.floatToRawIntBits(column[slot]).toLong()
		hash *= MetaWorldSnapshot.FNV_PRIME_64
	}
	return hash
}

/**
 * Mixes one entity's custom window, skipping the indices its exclusion masks name.
 *
 * The excluded slots are skipped entirely rather than mixed as zero, so a game can add a snapshot-only field
 * without changing the digest of every entity that already had one - which would otherwise invalidate a committed
 * golden vector for a change that altered no simulation state at all.
 */
internal fun mixCustomWindow(
	start: Long,
	slot: Int,
	ints: IntArray,
	intOffset: Int,
	floats: FloatArray,
	floatOffset: Int,
	excludedInts: Int,
	excludedFloats: Int,
): Long {
	var hash = start
	// The slot first, because plain entities are skipped and the loop would otherwise hash the *sequence* of
	// stateful windows rather than where they sit. Two worlds that differ only in which entity carries the state
	// would then agree whenever the transform columns could not tell them apart either - which is precisely when
	// the two entities' transforms match, and precisely the desync worth catching.
	hash = hash xor slot.toLong()
	hash *= MetaWorldSnapshot.FNV_PRIME_64
	// The masks themselves, because the loops below skip excluded indices and so mix a *sequence* of surviving
	// values carrying no record of which fields they came from. Window [7, 9] excluding index 0 then presents the
	// same sequence as [9, 7] excluding index 1, and a different named field is authoritative on each side.
	//
	// Framing the masks rather than tagging every value with its index: a collision needs the two masks to differ,
	// so making any mask difference change the hash closes all of them - and costs two operations per entity
	// instead of one per field.
	hash = hash xor excludedInts.toLong()
	hash *= MetaWorldSnapshot.FNV_PRIME_64
	hash = hash xor excludedFloats.toLong()
	hash *= MetaWorldSnapshot.FNV_PRIME_64
	for (index in 0 until MetaEntityState.INTS) {
		if (excludedInts ushr index and 1 != 0) continue
		hash = hash xor ints[intOffset + index].toLong()
		hash *= MetaWorldSnapshot.FNV_PRIME_64
	}
	for (index in 0 until MetaEntityState.FLOATS) {
		if (excludedFloats ushr index and 1 != 0) continue
		hash = hash xor java.lang.Float.floatToRawIntBits(floats[floatOffset + index]).toLong()
		hash *= MetaWorldSnapshot.FNV_PRIME_64
	}
	return hash
}

internal fun digestWorldColumns(store: MetaTransformStore, columns: Int, seed: Long): Long = digestColumns(
	columns,
	seed,
	store.count,
	store.x,
	store.y,
	store.z,
	store.vx,
	store.vy,
	store.vz,
	store.rotation,
	store.scale,
)
