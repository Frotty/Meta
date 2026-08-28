package de.fatox.meta.entity

import de.fatox.meta.test.AllocationProbe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

/**
 * The hook that carries an entity's own fields in the same snapshot as its transform.
 *
 * The failure this guards against is a game keeping two histories - transforms in Meta's snapshot, gameplay state
 * in its own - that drift apart by one frame and desync in a way that looks like a physics bug.
 */
class MetaEntityStateTest {
	private class Plain(val id: Int) : MetaEntity()

	/** Carries gameplay state, one snapshot-only field, and records when it was reconciled. */
	private class Stateful(
		val id: Int,
		var health: Int = 100,
		var fuse: Float = 0f,
		var localOnly: Int = 0,
	) : MetaEntity(), MetaEntityState {
		var restoredAt = -1
		var neighboursWhenRestored = -1

		override fun captureState(ints: IntArray, floats: FloatArray) {
			ints[0] = health
			ints[1] = localOnly
			floats[0] = fuse
		}

		override fun restoreState(ints: IntArray, floats: FloatArray) {
			health = ints[0]
			localOnly = ints[1]
			fuse = floats[0]
		}

		override fun onRestored() {
			restoredAt = reconcileTick++
			neighboursWhenRestored = store?.count ?: -1
		}

		/** Index 1 is per-machine, so it must survive a rollback and never reach a digest. */
		override val digestExcludedInts: Int get() = 1 shl 1

		companion object {
			var reconcileTick = 0
		}
	}

	@Test
	fun `custom state rides along with the transform`() {
		val world = MetaEntityWorld()
		val entities = (0 until 20).map { world.add(Stateful(it, health = 100 - it, fuse = it * 0.5f)) }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)

		for (entity in entities) {
			entity.health = -1
			entity.fuse = 999f
		}
		world.restoreFrom(snapshot)

