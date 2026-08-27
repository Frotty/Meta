package de.fatox.meta.entity

import kotlin.math.floor

/** Which two of the three position columns the index divides the world along. */
enum class MetaSpatialPlane {
	/** Side-on or top-down 2D: x and y. */
	XY,

	/** A 3D game's ground plane, with y up: x and z. */
	XZ,
}

/**
 * Which entities are near a place, maintained by updating rather than rebuilding.
 *
 * Entities are filed into grid cells and stay there. [update] asks four float compares per entity - "is it still
 * inside the cell it was in?" - and re-files only the few that crossed a boundary. Nothing is re-sorted, nothing is
 * reallocated, and a scene where most things are still costs almost nothing to maintain.
 *
 * A structure rebuilt every frame spends longer filing entities than a linear scan spends testing them, which makes
 * it slower than the thing it replaces. Only an incremental one is worth having.
 *
 * ### What it is actually for
 *
 * It does not beat a trivial cull, and cannot: the per-frame check is itself a pass over every entity. Measured on
 * 6,000 entities with about 330 visible, a bare in-bounds test over the position columns runs in 5us while
 * maintaining this index costs 10us and querying it 5us.
 *
 * What it changes is *how many entities the expensive test runs on* - 6,000 down to 487 in that scene. Real culling
 * is not a bare compare: it reads bounds, tests frustum planes, and reaches through to the entity. So the index
 * pays as soon as that work exceeds roughly two nanoseconds per entity, which any real cull does comfortably. If
 * your cull genuinely is one compare over two float columns, keep the scan.
 *
 * ### Everything is an IntArray indexed by slot
 *
 * The cell membership lists are intrusive: [next] and [prev] hold slot indices rather than nodes, so a cell is a
 * chain through arrays that already exist. Linking and unlinking are four array writes with no allocation, and the
 * layout matches [MetaTransformStore]'s columns exactly.
 *
 * ### Cells are hashed
 *
 * Cell coordinates are hashed into a fixed number of buckets rather than indexed into an array covering the world,
 * so coordinates may be any finite float and memory is fixed by [bucketCount] rather than by how far the player
 * has walked. Distant cells can share a bucket; each entity keeps its cell, so a query rejects foreign entries
 * instead of returning them.
 *
 * ### It indexes points
 *
 * An entity is filed by its origin; the index knows nothing about its size. A query must widen by the largest
 * extent among the entities it wants - [forEachInBounds]'s `margin`. Getting it wrong looks like large objects
 * vanishing as their centre leaves the view, so the parameter is named rather than defaulted away.
 *
 * ```kotlin
 * private val index = MetaSpatialIndex(cellSize = 128f)
 *
 * fun render(world: MetaEntityWorld) {
 *     index.update(world.store, MetaSpatialPlane.XY)          // only movers that changed cell
 *     index.forEachInBounds(left, bottom, right, top, margin = largestRadius) { slot -> draw(slot) }
 * }
 * ```
 */
