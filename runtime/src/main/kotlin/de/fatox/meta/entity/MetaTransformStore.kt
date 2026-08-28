package de.fatox.meta.entity

/**
 * Columns of transform data for every live entity, stored one component per array.
 *
 * ### Why this shape
 *
 * Measured against the ordinary object-per-entity layout at 100k entities: integrating positions is **4-10x**
 * faster reading columns, and a distance cull is **1.6x** faster. The win comes from walking memory linearly -
 * three contiguous float arrays instead of chasing an entity reference to a `Vector3` to its fields - and it is
 * also what makes SIMD possible at all, because `FloatVector.fromArray` needs contiguous lanes.
 *
 * ### The rule that makes it safe
 *
 * **Bulk systems read the columns. Individual game code reads [MetaEntity].** Both are supported and the split is
 * not cosmetic: iterating entities *through* the facade measured 4x slower than the plain object layout - worse
 * than what it replaces - because it pays object chasing *and* column indirection. See [MetaEntity] for the
 * detection that stops that happening by accident.
 *
 * ### What lives here and what does not
 *
 * Only the universal hot fields: position, velocity, rotation, scale. A model, an AI state machine, an inventory
 * or a name stays an ordinary field on your own entity subclass. This is deliberately not an ECS - there is no
 * component registry and no archetype table, because the thing that pays is the layout of the fields every entity
 * has and every system touches, not the dissolution of the entity into parts.
 *
 * ### Density
 *
 * Slots are kept dense by swapping the last entity into a freed hole, so [count] is always the live range and a
 * system can iterate `0 until count` with no holes to skip. The moved entity's slot is patched by the store, so
 * an entity's *object identity* is stable even though its slot is not. Never store a slot index yourself; hold
 * the [MetaEntity].
 */
class MetaTransformStore(initialCapacity: Int = DEFAULT_CAPACITY) {
	init {
		require(initialCapacity > 0) { "Store capacity must be positive, was $initialCapacity" }
	}

	private var capacity = initialCapacity

	/**
	 * Position, velocity, rotation and scale, one component per column.
	 *
	 * Readable and element-mutable by anyone; replaceable only by [grow]. A public setter let a caller assign
	 * `store.x = FloatArray(0)`, or swap one column without `capacity`, `count` or the others - after which the
	 * next access reads unrelated data or lands out of bounds, possibly once [MetaEntityWorld.remove] has already
	 * shortened its entity list.
	 *
	 * That costs nothing at the only place it matters. A system hoists these into locals *once* before its loop,
	 * so the getter runs once per system per frame rather than per element:
	 *
	 * ```kotlin
	 * val px = store.x; val vx = store.vx
	 * for (i in 0 until store.count) px[i] += vx[i] * dt
	 * ```
	 *
	 * They are reallocated when the store grows, so hoist them *inside* the frame that uses them and never cache
	 * one across frames. [forEachSlot] exists so a system does not have to remember that.
	 */
	var x: FloatArray = FloatArray(initialCapacity)
		private set
	var y: FloatArray = FloatArray(initialCapacity)
		private set
	var z: FloatArray = FloatArray(initialCapacity)
		private set
	var vx: FloatArray = FloatArray(initialCapacity)
		private set
	var vy: FloatArray = FloatArray(initialCapacity)
		private set
	var vz: FloatArray = FloatArray(initialCapacity)
		private set
	var rotation: FloatArray = FloatArray(initialCapacity)
		private set
	var scale: FloatArray = FloatArray(initialCapacity)
		private set

	/** Which entity owns each slot, so a swap can patch the moved one. Also what makes a slot printable. */
	private var owners: Array<MetaEntity?> = arrayOfNulls(initialCapacity)

	/**
	 * Live entity count. Slots `0 until count` are occupied; there are no holes.
	 *
	 * Not `@JvmField`, because it needs a private setter and the two are mutually exclusive. That costs nothing
	 * here: a system reads this once to bound its loop, not once per element.
	 */
	var count: Int = 0
		private set

	/** Guards against structural change during a [forEachSlot] walk, which would skip or double-visit entities. */
	private var iterating = false

	/**
	 * How many live entities implement [MetaEntityState], so a capture can skip the whole pass when none do.
	 *
	 * Counted as entities come and go rather than scanned for, because the scan's worst case is exactly the case
	 * worth keeping free: a game not using the hook would pay an instanceof per entity per frame to learn nothing.
	 */
	private var customStateCount = 0

	/**
	 * The buffers handed to [MetaEntityState]. One pair per store, refilled per entity.
	 *
	 * Fixed windows rather than offsets into the flat arrays: an entity that writes past its share gets an
	 * out-of-bounds here instead of quietly overwriting the next entity's state.
	 */
	private val customScratchInts = IntArray(MetaEntityState.INTS)
	private val customScratchFloats = FloatArray(MetaEntityState.FLOATS)