		for ((index, entity) in entities.withIndex()) {
			assertEquals(100 - index, entity.health) { "Entity $index did not get its health back" }
			assertEquals(index * 0.5f, entity.fuse) { "Entity $index did not get its fuse back" }
		}
	}

	@Test
	fun `custom state follows its entity through a slot permutation`() {
		// The transform columns and the custom windows are both indexed by slot, so a restore that reunited an
		// entity with the wrong window would hand it a neighbour's health while its position looked perfect.
		val world = MetaEntityWorld()
		val entities = (0 until 10).map { world.add(Stateful(it, health = it * 10)) }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)

		world.remove(entities[2])
		world.remove(entities[5])
		for (entity in entities) entity.health = 0
		world.restoreFrom(snapshot)

		world.validate()
		for ((index, entity) in entities.withIndex()) {
			assertEquals(index * 10, entity.health) { "Entity $index came back with another entity's health" }
			assertEquals(index, (world.entityAt(index) as Stateful).id)
		}
	}

	@Test
	fun `reconciliation runs only once the whole world is back`() {
		// onRestored is specified to see a fully restored world, so anything reading a neighbour is safe. If it
		// were called inside the restore loop the early entities would observe a half-rolled-back world.
		Stateful.reconcileTick = 0
		val world = MetaEntityWorld()
		val entities = (0 until 12).map { world.add(Stateful(it)) }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)
		world.remove(entities[0])

		world.restoreFrom(snapshot)

		for (entity in entities) {
			assertTrue(entity.restoredAt >= 0) { "Entity ${entity.id} was never reconciled" }
			assertEquals(12, entity.neighboursWhenRestored) {
				"Entity ${entity.id} was reconciled while the world still held " +
					"${entity.neighboursWhenRestored} of 12 entities"
			}
		}
	}

	@Test
	fun `an excluded index is restored but never digested`() {
		val world = MetaEntityWorld()
		val entity = world.add(Stateful(0, health = 50, localOnly = 7))
		val before = world.digest()

		// Per-machine state changing must not read as a divergence...
		entity.localOnly = 4242
		assertEquals(before, world.digest()) {
			"A snapshot-only field changed the digest, which would desync peers that agree about the game"
		}
		// ...while shared state still does, so the exclusion did not switch custom state off wholesale.
		entity.health = 49
		assertNotEquals(before, world.digest())

		// And the excluded field still round-trips, because a snapshot needs everything to restore correctly.
		entity.localOnly = 7
		entity.health = 50
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)
		entity.localOnly = 0
		world.restoreFrom(snapshot)
		assertEquals(7, entity.localOnly) { "An excluded index was dropped from the snapshot as well" }
	}

	@Test
	fun `a snapshot hashes the same as the world it came from with custom state`() {
		val world = MetaEntityWorld()
		repeat(15) { world.add(Stateful(it, health = it, fuse = it * 1.5f, localOnly = it * 3)) }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)

		assertEquals(world.digest(MetaTransformColumns.ALL), snapshot.digest(MetaTransformColumns.ALL))
		assertEquals(world.digest(), snapshot.digest())
	}

	@Test
	fun `custom state changes the digest at all`() {
		// Guards the whole feature against being wired up but inert - every other test here would still pass.
		val world = MetaEntityWorld()
		val entity = world.add(Stateful(0, health = 10))
		val before = world.digest()
		entity.health = 11
		assertNotEquals(before, world.digest()) { "Custom state does not reach the digest" }
	}

	@Test
	fun `entities without the hook are unaffected by those with it`() {
		val world = MetaEntityWorld()
		val plain = world.add(Plain(0))
		val stateful = world.add(Stateful(1, health = 5))
		plain.setPosition(3f, 4f, 5f)
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)

		plain.setPosition(0f, 0f, 0f)
		stateful.health = 0
		world.restoreFrom(snapshot)

		assertEquals(3f, plain.x)
		assertEquals(5, stateful.health)
		world.validate()
	}

	@Test
	fun `one entity's leftovers do not become another's state`() {
		// The scratch buffers are shared between entities, so an implementation that writes fewer indices than the
		// one before it would otherwise inherit the difference - and restore values it never captured.
		class Sparse(val marker: Int) : MetaEntity(), MetaEntityState {
			var seen = IntArray(MetaEntityState.INTS)

			override fun captureState(ints: IntArray, floats: FloatArray) {
				// Only index 0, deliberately.
				ints[0] = marker
			}

			override fun restoreState(ints: IntArray, floats: FloatArray) {
				seen = ints.copyOf()
			}
		}

		val world = MetaEntityWorld()
		val greedy = world.add(Stateful(0, health = 77, localOnly = 88))
		val sparse = world.add(Sparse(5))
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)
		world.restoreFrom(snapshot)

		assertEquals(5, sparse.seen[0])
		for (index in 1 until MetaEntityState.INTS) {
			assertEquals(0, sparse.seen[index]) {
				"Index $index leaked from the previous entity's window (it held ${sparse.seen[index]})"
			}
		}
		assertEquals(77, greedy.health)
	}

	@Test
	fun `a world without the hook carries no custom footprint`() {
		val world = MetaEntityWorld()
		repeat(100) { world.add(Plain(it)) }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)

		assertTrue(snapshot.customInts.isEmpty()) { "Buffers were reserved for a game that does not use the hook" }
		assertTrue(!snapshot.hasCustomState)
	}

	@Test
	fun `reconciliation sees the rebuilt world, not just the restored columns`() {
		// The store's columns and the world's entity list are restored by different objects, so "the whole world
		// is back" has two halves. A callback firing between them sees restored transforms through a stale list:
		// still holding what the rollback killed, still missing what it brought back. Reading store.count would
		// not notice - it is set by the first half - so this reads the world the way a game actually would.
		class Watcher(val id: Int) : MetaEntity(), MetaEntityState {
			var world: MetaEntityWorld? = null
			var sizeSeen = -1
			var idsSeen: List<Int> = emptyList()

			override fun captureState(ints: IntArray, floats: FloatArray) {
				ints[0] = id
			}

			override fun restoreState(ints: IntArray, floats: FloatArray) = Unit

			override fun onRestored() {
				val world = world ?: return
				sizeSeen = world.size
				idsSeen = (0 until world.size).map { (world.entityAt(it) as Watcher).id }
			}
		}

		val world = MetaEntityWorld()
		val watchers = (0 until 6).map { world.add(Watcher(it)).also { w -> w.world = world } }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)

		// Both directions at once: two die and one is born, so a stale list is wrong in both.
		world.remove(watchers[1])
		world.remove(watchers[4])
		val newborn = Watcher(99).also { it.world = world }
		world.add(newborn)

		world.restoreFrom(snapshot)

		for (watcher in watchers) {
			assertEquals(6, watcher.sizeSeen) {
				"Watcher ${watcher.id} reconciled against a world of ${watcher.sizeSeen}, not the restored 6"
			}
			assertEquals(listOf(0, 1, 2, 3, 4, 5), watcher.idsSeen) {
				"Watcher ${watcher.id} saw ${watcher.idsSeen} - a list that does not match the restored world"
			}
		}
		// The entity the rollback undid is not reconciled at all, because it is not in the world any more.
		assertEquals(-1, newborn.sizeSeen) { "An entity removed by the rollback was still reconciled" }
	}

	@Test
	fun `which slot owns the custom state is part of the digest`() {
		// Skipping plain slots made the custom digest a hash of the *sequence* of stateful windows rather than of
		// where they sit, so moving the state to a different entity was invisible as long as the two entities'
		// transforms matched - which is exactly the case where the transform digest cannot separate them either.
		// Two peers could then attach the same gameplay state to different entities and agree.
		fun world(statefulFirst: Boolean): MetaEntityWorld {
			val world = MetaEntityWorld()
			if (statefulFirst) {
				world.add(Stateful(0, health = 5))
				world.add(Plain(1))
			} else {
				world.add(Plain(1))
				world.add(Stateful(0, health = 5))
			}
			// Identical transforms, so nothing else in the digest can tell these two worlds apart.
			for (index in 0 until world.size) world.entityAt(index).setPosition(0f, 0f, 0f)
			return world
		}

		assertNotEquals(world(statefulFirst = true).digest(), world(statefulFirst = false).digest()) {
			"Moving custom state to a different slot did not change the digest"
		}

		// The same must hold for a captured snapshot, which digests through a separate path.
		val first = MetaWorldSnapshot().also { world(statefulFirst = true).captureInto(it) }
		val second = MetaWorldSnapshot().also { world(statefulFirst = false).captureInto(it) }
		assertNotEquals(first.digest(), second.digest()) {
			"A snapshot's digest did not distinguish which slot owned the custom state"
		}
	}

	@Test
	fun `two windows differing only in which fields are excluded do not collide`() {
		// Excluded indices are skipped, so the mixer saw a *sequence* of surviving values with no record of which
		// fields they came from. [7, 9] excluding index 0 then presents the same sequence as [9, 7] excluding
		// index 1 - a different field is authoritative on each side and the digests agreed.
		//
		// It takes two peers whose masks differ, which the previous round established is possible: a mask may be
		// derived from configuration. Framing the mask itself settles it, and does so for every mask difference
		// rather than only for the arrangements that happen to collide.
		class Framed(val values: IntArray, override val digestExcludedInts: Int) : MetaEntity(), MetaEntityState {
			override fun captureState(ints: IntArray, floats: FloatArray) {
				values.copyInto(ints, 0, 0, values.size)
			}

			override fun restoreState(ints: IntArray, floats: FloatArray) = Unit
		}

		fun digestOf(values: IntArray, excluded: Int): Pair<Long, Long> {
			val world = MetaEntityWorld()
			world.add(Framed(values, excluded))
			val snapshot = MetaWorldSnapshot()
			world.captureInto(snapshot)
			return world.digest() to snapshot.digest()
		}

		val first = digestOf(intArrayOf(7, 9), excluded = 1 shl 0)
		val second = digestOf(intArrayOf(9, 7), excluded = 1 shl 1)

		assertNotEquals(first.first, second.first) {
			"Two entities with different authoritative fields produced the same live digest"
		}
		assertNotEquals(first.second, second.second) {
			"Two entities with different authoritative fields produced the same snapshot digest"
		}
	}

	@Test
	fun `reconciliation may not restructure the world it is reading`() {
		// notifyRestored walks slots, and a swap-remove moves the last entity into the slot the cursor has already
		// passed - so that entity would silently never be reconciled and would keep stale derived state. Meta's
		// convention for this is to refuse rather than half-support it, the same way forEachSlot does.
		class Suicidal : MetaEntity(), MetaEntityState {
			var world: MetaEntityWorld? = null

			override fun captureState(ints: IntArray, floats: FloatArray) = Unit

			override fun restoreState(ints: IntArray, floats: FloatArray) = Unit

			override fun onRestored() {
				world?.remove(this)
			}
		}

		val world = MetaEntityWorld()
		repeat(4) { world.add(Suicidal().also { it.world = world }) }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)

		assertThrows(IllegalStateException::class.java) { world.restoreFrom(snapshot) }

		// And the store is not left locked by the callback that threw, so the world stays usable.
		world.store.checkMutable("add an entity")
		assertEquals(4, world.size)
	}

	@Test
	fun `masks that exclude the same fields hash the same`() {
		// Framing the raw Int made two spellings of "exclude everything" - -1 and 0xFFFF - different hashes, even
		// though only the low INTS bits mean anything. Two peers whose authoritative state agrees perfectly would
		// then report a desync over bits that name no field at all.
		class Masked(override val digestExcludedInts: Int, override val digestExcludedFloats: Int) :
			MetaEntity(), MetaEntityState {
			override fun captureState(ints: IntArray, floats: FloatArray) {
				ints[0] = 3
				floats[0] = 1.5f
			}

			override fun restoreState(ints: IntArray, floats: FloatArray) = Unit
		}

		fun digestOf(excludedInts: Int, excludedFloats: Int): Pair<Long, Long> {
			val world = MetaEntityWorld()
			world.add(Masked(excludedInts, excludedFloats))
			val snapshot = MetaWorldSnapshot()
			world.captureInto(snapshot)
			return world.digest() to snapshot.digest()
		}

		val allBits = digestOf(-1, -1)
		val exactBits = digestOf((1 shl MetaEntityState.INTS) - 1, (1 shl MetaEntityState.FLOATS) - 1)
		assertEquals(allBits.first, exactBits.first) {
			"Two spellings of the same exclusion produced different live digests"
		}
		assertEquals(allBits.second, exactBits.second) {
			"Two spellings of the same exclusion produced different snapshot digests"
		}

		// A mask that genuinely names different fields must still differ, so this did not normalise it away.
		assertNotEquals(allBits.first, digestOf(0, 0).first)
	}

	@Test
	fun `restoring state may not restructure the world either`() {
		// Same walk-by-slot hazard as reconciliation, and worse here: the store has already rebound the snapshot
		// population while the world's entity list is still the pre-restore one, so a removal updates the two
		// against different layouts and the list rebuild that follows re-adds every snapshot owner regardless.
		class Meddling : MetaEntity(), MetaEntityState {
			var world: MetaEntityWorld? = null

			override fun captureState(ints: IntArray, floats: FloatArray) = Unit

			override fun restoreState(ints: IntArray, floats: FloatArray) {
				world?.remove(this)
			}
		}

		val world = MetaEntityWorld()
		repeat(4) { world.add(Meddling().also { it.world = world }) }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)

		assertThrows(IllegalStateException::class.java) { world.restoreFrom(snapshot) }

		// The lock is released, so the store is not wedged for everything that follows.
		world.store.checkMutable("add an entity")
		// And the world is still structurally sound. A hook throwing - this guard, or a caller's own validation -
		// must not leave the columns holding the snapshot's population while the entity list holds the old one:
		// size would disagree with count, and a later removal would swap the wrong list slot.
		world.validate()
		assertEquals(world.store.count, world.size)
	}

	@Test
	fun `a restore hook may traverse the store it is promised it can read`() {
		// Both hooks are documented as seeing a readable world, and forEachSlot is the documented way to read the
		// store - so blocking it with the same flag that blocks structural change made the contract contradict
		// itself, and any reconciliation that inspects its neighbours would abort every rollback.
		class Neighbourly : MetaEntity(), MetaEntityState {
			var seenDuringRestore = -1
			var seenDuringReconcile = -1

			override fun captureState(ints: IntArray, floats: FloatArray) = Unit

			override fun restoreState(ints: IntArray, floats: FloatArray) {
				var visited = 0
				store?.forEachSlot { visited++ }
				seenDuringRestore = visited
			}

			override fun onRestored() {
				var visited = 0
				store?.forEachSlot { visited++ }
				seenDuringReconcile = visited
			}
		}

		val world = MetaEntityWorld()
		val entities = (0 until 7).map { world.add(Neighbourly()) }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)
		world.remove(entities[3])

		world.restoreFrom(snapshot)

		for (entity in entities) {
			assertEquals(7, entity.seenDuringRestore) { "A restore hook could not walk the store" }
			assertEquals(7, entity.seenDuringReconcile) { "A reconciliation hook could not walk the store" }
		}
		world.validate()
	}

	@Test
	fun `a hook that digests between reading two fields still reads its own state`() {
		// Reading the world is now allowed, and digest and capture borrow the same scratch the hook is currently
		// holding. A hook that reads one field, digests, then reads another would take the second from whichever
		// entity the digest visited last - restoring corrupt state, silently, in the middle of a rollback.
		class Interleaving(val id: Int) : MetaEntity(), MetaEntityState {
			var world: MetaEntityWorld? = null
			var first = -1
			var second = -1
			var digestSeen = 0L

			override fun captureState(ints: IntArray, floats: FloatArray) {
				ints[0] = id
				ints[1] = id * 100
			}

			override fun restoreState(ints: IntArray, floats: FloatArray) {
				first = ints[0]
				// Anything that walks every entity through the shared scratch will do; digest is the one a game
				// reaches for, to log or compare mid-rollback.
				digestSeen = world?.digest() ?: 0L
				second = ints[1]
			}
		}

		val world = MetaEntityWorld()
		val entities = (0 until 5).map { world.add(Interleaving(it).also { e -> e.world = world }) }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)
		world.restoreFrom(snapshot)

		for (entity in entities) {
			assertEquals(entity.id, entity.first) { "Entity ${entity.id} read the wrong first field" }
			assertEquals(entity.id * 100, entity.second) {
				"Entity ${entity.id} read ${entity.second} after digesting, not ${entity.id * 100} - the nested " +
					"read overwrote the buffer it was holding"
			}
		}
		assertNotEquals(0L, entities[0].digestSeen)
	}

	@Test
	fun `capturing into the snapshot being restored is refused`() {
		// The per-depth scratch protects the buffers handed to a hook; it does nothing for the snapshot being read
		// from. A hook capturing into that same snapshot overwrites the windows of every slot not yet visited with
		// live, half-restored values, so those entities quietly keep their pre-rollback state. The restore reads
		// its source while callbacks run, so the source has to be off limits to them.
		class Recapturing : MetaEntity(), MetaEntityState {
			var world: MetaEntityWorld? = null
			var target: MetaWorldSnapshot? = null
			var health = 0

			override fun captureState(ints: IntArray, floats: FloatArray) {
				ints[0] = health
			}

			override fun restoreState(ints: IntArray, floats: FloatArray) {
				health = ints[0]
				target?.let { world?.captureInto(it) }
			}
		}

		val world = MetaEntityWorld()
		val entities = (0 until 5).map { world.add(Recapturing().also { it.world = world }) }
		entities.forEachIndexed { index, entity -> entity.health = index + 1 }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)

		entities.forEach { it.target = snapshot }
		assertThrows(IllegalStateException::class.java) { world.restoreFrom(snapshot) }

		// Refused, not tolerated - and the world is still sound, as any throwing hook must leave it.
		world.validate()
		assertEquals(5, world.size)

		// Capturing into a *different* snapshot from a hook stays legal; only the active source is protected.
		val other = MetaWorldSnapshot()
		entities.forEach { it.target = other }
		world.restoreFrom(snapshot)
		world.validate()
		for ((index, entity) in entities.withIndex()) assertEquals(index + 1, entity.health)
	}

	@Test
	fun `a throwing restore hook leaves the world structurally consistent`() {
		// Same hazard reached the ordinary way: an implementation that validates a captured value and rejects it.
		class Picky : MetaEntity(), MetaEntityState {
			var explode = false

			override fun captureState(ints: IntArray, floats: FloatArray) = Unit

			override fun restoreState(ints: IntArray, floats: FloatArray) {
				if (explode) error("that value is not acceptable")
			}
		}

		val world = MetaEntityWorld()
		val entities = (0 until 5).map { world.add(Picky()) }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)

		// Change the population, so a half-applied restore is visibly wrong rather than coincidentally fine.
		world.remove(entities[1])
		world.remove(entities[3])
		entities.forEach { it.explode = true }

		assertThrows(IllegalStateException::class.java) { world.restoreFrom(snapshot) }

		world.validate()
		assertEquals(5, world.size)
		assertEquals(5, world.store.count)
		for ((slot, entity) in entities.withIndex()) assertEquals(entity, world.entityAt(slot))
	}

	@Test
	fun `a retained snapshot keeps hashing the world it captured when a mask changes`() {
		// Nothing stops an implementation deriving an exclusion mask from mutable state - a debug toggle, a
		// per-machine setting. Reading it live at digest time would make a stored snapshot answer differently
		// depending on when it was asked, so a late desync check would report on a world that never existed.
		class Shifting(var excludeHealth: Boolean) : MetaEntity(), MetaEntityState {
			var health = 42

			override fun captureState(ints: IntArray, floats: FloatArray) {
				ints[0] = health
			}

			override fun restoreState(ints: IntArray, floats: FloatArray) {
				health = ints[0]
			}

			override val digestExcludedInts: Int get() = if (excludeHealth) 1 else 0
		}

		val world = MetaEntityWorld()
		val entity = world.add(Shifting(excludeHealth = false))
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)
		val captured = snapshot.digest()

		entity.excludeHealth = true

		assertEquals(captured, snapshot.digest()) {
			"A retained snapshot's digest moved when the live entity's exclusion mask changed"
		}
		// The live world is entitled to disagree - it is being hashed as it is now, and now the mask differs.
		assertNotEquals(captured, world.digest())
	}

	@Test
	fun `digesting a snapshot with custom state repeatedly allocates nothing`() {
		assumeTrue(AllocationProbe.isSupported, "This JVM cannot report per-thread allocation")
		val world = MetaEntityWorld()
		repeat(500) { world.add(Stateful(it, health = it, fuse = it * 0.5f)) }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)

		// Peer verification hashes retained snapshots every frame, so this runs as often as capture does.
		val bytes = AllocationProbe.measure(warmup = 50, iterations = 20) { snapshot.digest(MetaTransformColumns.ALL) }

		assertTrue(bytes <= 0) {
			"Digesting a snapshot allocated $bytes bytes per call, in the one path this class exists to keep free"
		}
	}

	@Test
	fun `capturing custom state allocates nothing`() {
		assumeTrue(AllocationProbe.isSupported, "This JVM cannot report per-thread allocation")
		val world = MetaEntityWorld()
		repeat(1_000) { world.add(Stateful(it, health = it)) }
		val snapshot = MetaWorldSnapshot()
		world.captureInto(snapshot)

		val bytes = AllocationProbe.measure(warmup = 50, iterations = 20) { world.captureInto(snapshot) }

		assertTrue(bytes <= 0) { "Capturing custom state allocated $bytes bytes per frame" }
	}

	// ---------------------------------------------------------------- differential

	@Test
	fun `a rolled back replay agrees with a straight run when entities carry their own state`() {
		// The transform version of this test cannot catch a custom-state bug, because it has none to lose. Here
		// health and fuse advance every frame, so a window restored to the wrong entity - or not at all - diverges.
		//
		// `localOnly` earns its keep here too: a pooled entity is respawned without resetting it, so it carries a
		// value that depends on how many previous lives it had - which a replay genuinely changes. It is excluded
		// from the digest, and this test passes *because* it is. Stop excluding it and this test fails, which is
		// the property worth having: excluded state is allowed to diverge, and the digest is allowed not to care.
		val straight = replay(rollbackEvery = 0, depth = 0)
		for (depth in intArrayOf(1, 4, 6)) {
			for (every in intArrayOf(3, 7)) {
				val replayed = replay(rollbackEvery = every, depth = depth)
				for (frame in straight.indices) {
					assertEquals(straight[frame], replayed[frame]) {
						"Digests diverged at frame $frame with depth $depth every $every frames"
					}
				}
			}
		}
	}

	private fun replay(rollbackEvery: Int, depth: Int): LongArray {
		val world = MetaEntityWorld()
		val pool = (0 until 48).map { Stateful(it) }
		val history = Array(depth + 1) { MetaWorldSnapshot() }
		val digests = LongArray(FRAMES)
		var frame = 0
		var frontier = 0
		var lastRewoundAt = -1

		while (frame < FRAMES) {
			if (rollbackEvery > 0 &&
				frame == frontier &&
				frame > depth &&
				frame % rollbackEvery == 0 &&
				frame != lastRewoundAt
			) {
				lastRewoundAt = frame
				val target = history[frame % history.size]
				val rewindTo = target.frame
				world.restoreFrom(target)
				frame = rewindTo
			}
			history[frame % history.size].let {
				world.captureInto(it)
				it.frame = frame
			}

			val store = world.store
			for (slot in 0 until store.count) {
				store.x[slot] += store.vx[slot]
				if (store.x[slot] > 60f || store.x[slot] < -60f) store.vx[slot] = -store.vx[slot]
			}
			for (index in 0 until world.size) {
				val entity = world.entityAt(index) as Stateful
				entity.health -= 1
				entity.fuse += 0.25f
				entity.localOnly = entity.localOnly xor frame
			}
			val roll = mix(frame)
			val candidate = pool[(roll ushr 8) % pool.size]
			if (!candidate.isBound) {
				world.add(candidate)
				candidate.setPosition((roll % 120) - 60f, 0f, 0f)
				candidate.setVelocity(((roll ushr 12) % 5) - 2f, 0f, 0f)
				candidate.health = 100
				candidate.fuse = 0f
			}
			if (roll % 4 == 0 && world.size > 2) world.remove(world.entityAt((roll ushr 20) % world.size))

			digests[frame] = world.digest(MetaTransformColumns.ALL)
			frame++
			if (frame > frontier) frontier = frame
		}
		if (rollbackEvery > 0) check(lastRewoundAt >= 0) { "This configuration never rewound, so it proves nothing" }
		return digests
	}

	private fun mix(value: Int): Int {
		var h = value * -0x61c88647
		h = h xor (h ushr 15)
		h *= -0x7ee3623b
		h = h xor (h ushr 13)
		return h and 0x7fffffff
	}

	private companion object {
		const val FRAMES = 160
	}
}
