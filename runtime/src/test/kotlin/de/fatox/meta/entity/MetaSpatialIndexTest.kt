package de.fatox.meta.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * Checks the index against the linear scan it replaces, over generated scenes that keep moving.
 *
 * An index that is *updated* rather than rebuilt can drift: an entity unlinked from the wrong chain, a stale cell
 * left behind by a shrink, a bucket head not repaired. None of that throws - it produces an entity that has
 * quietly stopped existing, or one that is reported in two places, and the symptom appears wherever the caller was
 * using the result rather than here. So correctness is checked as a property against brute force, across frames
 * rather than in one shot, because drift needs time to happen.
 */
class MetaSpatialIndexTest {
	@Test
	fun `queries agree with a linear scan across many frames of movement`() {
		val random = Random(SEED)
		var totalMatched = 0L
		var totalVisited = 0L
		var nonEmpty = 0

		repeat(SCENES) { scene ->
			var count = random.nextInt(20, 300)
			// Straddling the origin so negative cells, negative hashes and the cell astride zero are all covered.
			val xs = FloatArray(600) { random.nextInt(-3000, 3000).toFloat() + random.nextFloat() }
			val ys = FloatArray(600) { random.nextInt(-3000, 3000).toFloat() + random.nextFloat() }
			val index = MetaSpatialIndex(cellSize = CELL_SIZES[scene % CELL_SIZES.size])

			repeat(FRAMES_PER_SCENE) { frame ->
				// Move a slice of the population, and occasionally change how many there are - the two things a
				// rebuild handles for free and an incremental index has to get right.
				repeat(count / 4) {
					val slot = random.nextInt(count)
					xs[slot] += random.nextInt(-200, 200).toFloat()
					ys[slot] += random.nextInt(-200, 200).toFloat()
				}
				if (random.nextInt(4) == 0) count = random.nextInt(20, 300)

				index.update(xs, ys, count)
				assertEquals(count, index.size)

				val minX = random.nextInt(-3200, 3200).toFloat()
				val minY = random.nextInt(-3200, 3200).toFloat()
				val maxX = minX + random.nextInt(1, 1500)
				val maxY = minY + random.nextInt(1, 1500)
				val margin = random.nextInt(0, 30).toFloat()

				val expected = HashSet<Int>()
				for (slot in 0 until count) {
					if (xs[slot] >= minX - margin && xs[slot] <= maxX + margin &&
						ys[slot] >= minY - margin && ys[slot] <= maxY + margin
					) {
						expected.add(slot)
					}
				}

				val actual = HashSet<Int>()
				val visited = index.forEachInBounds(minX, minY, maxX, maxY, margin) { slot ->
					// A slot in two chains at once reports twice, which doubles whatever the caller accumulates.
					assertTrue(actual.add(slot)) { "seed=$SEED scene=$scene frame=$frame: slot $slot visited twice" }
				}
				assertEquals(visited, actual.size)

				assertTrue(actual.containsAll(expected)) {
					"seed=$SEED scene=$scene frame=$frame: the index lost " +
						"${(expected - actual).size} entities a linear scan found: ${(expected - actual).take(5)}"
				}
				// Every returned slot must still be live: a shrink that left entries behind reports ghosts.
				assertTrue(actual.all { it < count }) {
					"seed=$SEED scene=$scene frame=$frame: returned slots beyond the live range ($count)"
				}

				totalMatched += expected.size.toLong()
				totalVisited += actual.size.toLong()
				if (expected.isNotEmpty()) nonEmpty++
			}
		}

		assertTrue(nonEmpty > SCENES) { "seed=$SEED: only $nonEmpty queries matched anything; this tested little" }
		// Bounds the over-return as well as the misses. Everything above is a superset check, so an index that
		// returned everything would pass it while being slower than the scan it replaces - which is exactly what
		// losing the bucket-collision rejection does.
		assertTrue(totalVisited < totalMatched * 2) {
			"seed=$SEED: visited $totalVisited to return $totalMatched; the broad-phase is not narrowing anything"
		}
		println("[spatial] seed=$SEED frames=${SCENES * FRAMES_PER_SCENE} matched=$totalMatched visited=$totalVisited")
	}

	@Test
	fun `a still scene costs nothing to maintain`() {
		// The property the whole design exists for. If this were not true the index would be a rebuild wearing a
		// different name, and slower than the linear scan it is meant to replace.
		val random = Random(SEED)
		val count = 5_000
		val xs = FloatArray(count) { random.nextInt(-5000, 5000).toFloat() }
		val ys = FloatArray(count) { random.nextInt(-5000, 5000).toFloat() }
		val index = MetaSpatialIndex(cellSize = 128f)

		index.update(xs, ys, count)
		assertEquals(count, index.lastMoved, "The first update has to file everything")

		index.update(xs, ys, count)
		assertEquals(0, index.lastMoved, "Nothing moved, so nothing should have been re-filed")

		// Move one entity far enough to change cell, and nudge another within its cell.
		xs[10] += 1_000f
		xs[20] += 0.001f
		index.update(xs, ys, count)
		assertTrue(index.lastMoved <= 2) { "One entity crossed a cell; ${index.lastMoved} were re-filed" }
		assertTrue(index.lastMoved >= 1) { "The entity that crossed a cell was not re-filed" }
	}