	/** How many entities this store can hold before it grows. Grows by doubling; never shrinks. */
	val currentCapacity: Int get() = capacity

	/**
	 * Binds [entity] to a fresh slot and returns it.
	 *
	 * The entity's scale defaults to 1 rather than 0, because a zero-scale entity is invisible and the resulting
	 * "nothing renders" is a genuinely hard thing to trace back to a defaulted array.
	 */
	internal fun allocate(entity: MetaEntity): Int {
		checkMutable("add an entity")
		if (count == capacity) grow()
		val slot = count++
		owners[slot] = entity
		if (entity is MetaEntityState) customStateCount++
		x[slot] = 0f; y[slot] = 0f; z[slot] = 0f
		vx[slot] = 0f; vy[slot] = 0f; vz[slot] = 0f
		rotation[slot] = 0f
		scale[slot] = 1f
		return slot
	}

	/**
	 * Frees [slot] by moving the last live entity into it, keeping the range dense.
	 *
	 * The moved entity is told its new slot here, which is why nothing outside this class may hold a raw index.
	 */
	internal fun release(slot: Int) {
		checkMutable("remove an entity")
		require(slot in 0 until count) { "Slot $slot is not live (count=$count)" }
		// Read before the swap below overwrites it with the entity moving down.
		if (owners[slot] is MetaEntityState) customStateCount--
		val last = count - 1
		if (slot != last) {
			x[slot] = x[last]; y[slot] = y[last]; z[slot] = z[last]
			vx[slot] = vx[last]; vy[slot] = vy[last]; vz[slot] = vz[last]
			rotation[slot] = rotation[last]
			scale[slot] = scale[last]
			val moved = owners[last]
			owners[slot] = moved
			// The one line that makes swap-remove safe: the entity that moved learns where it went.
			moved?.rebind(slot)
		}
		owners[last] = null
		count = last
	}

	/**
	 * Throws if a system is walking this store, naming [action].
	 *
	 * Public so a caller that mutates more than the columns can refuse *before* it changes anything of its own.
	 * [MetaEntityWorld.remove] has to: it swaps its own entity list first, so discovering the problem inside
	 * [release] would leave the list shortened and reordered against a store that never changed.
	 */
	fun checkMutable(action: String) {
		check(!iterating) { "Cannot $action while a system is iterating this store" }
	}

	/** The entity in [slot], for diagnostics. Null outside the live range. */
	fun ownerOf(slot: Int): MetaEntity? = if (slot in 0 until count) owners[slot] else null

	/**
	 * Runs [body] over every live slot with the store locked against structural change.
	 *
	 * Prefer this to a hand-rolled `for (i in 0 until store.count)` when the body might add or remove entities:
	 * adding reallocates the columns and removing swaps a different entity into the index just visited, so either
	 * one silently corrupts a hand-rolled walk. Here it throws instead.
	 *
	 * The columns are still read directly inside [body], so this costs nothing per element.
	 */
	inline fun forEachSlot(body: (slot: Int) -> Unit) {
		beginIteration()
		try {
			for (slot in 0 until count) body(slot)
		} finally {
			endIteration()
		}
	}

	@PublishedApi
	internal fun beginIteration() {
		check(!iterating) { "This store is already being iterated; nested systems would see inconsistent slots" }
		iterating = true
	}

	@PublishedApi
	internal fun endIteration() {
		iterating = false
	}

	/**
	 * Releases every entity and empties the columns.
	 *
	 * Internal: this half of the world cannot clear the other. Called directly it would empty the columns and unbind
	 * every entity while [MetaEntityWorld]'s list still held them - leaving a world whose `size` is non-zero, whose
	 * `validate()` fails, and whose ghost entities cannot be removed because removal refuses an unbound entity.
	 * Clear through [MetaEntityWorld.clear].
	 */
	internal fun clear() {
		checkMutable("clear the store")
		for (slot in 0 until count) {
			owners[slot]?.unbind()
			owners[slot] = null
		}
		customStateCount = 0
		count = 0
	}

