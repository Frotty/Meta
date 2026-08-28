package de.fatox.meta.entity

import de.fatox.meta.test.AllocationProbe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

/**
 * The rollback bridge, checked the way a rollback engine actually fails.
 *
 * Rollback bugs do not throw. A restore that puts the right numbers in the wrong slots leaves a world that is
 * internally consistent, passes [MetaEntityWorld.validate], renders plausibly, and disagrees with the other peer -
 * so the tests here compare a *replayed* simulation against a straight one frame by frame, rather than asserting
 * that a restore "looks right". That is the only shape of test that catches the cases nobody thought of.
 */
class MetaWorldSnapshotTest {
	private class Probe(val id: Int) : MetaEntity()

	// ---------------------------------------------------------------- round trip

	@Test
	fun `a restored world is indistinguishable from the one captured`() {
		val world = MetaEntityWorld()
		val probes = (0 until 50).map { world.add(Probe(it)) }
		for ((index, probe) in probes.withIndex()) {
			probe.setPosition(index * 3f, index * -7f, index * 0.5f)
			probe.setVelocity(index * 0.25f, 1f, -index.toFloat())
			probe.rotation = index * 11f
			probe.scale = 1f + index
		}
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)
		val before = world.digest(MetaTransformColumns.ALL)

		// Move everything, kill some of it, spawn more: the state has nothing in common with the capture.
		for (probe in probes) probe.setPosition(-1f, -1f, -1f)
		for (index in 0 until 20) world.remove(probes[index])
		repeat(10) { world.add(Probe(1000 + it)) }
		assertNotEquals(before, world.digest(MetaTransformColumns.ALL))

		world.restoreFrom(snapshot)