	@Test
	fun `entities are found after crossing cells repeatedly`() {
		// Walks one entity across many boundaries in both directions. Each crossing unlinks and relinks it, so a
		// chain repaired incorrectly shows up as an entity that can no longer find itself.
		val index = MetaSpatialIndex(cellSize = 10f)
		val xs = floatArrayOf(0f, 55f, -55f)
		val ys = floatArrayOf(0f, 0f, 0f)

		for (step in -60..60) {
			xs[0] = step.toFloat()
			index.update(xs, ys, 3)
			var found = false
			index.forEachInBounds(xs[0], 0f, xs[0], 0f, margin = 0f) { if (it == 0) found = true }
			assertTrue(found) { "The entity could not find itself at x=${xs[0]}" }
			// The stationary neighbours must survive every relink of their neighbour.
			var others = 0
			index.forEachInBounds(-60f, -1f, 60f, 1f, margin = 0f) { others++ }
			assertTrue(others >= 1) { "Stationary entities were lost while another moved" }
		}
	}

	@Test
	fun `shrinking the population removes the entities that went`() {
		val index = MetaSpatialIndex(cellSize = 16f)
		val xs = FloatArray(50) { it * 40f }
		val ys = FloatArray(50)
		index.update(xs, ys, 50)
		assertEquals(50, index.size)

		index.update(xs, ys, 10)
		assertEquals(10, index.size)
		var visited = 0
		index.forEachInBounds(-100f, -10f, 5000f, 10f, margin = 0f) { visited++ }
		assertEquals(10, visited) { "Entities beyond the live range were still reported" }

		// And growing again refiles them rather than resurrecting stale chains.
		index.update(xs, ys, 50)
		visited = 0
		index.forEachInBounds(-100f, -10f, 5000f, 10f, margin = 0f) { visited++ }
		assertEquals(50, visited)
	}

	@Test
	fun `it indexes the world's transform columns on either plane`() {
		val world = MetaEntityWorld(initialCapacity = 4)
		repeat(20) { index -> world.add(object : MetaEntity() {}).setPosition(index * 25f, 0f, index * 25f) }
		val index = MetaSpatialIndex(cellSize = 50f)

		index.update(world.store, MetaSpatialPlane.XZ)
		assertEquals(20, index.size)

		// XY indexes y, which is zero for every entity, so they share a row - a different partition of the same
		// data, and proof the plane argument is honoured rather than ignored.
		index.update(world.store, MetaSpatialPlane.XY)
		var row = 0
		index.forEachInBounds(-10f, -1f, 1000f, 1f, margin = 0f) { row++ }
		assertEquals(20, row)
	}

	@Test
	fun `spanning cost survives coordinates that overflow an int cell index`() {
		// The method exists to warn a caller off a ruinous query, so it is the one place an arithmetic wrap is
		// worst: subtracting Int cell indices near the extremes produces a small or negative span, telling the
		// caller a multi-billion-cell walk is cheap.
		val index = MetaSpatialIndex(cellSize = 1f)
		index.update(floatArrayOf(0f), floatArrayOf(0f), 1)

		val span = index.cellsSpanned(-2_000_000_000f, -2_000_000_000f, 2_000_000_000f, 2_000_000_000f, margin = 0f)
		assertTrue(span > 1_000_000_000L) { "cellsSpanned reported $span for a multi-billion-cell query" }

		// And a tight query still reports something small, so the fix did not simply saturate everything.
		assertTrue(index.cellsSpanned(0f, 0f, 4f, 4f, margin = 0f) < 100L)
	}

	@Test
	fun `construction arguments are validated rather than silently wrong`() {
		assertThrows(IllegalArgumentException::class.java) { MetaSpatialIndex(cellSize = 0f) }
		assertThrows(IllegalArgumentException::class.java) { MetaSpatialIndex(cellSize = -8f) }
		assertThrows(IllegalArgumentException::class.java) { MetaSpatialIndex(cellSize = Float.NaN) }
		// Not a power of two would make the bucket mask file entries into the wrong buckets, silently.
		assertThrows(IllegalArgumentException::class.java) { MetaSpatialIndex(cellSize = 8f, bucketCount = 1000) }
	}

	private companion object {
		const val SEED = 20260827L
		const val SCENES = 25
		const val FRAMES_PER_SCENE = 20
		val CELL_SIZES = floatArrayOf(16f, 64f, 200f)
	}
}