	/**
	 * Copies the live columns and slot owners into [snapshot]. See [MetaEntityWorld.captureInto].
	 *
	 * Column at a time with `System.arraycopy`, which is an intrinsic and moves the whole run at memory speed.
	 */
	internal fun captureInto(snapshot: MetaWorldSnapshot) {
		snapshot.ensureCapacity(count)
		val previousCount = snapshot.count
		System.arraycopy(x, 0, snapshot.x, 0, count)
		System.arraycopy(y, 0, snapshot.y, 0, count)
		System.arraycopy(z, 0, snapshot.z, 0, count)
		System.arraycopy(vx, 0, snapshot.vx, 0, count)
		System.arraycopy(vy, 0, snapshot.vy, 0, count)
		System.arraycopy(vz, 0, snapshot.vz, 0, count)
		System.arraycopy(rotation, 0, snapshot.rotation, 0, count)
		System.arraycopy(scale, 0, snapshot.scale, 0, count)
		System.arraycopy(owners, 0, snapshot.owners, 0, count)
		// Anything the previous capture left past the new live range is cleared, so reusing a snapshot for a
		// smaller scene cannot go on pinning entities it no longer describes.
		if (previousCount > count) java.util.Arrays.fill(snapshot.owners, count, previousCount, null)
		snapshot.count = count
		captureCustomInto(snapshot)
	}

	/**
	 * Collects [MetaEntityState] from whichever entities implement it.
	 *
	 * Every slot gets a window, implementer or not, so a mixed world stays a flat indexable array rather than
	 * something a restore has to search. The scratch is zeroed per entity, so an implementation that writes three
	 * ints does not inherit the previous entity's remaining thirteen.
	 */
	private fun captureCustomInto(snapshot: MetaWorldSnapshot) {
		snapshot.hasCustomState = customStateCount > 0
		if (customStateCount == 0) return
		snapshot.ensureCustomCapacity()
		val ints = customScratchInts
		val floats = customScratchFloats
		for (slot in 0 until count) {
			java.util.Arrays.fill(ints, 0)
			java.util.Arrays.fill(floats, 0f)
			val entity = owners[slot] as? MetaEntityState
			entity?.captureState(ints, floats)
			System.arraycopy(ints, 0, snapshot.customInts, slot * MetaEntityState.INTS, MetaEntityState.INTS)
			System.arraycopy(floats, 0, snapshot.customFloats, slot * MetaEntityState.FLOATS, MetaEntityState.FLOATS)
			// Taken now, not read back at digest time: an implementation is free to derive a mask from mutable
			// state, and a retained snapshot must keep hashing the world it captured.
			snapshot.customExcludedInts[slot] = entity?.digestExcludedInts ?: 0
			snapshot.customExcludedFloats[slot] = entity?.digestExcludedFloats ?: 0
		}
	}

	/**
	 * Replaces the live columns, the count and every slot binding with [snapshot]'s.
	 *
	 * Every entity currently bound is unbound *first*, and only then is the snapshot's set bound. Done in one pass
	 * each, that handles the three cases uniformly and without a set to test membership against: an entity in both
	 * is rebound to its snapshot slot, an entity added since the capture is unbound and correctly leaves the
	 * world, and an entity removed since is bound again from the reference the snapshot kept alive for exactly
	 * this. Anything cheaper needs to ask "was this in the snapshot", which means hashing entities every frame.
	 */
	internal fun restoreFrom(snapshot: MetaWorldSnapshot) {
		checkMutable("restore a snapshot")
		// Every entity is checked before any of them is touched, so a snapshot belonging to another world is
		// refused rather than half applied. Binding one of those would re-point it at this store while the world
		// that still lists it carries on believing it owns it - two worlds, one entity, and the first symptom is
		// somebody reading a transform that belongs to a different scene.
		for (slot in 0 until snapshot.count) {
			val entity = checkNotNull(snapshot.owners[slot]) {
				"Snapshot slot $slot holds no entity, so the world cannot be restored from it. A snapshot must be " +
					"filled by MetaEntityWorld.captureInto and its entity references left alone."
			}
			val owner = entity.store
			check(owner == null || owner === this) {
				"${entity::class.simpleName} in snapshot slot $slot still belongs to another world. Restore a " +
					"snapshot into the world it was captured from, or clear the other world first."
			}
		}
		ensureCapacityFor(snapshot.count)
		for (slot in 0 until count) {
			owners[slot]?.unbind()
			owners[slot] = null
		}
		System.arraycopy(snapshot.x, 0, x, 0, snapshot.count)
		System.arraycopy(snapshot.y, 0, y, 0, snapshot.count)
		System.arraycopy(snapshot.z, 0, z, 0, snapshot.count)
		System.arraycopy(snapshot.vx, 0, vx, 0, snapshot.count)
		System.arraycopy(snapshot.vy, 0, vy, 0, snapshot.count)
		System.arraycopy(snapshot.vz, 0, vz, 0, snapshot.count)
		System.arraycopy(snapshot.rotation, 0, rotation, 0, snapshot.count)
		System.arraycopy(snapshot.scale, 0, scale, 0, snapshot.count)
		// Recounted rather than carried over: the snapshot's population is not this world's, so the tally that
		// decides whether the custom passes run has to come from what is actually being bound.
		var rebuiltCustomStateCount = 0
		for (slot in 0 until snapshot.count) {
			val entity = checkNotNull(snapshot.owners[slot]) {
				"Snapshot slot $slot holds no entity, so the world cannot be restored from it. A snapshot must be " +
					"filled by MetaEntityWorld.captureInto and its entity references left alone."
			}
			owners[slot] = entity
			entity.bind(this, slot)
			if (entity is MetaEntityState) rebuiltCustomStateCount++
		}
		count = snapshot.count
		customStateCount = rebuiltCustomStateCount
		restoreCustomFrom(snapshot)
	}