class MetaSpatialIndex(
	cellSize: Float,
	bucketCount: Int = DEFAULT_BUCKETS,
) {
	init {
		require(cellSize > 0f && cellSize.isFinite()) { "Cell size must be positive and finite, was $cellSize" }
		require(bucketCount > 0 && bucketCount and (bucketCount - 1) == 0) {
			"Bucket count must be a positive power of two, was $bucketCount"
		}
	}

	/**
	 * The width of one cell in world units.
	 *
	 * Roughly twice the typical entity size is a sound start. Too small and a query walks many empty cells; too
	 * large and each cell holds entities the query then rejects. [averageOccupancy] reports what the choice is
	 * actually producing.
	 */
	val cellSize: Float = cellSize

	private val inverseCellSize: Float = 1f / cellSize
	private val bucketMask: Int = bucketCount - 1

	/** First slot in each bucket, or [NONE]. */
	private val bucketHead = IntArray(bucketCount) { NONE }

	/** Intrusive chain and cell membership, all indexed by entity slot. */
	private var next = IntArray(0)
	private var prev = IntArray(0)
	private var cellX = IntArray(0)
	private var cellY = IntArray(0)
	private var bucketOfSlot = IntArray(0)
	private var filed = BooleanArray(0)

	/**
	 * The world-space bounds of the cell each entity was filed into.
	 *
	 * Redundant with [cellX]/[cellY] and kept anyway, because it makes the per-frame question cheap. Asking "which
	 * cell is this in now" costs a multiply, a floor and an int conversion per axis; asking "is it still inside the
	 * box it was in" is four float compares that vectorize and never leave the FPU. The first form measured slower
	 * per entity than the trivial cull it exists to avoid, which made the whole index a loss.
	 */
	private var boundMinX = FloatArray(0)
	private var boundMaxX = FloatArray(0)
	private var boundMinY = FloatArray(0)
	private var boundMaxY = FloatArray(0)

	/** How many slots the index currently holds. */
	var size: Int = 0
		private set

	/** How many entities changed cell during the last [update]. The number that decides whether this is paying. */
	var lastMoved: Int = 0
		private set

	/** Occupied buckets, for judging whether [cellSize] and [bucketCount] suit the scene. */
	val occupiedBuckets: Int
		get() {
			var occupied = 0
			for (bucket in bucketHead.indices) if (bucketHead[bucket] != NONE) occupied++
			return occupied
		}

	/** Mean entities per occupied bucket. Well above a handful means the cells are too large. */
	val averageOccupancy: Float
		get() = occupiedBuckets.let { if (it == 0) 0f else size.toFloat() / it }

	/**
	 * Brings the index up to date with the store, re-filing only entities that changed cell.
	 *
	 * Call once per frame before querying. The first call files everything; later calls are proportional to how
	 * much actually moved rather than to how much exists.
	 */
	fun update(store: MetaTransformStore, plane: MetaSpatialPlane) {
		val vertical = if (plane == MetaSpatialPlane.XY) store.y else store.z
		update(store.x, vertical, store.count)
	}

	/**
	 * Brings the index up to date from two coordinate columns.
	 *
	 * The column form exists so a game can index something that is not a [MetaTransformStore] - a particle system,
	 * a chunk table - without owning an entity for each.
	 */
	fun update(horizontal: FloatArray, vertical: FloatArray, count: Int) {
		require(count >= 0) { "Count must not be negative, was $count" }
		require(count <= horizontal.size && count <= vertical.size) {
			"Count $count exceeds the columns supplied (${horizontal.size}, ${vertical.size})"
		}
		ensureCapacity(count)

		// Slots that no longer exist. The store keeps its range dense by swapping the last entity down, so a
		// shrink means the tail is gone - and an entry left behind would have a query report an entity that was
		// removed, which reads as a ghost rather than as a stale index.
		for (slot in count until size) unfile(slot)

		var moved = 0
		for (slot in 0 until count) {
			val x = horizontal[slot]
			val y = vertical[slot]
			// The whole point: for most entities on most frames these four compares are the entire cost, and they
			// vectorize. Only an entity that has actually left its cell pays for the arithmetic below.
			if (filed[slot] &&
				x >= boundMinX[slot] && x < boundMaxX[slot] &&
				y >= boundMinY[slot] && y < boundMaxY[slot]
			) {
				continue
			}
			val cx = cellOf(x)
			val cy = cellOf(y)
			// The float bounds are a fast approximation and can be wrong in one direction: they can claim an
			// entity moved when it did not. A cell interval is two adjacent cell edges rounded to Float, and past
			// 2^24 those round to the same value - at cellSize 1 and x = 20,000,000 the interval is empty, so the
			// test above fails forever and a stationary entity is unlinked and relinked every frame. At
			// Int.MAX_VALUE the upper edge wraps negative and inverts it outright.
			//
			// Confirming against the cell itself costs a floor per apparently-moved entity, which is the rare
			// case, and makes the fast path an optimisation rather than the source of truth.
			if (filed[slot] && cellX[slot] == cx && cellY[slot] == cy) continue
			if (filed[slot]) unlink(slot)
			link(slot, cx, cy)
			moved++
		}
		size = count
		lastMoved = moved
	}

	/**
	 * Visits every entity whose origin lies within the given area, widened by [margin].
	 *
	 * [margin] must cover the largest extent among the entities being queried; see the class docs. Iteration order
	 * is unspecified. Do not add or remove entities from inside [action] - update afterwards instead.
	 *
	 * @return how many entities were visited.
	 */
	inline fun forEachInBounds(
		minHorizontal: Float,
		minVertical: Float,
		maxHorizontal: Float,
		maxVertical: Float,
		margin: Float,
		action: (slot: Int) -> Unit,
	): Int {
		if (size == 0) return 0
		var visited = 0
		val fromX = cellIndexOf(minHorizontal - margin)
		val toX = cellIndexOf(maxHorizontal + margin)
		val fromY = cellIndexOf(minVertical - margin)
		val toY = cellIndexOf(maxVertical + margin)
		for (y in fromY..toY) {
			for (x in fromX..toX) {
				var slot = headOf(x, y)
				while (slot != NO_SLOT) {
					// The bucket may hold entities from an unrelated cell that hashed the same way; reject those
					// rather than handing back a false positive the caller cannot recognise.
					if (cellXOf(slot) == x && cellYOf(slot) == y) {
						visited++
						action(slot)
					}
					slot = nextOf(slot)
				}
			}
		}
		return visited
	}

	/**
	 * How many cells an area spans, so a caller can tell a cheap query from a ruinous one.
	 *
	 * A query covering more cells than there are entities is slower than testing every entity. A camera zoomed far
	 * out does exactly that, and it should be visible rather than mysterious.
	 */
	fun cellsSpanned(
		minHorizontal: Float,
		minVertical: Float,
		maxHorizontal: Float,
		maxVertical: Float,
		margin: Float,
	): Long {
		if (size == 0) return 0
		// Widened to Long before subtracting, not after. Cell indices reach into the billions for a small cell
		// size and a far-flung coordinate, and an Int subtraction there wraps - so this method could report a
		// small or negative span for exactly the multi-billion-cell query it exists to warn about.
		val across = cellIndexOf(maxHorizontal + margin).toLong() - cellIndexOf(minHorizontal - margin).toLong() + 1L
		val down = cellIndexOf(maxVertical + margin).toLong() - cellIndexOf(minVertical - margin).toLong() + 1L
		if (across <= 0L || down <= 0L) return 0
		// The product overflows too once both axes are large; saturate rather than wrap, since every caller is
		// asking "is this too big" and a wrapped answer says no.
		if (across > Long.MAX_VALUE / down) return Long.MAX_VALUE
		return across * down
	}

	/** Empties the index without releasing its buffers. */
	fun clear() {
		for (slot in 0 until size) if (filed[slot]) filed[slot] = false
		java.util.Arrays.fill(bucketHead, NONE)
		size = 0
		lastMoved = 0
	}

	private fun link(slot: Int, x: Int, y: Int) {
		val bucket = bucketOf(x, y)
		val head = bucketHead[bucket]
		next[slot] = head
		prev[slot] = NONE
		if (head != NONE) prev[head] = slot
		bucketHead[bucket] = slot
		cellX[slot] = x
		cellY[slot] = y
		bucketOfSlot[slot] = bucket
		boundMinX[slot] = x * cellSize
		boundMaxX[slot] = (x + 1) * cellSize
		boundMinY[slot] = y * cellSize
		boundMaxY[slot] = (y + 1) * cellSize
		filed[slot] = true
	}

	private fun unlink(slot: Int) {
		val behind = prev[slot]
		val ahead = next[slot]
		if (behind == NONE) bucketHead[bucketOfSlot[slot]] = ahead else next[behind] = ahead
		if (ahead != NONE) prev[ahead] = behind
		next[slot] = NONE
		prev[slot] = NONE
	}

	private fun unfile(slot: Int) {
		if (!filed[slot]) return
		unlink(slot)
		filed[slot] = false
	}

	@PublishedApi internal fun headOf(x: Int, y: Int): Int = bucketHead[bucketOf(x, y)]
	@PublishedApi internal fun nextOf(slot: Int): Int = next[slot]
	@PublishedApi internal fun cellXOf(slot: Int): Int = cellX[slot]
	@PublishedApi internal fun cellYOf(slot: Int): Int = cellY[slot]

	@PublishedApi
	internal fun cellIndexOf(coordinate: Float): Int = cellOf(coordinate)

	/**
	 * Which cell a coordinate falls in.
	 *
	 * `floor`, not truncation, for even cell widths rather than for correctness: truncation rounds towards zero, so
	 * one cell would straddle the origin and hold twice as much - which is where a level usually puts its densest
	 * content. Filing and querying share this function, so nothing is missed either way.
	 */
	private fun cellOf(coordinate: Float): Int {
		if (!coordinate.isFinite()) return 0
		return floor(coordinate * inverseCellSize).toInt()
	}

	/**
	 * Mixes a cell's two coordinates into a bucket.
	 *
	 * Two large odd primes so cells adjacent in either axis land far apart, spreading a rectangular query across
	 * the table instead of hammering one part of it. The shift folds the high bits down, which masking alone would
	 * discard - making every 4096th cell collide by construction.
	 */
	private fun bucketOf(x: Int, y: Int): Int {
		var hash = x * PRIME_X xor y * PRIME_Y
		hash = hash xor (hash ushr 16)
		return hash and bucketMask
	}

	private fun ensureCapacity(required: Int) {
		if (required <= next.size) return
		val grown = maxOf(required, next.size * 2, MIN_CAPACITY)
		next = next.copyOf(grown)
		prev = prev.copyOf(grown)
		cellX = cellX.copyOf(grown)
		cellY = cellY.copyOf(grown)
		bucketOfSlot = bucketOfSlot.copyOf(grown)
		filed = filed.copyOf(grown)
		boundMinX = boundMinX.copyOf(grown)
		boundMaxX = boundMaxX.copyOf(grown)
		boundMinY = boundMinY.copyOf(grown)
		boundMaxY = boundMaxY.copyOf(grown)
	}

	companion object {
		/** Power of two so the bucket mask is a single AND. */
		const val DEFAULT_BUCKETS: Int = 4096

		/** End of a chain. Public because [forEachInBounds] is inline and compares against it. */
		const val NO_SLOT: Int = -1

		private const val NONE = NO_SLOT
		private const val MIN_CAPACITY = 64
		private const val PRIME_X = 0x9E3779B1.toInt()
		private const val PRIME_Y = 0x85EBCA77.toInt()
	}
}