		assertEquals(before, world.digest(MetaTransformColumns.ALL))
		assertEquals(50, world.size)
		world.validate()
		for ((index, probe) in probes.withIndex()) {
			assertEquals(index, (world.entityAt(index) as Probe).id)
			assertEquals(index * 3f, probe.x)
			assertEquals(index * -7f, probe.y)
			assertEquals(index * 0.5f, probe.z)
			assertEquals(index * 0.25f, probe.velocityX)
			assertEquals(index * 11f, probe.rotation)
			assertEquals(1f + index, probe.scale)
		}
	}

	@Test
	fun `rolling back over a removal restores the entity and everyone else's slot`() {
		// The case that makes slot bindings state rather than bookkeeping. Removing entity 1 swaps the last entity
		// down into slot 1, so after the removal the columns hold a different permutation entirely. A restore that
		// put the numbers back but not the bindings would leave every entity reading a neighbour's transform - and
		// nothing would look wrong until two peers disagreed about where things are.
		val world = MetaEntityWorld()
		val probes = (0 until 8).map { world.add(Probe(it)) }
		for ((index, probe) in probes.withIndex()) probe.setPosition(index.toFloat(), 0f, 0f)
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)

		world.remove(probes[1])
		// Swap-remove moved the last entity into the hole, so the permutation is now genuinely different.
		assertEquals(7, (world.entityAt(1) as Probe).id)
		assertEquals(7, world.size)

		world.restoreFrom(snapshot)

		world.validate()
		assertEquals(8, world.size)
		for ((index, probe) in probes.withIndex()) {
			assertTrue(probe.isBound) { "Probe $index was left unbound by the restore" }
			assertEquals(index, (world.entityAt(index) as Probe).id) { "Slot $index holds the wrong entity" }
			assertEquals(index.toFloat(), probe.x) { "Probe $index came back with another entity's position" }
		}
	}

	@Test
	fun `rolling back over a spawn removes the entity from the world`() {
		val world = MetaEntityWorld()
		val original = world.add(Probe(0))
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)

		val spawned = world.add(Probe(1))
		assertTrue(spawned.isBound)

		world.restoreFrom(snapshot)

		world.validate()
		assertEquals(1, world.size)
		assertTrue(original.isBound)
		assertFalse(spawned.isBound) { "An entity spawned after the capture survived the rollback" }
		// And it is genuinely detached, not merely flagged: reading its transform explains the mistake.
		assertThrows(IllegalStateException::class.java) { spawned.x }
	}

	// ---------------------------------------------------------------- the differential test

	/**
	 * A rolled-back replay must agree with a straight run, frame for frame.
	 *
	 * This is the property GGPO actually needs and the one no unit test of `restore` can stand in for: the engine
	 * saves, predicts, discovers it was wrong, restores and re-simulates, and every one of those re-simulations
	 * must land on the state the straight run reached. Anything the bridge fails to restore - a slot permutation,
	 * a stale count, a column - shows up as two digests that stop matching, at the exact frame it went wrong.
	 *
	 * The simulation spawns and despawns from the frame number alone, so the replay makes the same decisions, and
	 * it reuses a fixed pool of entities the way a rollback game does rather than allocating new ones.
	 */
	@Test
	fun `a rolled back replay agrees with a straight run frame for frame`() {
		val straight = runFrames(rollbackEvery = 0, rollbackDepth = 0)
		for (depth in intArrayOf(1, 2, 5, 7)) {
			for (every in intArrayOf(3, 8, 11)) {
				val replayed = runFrames(rollbackEvery = every, rollbackDepth = depth)
				for (frame in straight.indices) {
					assertEquals(straight[frame], replayed[frame]) {
						"Digests diverged at frame $frame replaying with depth $depth every $every frames"
					}
				}
			}
		}
	}

	/** Digest per frame of a run that periodically rolls back [rollbackDepth] frames and re-simulates. */
	private fun runFrames(rollbackEvery: Int, rollbackDepth: Int): LongArray {
		val world = MetaEntityWorld()
		val pool = (0 until POOL).map { Probe(it) }
		val history = Array(rollbackDepth + 1) { MetaWorldSnapshot() }
		val digests = LongArray(FRAMES)

		var frame = 0
		// Rewinds happen only at the frontier and only once per frontier frame, which is both how a rollback
		// engine behaves - new input arrives at the leading edge, never during a re-simulation - and what keeps
		// this loop finite. Without the frontier test, walking back up to the rewind point satisfies the same
		// condition again and the replay rewinds forever.
		var frontier = 0
		var lastRewoundAt = -1
		while (frame < FRAMES) {
			if (rollbackEvery > 0 &&
				frame == frontier &&
				frame > rollbackDepth &&
				frame % rollbackEvery == 0 &&
				frame != lastRewoundAt
			) {
				lastRewoundAt = frame
				// Rewind and re-simulate. The digests recorded on the way back up are overwritten by the replayed
				// values, so a divergence cannot be masked by the straight-run values already sitting there.
				val target = history[frame % history.size]
				val rewindTo = target.frame
				world.restoreFrom(target)
				frame = rewindTo
			}
			history[frame % history.size].let { slot ->
				world.captureInto(slot)
				slot.frame = frame
			}
			step(world, pool, frame)
			digests[frame] = world.digest(MetaTransformColumns.ALL)
			frame++
			if (frame > frontier) frontier = frame
		}
		// A replay that never actually rewound would agree with the straight run trivially.
		if (rollbackEvery > 0) {
			check(lastRewoundAt >= 0) { "This configuration never rewound, so it proves nothing" }
		}
		return digests
	}

	/** One tick: everything drifts, and membership changes as a pure function of the frame number. */
	private fun step(world: MetaEntityWorld, pool: List<Probe>, frame: Int) {
		val store = world.store
		for (slot in 0 until store.count) {
			store.x[slot] += store.vx[slot]
			store.y[slot] += store.vy[slot]
			if (store.x[slot] > 100f || store.x[slot] < -100f) store.vx[slot] = -store.vx[slot]
			if (store.y[slot] > 100f || store.y[slot] < -100f) store.vy[slot] = -store.vy[slot]
			store.rotation[slot] += 0.5f
		}
		// Membership decided by the frame alone, so a replay makes the same choices without carrying a PRNG.
		val roll = mix(frame)
		if (roll % 3 != 0) {
			val candidate = pool[(roll ushr 8) % POOL]
			if (!candidate.isBound) {
				world.add(candidate)
				candidate.setPosition((roll % 200) - 100f, ((roll ushr 4) % 200) - 100f, 0f)
				candidate.setVelocity(((roll ushr 12) % 7) - 3f, ((roll ushr 16) % 7) - 3f, 0f)
				candidate.rotation = (roll % 360).toFloat()
			}
		}
		if (roll % 5 == 0 && world.size > 2) {
			val victim = world.entityAt((roll ushr 20) % world.size)
			world.remove(victim)
		}
	}

	/** A cheap avalanche so consecutive frames make unrelated decisions. Not security, just spread. */
	private fun mix(value: Int): Int {
		var h = value * -0x61c88647
		h = h xor (h ushr 15)
		h *= -0x7ee3623b
		h = h xor (h ushr 13)
		return h and 0x7fffffff
	}

	// ---------------------------------------------------------------- digest

	@Test
	fun `the digest is bit exact rather than approximate`() {
		val world = MetaEntityWorld()
		val probe = world.add(Probe(0))
		probe.setPosition(1f, 2f, 3f)
		val before = world.digest(MetaTransformColumns.ALL)

		// One ulp. An epsilon comparison would call these equal; two peers running different trig would not.
		probe.x = Math.nextUp(1f)

		assertNotEquals(before, world.digest(MetaTransformColumns.ALL)) {
			"A one-ulp divergence did not change the digest, so it cannot detect a StrictMath-vs-Math desync"
		}
	}

	@Test
	fun `the digest covers only the columns asked for`() {
		val world = MetaEntityWorld()
		val probe = world.add(Probe(0))
		probe.setPosition(1f, 2f, 3f)
		probe.scale = 1f
		val simulation = world.digest(MetaTransformColumns.SIMULATION)
		val all = world.digest(MetaTransformColumns.ALL)

		// Scale is the presentation column a game tweens for a hit flash; it must not fire a desync alarm.
		probe.scale = 12.5f
		assertEquals(simulation, world.digest(MetaTransformColumns.SIMULATION)) {
			"Changing scale changed the SIMULATION digest, which would desync peers that agree about the game"
		}
		assertNotEquals(all, world.digest(MetaTransformColumns.ALL)) {
			"Changing scale did not change the ALL digest, so the column is not covered at all"
		}
		// Position still counts, so the exclusion did not simply switch the whole thing off.
		probe.x = 99f
		assertNotEquals(simulation, world.digest(MetaTransformColumns.SIMULATION))
	}

	@Test
	fun `a snapshot hashes the same as the world it came from`() {
		val world = MetaEntityWorld()
		repeat(30) { index ->
			world.add(Probe(index)).apply {
				setPosition(index * 1.5f, index * -2.5f, index.toFloat())
				setVelocity(index * 0.1f, 0f, 0f)
			}
		}
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)

		for (columns in intArrayOf(MetaTransformColumns.SIMULATION, MetaTransformColumns.ALL)) {
			assertEquals(world.digest(columns), snapshot.digest(columns)) {
				"A snapshot disagreed with its own world, so a history cannot be compared without restoring it"
			}
		}
	}

	@Test
	fun `the count is part of the digest`() {
		// Two worlds agreeing entity-for-entity over a common prefix are still different worlds. Without the count
		// in the hash, a peer that failed to spawn something would match until that entity happened to move.
		val shorter = MetaEntityWorld()
		val longer = MetaEntityWorld()
		repeat(4) { shorter.add(Probe(it)) }
		repeat(5) { longer.add(Probe(it)) }

		assertNotEquals(shorter.digest(), longer.digest())
	}

	// ---------------------------------------------------------------- cost and lifetime

	@Test
	fun `capturing a sized snapshot allocates nothing`() {
		assumeTrue(AllocationProbe.isSupported, "This JVM cannot report per-thread allocation")
		val world = MetaEntityWorld()
		repeat(2_000) { world.add(Probe(it)).setPosition(it.toFloat(), 0f, 0f) }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)

		val bytes = AllocationProbe.measure(warmup = 50, iterations = 20) { world.captureInto(snapshot) }

		assertTrue(bytes <= 0) {
			"Capturing allocated $bytes bytes. A rollback engine captures every frame, so at 60fps this is " +
				"${bytes * 60 / 1024} KB/s of garbage produced by the thing meant to hide network stalls."
		}
	}

	@Test
	fun `reusing a snapshot for a smaller world stops pinning the entities it dropped`() {
		// The snapshot holds strong references so a rollback can resurrect the dead. That is right, and it is also
		// exactly how a window of snapshots turns into a leak: reuse one for a smaller scene without clearing the
		// tail and it keeps every entity from the larger one alive for as long as the ring lives.
		val world = MetaEntityWorld()
		val probes = (0 until 20).map { world.add(Probe(it)) }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)

		for (index in 5 until 20) world.remove(probes[index])
		world.captureInto(snapshot)

		assertEquals(5, snapshot.count)
		for (slot in 5 until 20) {
			assertNull(snapshot.owners[slot]) { "Slot $slot still pins an entity the snapshot no longer describes" }
		}
		// The live range is untouched by the clearing.
		for (slot in 0 until 5) assertSame(probes[slot], snapshot.owners[slot])
	}

	@Test
	fun `releasing retained entities empties the snapshot without dropping its buffers`() {
		val world = MetaEntityWorld()
		repeat(10) { world.add(Probe(it)) }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)
		val capacity = snapshot.capacity

		snapshot.releaseRetainedEntities()

		assertEquals(0, snapshot.count)
		assertEquals(capacity, snapshot.capacity)
		for (slot in 0 until 10) assertNull(snapshot.owners[slot])
	}

	@Test
	fun `a snapshot grows to fit a world larger than its initial capacity`() {
		val world = MetaEntityWorld()
		val snapshot = MetaWorldSnapshot(initialCapacity = 4)
		repeat(300) { world.add(Probe(it)).setPosition(it.toFloat(), 0f, 0f) }

		world.captureInto(snapshot)

		assertEquals(300, snapshot.count)
		assertTrue(snapshot.capacity >= 300)
		assertEquals(world.digest(MetaTransformColumns.ALL), snapshot.digest(MetaTransformColumns.ALL))
	}

	@Test
	fun `restoring into a world smaller than the snapshot grows the columns`() {
		val big = MetaEntityWorld(initialCapacity = 512)
		val probes = (0 until 400).map { big.add(Probe(it)) }
		for ((index, probe) in probes.withIndex()) probe.setPosition(index.toFloat(), 0f, 0f)
		val snapshot = MetaWorldSnapshot()
		big.captureInto(snapshot)

		val small = MetaEntityWorld(initialCapacity = 8)
		// The entities have to leave the first world before the second can bind them.
		big.clear()
		small.restoreFrom(snapshot)

		small.validate()
		assertEquals(400, small.size)
		for ((index, probe) in probes.withIndex()) assertEquals(index.toFloat(), probe.x)
	}

	// ---------------------------------------------------------------- refusals

	@Test
	fun `restoring while a system is iterating is refused before anything changes`() {
		val world = MetaEntityWorld()
		repeat(4) { world.add(Probe(it)).setPosition(it.toFloat(), 0f, 0f) }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)
		world.add(Probe(99))

		val sizeBefore = world.size
		val digestBefore = world.digest(MetaTransformColumns.ALL)
		world.store.forEachSlot {
			assertThrows(IllegalStateException::class.java) { world.restoreFrom(snapshot) }
		}

		// The point of refusing early: the world is exactly as it was, not half restored.
		assertEquals(sizeBefore, world.size)
		assertEquals(digestBefore, world.digest(MetaTransformColumns.ALL))
		world.validate()
	}

	@Test
	fun `restoring a snapshot into a different world is refused rather than silently splitting an entity`() {
		// Binding these into a second world would re-point them at its columns while the first world carries on
		// listing them, so two worlds would own one entity. Nothing throws at that moment; the first symptom is a
		// transform read that belongs to another scene entirely.
		val original = MetaEntityWorld()
		repeat(6) { original.add(Probe(it)).setPosition(it.toFloat(), 0f, 0f) }
		val snapshot = MetaWorldSnapshot()
		original.captureInto(snapshot)

		val other = MetaEntityWorld()
		assertThrows(IllegalStateException::class.java) { other.restoreFrom(snapshot) }

		// Refused before anything moved, in both worlds.
		assertEquals(0, other.size)
		assertEquals(6, original.size)
		original.validate()

		// And it is a guard against sharing, not against reuse: once the first world lets go, the restore works.
		original.clear()
		other.restoreFrom(snapshot)
		other.validate()
		assertEquals(6, other.size)
		for (slot in 0 until 6) assertEquals(slot.toFloat(), (other.entityAt(slot) as Probe).x)
	}

	private companion object {
		const val FRAMES = 240
		const val POOL = 64
	}
}