	/** Hands each entity its own state back. Reconciliation is a separate pass; see [notifyRestored]. */
	private fun restoreCustomFrom(snapshot: MetaWorldSnapshot) {
		if (customStateCount == 0 || !snapshot.hasCustomState) return
		val ints = customScratchInts
		val floats = customScratchFloats
		// Locked for the same reason the notification pass is, and with more at stake: the columns already hold
		// the snapshot's population while MetaEntityWorld's entity list is still the pre-restore one, so a removal
		// here would update the two against different slot layouts - and the list rebuild that follows re-adds
		// every snapshot owner regardless, leaving size, count, owners and bindings disagreeing four ways.
		beginIteration()
		try {
			for (slot in 0 until count) {
				val entity = owners[slot]
				if (entity !is MetaEntityState) continue
				System.arraycopy(snapshot.customInts, slot * MetaEntityState.INTS, ints, 0, MetaEntityState.INTS)
				System.arraycopy(
					snapshot.customFloats,
					slot * MetaEntityState.FLOATS,
					floats,
					0,
					MetaEntityState.FLOATS,
				)
				entity.restoreState(ints, floats)
			}
		} finally {
			endIteration()
		}
	}

	/**
	 * Lets every entity reconcile, once the whole world is back.
	 *
	 * A separate pass called by [MetaEntityWorld.restoreFrom], and called by it *last*, because the world these
	 * callbacks are promised is not complete until its entity list has been rebuilt too. Firing them at the end of
	 * the restore above would show a callback the restored columns through a still-stale `size` and `entityAt` -
	 * so an entity killed by the rollback would still be listed, and one resurrected by it would not be.
	 */
	internal fun notifyRestored() {
		if (customStateCount == 0) return
		// Locked for the same reason forEachSlot locks: this walks slots, and a swap-remove moves the last entity
		// into a slot the cursor has already passed, so that entity would silently never be reconciled and would
		// keep the stale derived state this pass exists to refresh. Refusing loudly beats half-supporting it.
		beginIteration()
		try {
			for (slot in 0 until count) (owners[slot] as? MetaEntityState)?.onRestored()
		} finally {
			endIteration()
		}
	}

	/**
	 * Mixes live [MetaEntityState] into [hash], skipping each entity's excluded indices.
	 *
	 * Read from the entities rather than from a snapshot, so this hashes the world as it is now - which is what a
	 * peer comparison needs. [MetaEntityState.captureState] is required to be pure for exactly this reason.
	 */
	internal fun digestCustomState(hash: Long): Long {
		if (customStateCount == 0) return hash
		var running = hash
		val ints = customScratchInts
		val floats = customScratchFloats
		for (slot in 0 until count) {
			val entity = owners[slot]
			if (entity !is MetaEntityState) continue
			java.util.Arrays.fill(ints, 0)
			java.util.Arrays.fill(floats, 0f)
			entity.captureState(ints, floats)
			running = mixCustomWindow(
				running,
				slot,
				ints,
				0,
				floats,
				0,
				entity.digestExcludedInts,
				entity.digestExcludedFloats,
			)
		}
		return running
	}

	private fun ensureCapacityFor(required: Int) {
		if (required <= capacity) return
		var next = maxOf(capacity, DEFAULT_CAPACITY)
		while (next < required) next *= 2
		growTo(next)
	}

	// maxOf rather than a bare double: a store constructed with capacity 0 would otherwise double to 0 forever.
	private fun grow() = growTo(maxOf(capacity * 2, DEFAULT_CAPACITY))

	private fun growTo(next: Int) {
		x = x.copyOf(next); y = y.copyOf(next); z = z.copyOf(next)
		vx = vx.copyOf(next); vy = vy.copyOf(next); vz = vz.copyOf(next)
		rotation = rotation.copyOf(next)
		scale = scale.copyOf(next)
		owners = owners.copyOf(next)
		capacity = next
	}

	companion object {
		const val DEFAULT_CAPACITY: Int = 256
	}
}
