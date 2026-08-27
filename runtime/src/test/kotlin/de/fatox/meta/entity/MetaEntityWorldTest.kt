package de.fatox.meta.entity

import com.badlogic.gdx.math.Vector3
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The invariants that keep a column layout debuggable.
 *
 * Every case here is a bug that SoA invites and that stays silent without a guard: an entity reading a neighbour's
 * position after a swap, a removed entity still resolving to a live slot, a system mutating the world it is
 * walking. None of them throws on its own, and all of them look correct at the call site - which is precisely why
 * they belong in tests rather than in a comment.
 */
class MetaEntityWorldTest {
	private class Probe(val name: String) : MetaEntity()

	private lateinit var world: MetaEntityWorld

	@BeforeEach
	fun setUp() {
		world = MetaEntityWorld(initialCapacity = 4)
		MetaEntityWorld.resetAccounting()
	}

	@AfterEach
	fun tearDown() {
		MetaEntityWorld.resetAccounting()
	}

	@Test
	fun `an entity reads and writes its own column values`() {
		val a = world.add(Probe("a"))
		val b = world.add(Probe("b"))

		a.setPosition(1f, 2f, 3f)
		b.setPosition(10f, 20f, 30f)

		assertEquals(1f, a.x); assertEquals(2f, a.y); assertEquals(3f, a.z)
		assertEquals(10f, b.x); assertEquals(20f, b.y); assertEquals(30f, b.z)
		// Scale defaults to 1, not 0: a zero-scale entity renders as nothing and that is hard to trace back here.
		assertEquals(1f, a.scale)
		world.validate()
	}

	@Test
	fun `removing an entity keeps every survivor pointing at its own data`() {
		// The core swap-remove hazard: the last entity moves into the freed slot, and if its slot index is not
		// patched it silently reads whatever the removed entity left behind.
		val entities = ArrayList<Probe>()
		for (index in 0 until 8) {
			val probe = world.add(Probe("e$index"))
			probe.setPosition(index.toFloat(), index * 2f, index * 3f)
			entities.add(probe)
		}

		world.remove(entities[2])
		world.remove(entities[0])

		world.validate()
		for (probe in entities) {
			if (probe === entities[2] || probe === entities[0]) continue
			val expected = probe.name.removePrefix("e").toFloat()
			assertEquals(expected, probe.x) { "${probe.name} is reading another entity's column data" }
			assertEquals(expected * 2f, probe.y)
			assertEquals(expected * 3f, probe.z)
		}
		assertEquals(6, world.size)
		assertEquals(6, world.store.count)
	}

	@Test
	fun `a removed entity throws instead of reading a reused slot`() {
		val a = world.add(Probe("a"))
		a.setPosition(5f, 5f, 5f)
		world.remove(a)
		assertFalse(a.isBound)

		// The slot is immediately reused; without the bound check `a.x` would quietly return b's position.
		val b = world.add(Probe("b"))
		b.setPosition(99f, 99f, 99f)

		val failure = assertThrows(IllegalStateException::class.java) { a.x }
		assertTrue(failure.message!!.contains("not in a world"))
		assertEquals(99f, b.x)
	}

	@Test
	fun `adding an entity that already belongs to a world is refused`() {
		val a = world.add(Probe("a"))
		val other = MetaEntityWorld()
		// Silently re-adding would leak the first slot and leave two worlds believing they own this entity.
		assertThrows(IllegalStateException::class.java) { other.add(a) }
	}

	@Test
	fun `the store grows without losing or reordering data`() {
		val entities = ArrayList<Probe>()
		// Capacity starts at 4, so this forces several reallocations.
		for (index in 0 until 33) {
			val probe = world.add(Probe("e$index"))
			probe.setPosition(index.toFloat(), 0f, 0f)
			entities.add(probe)
		}
		assertTrue(world.store.currentCapacity >= 33)
		world.validate()
		for (index in entities.indices) {
			assertEquals(index.toFloat(), entities[index].x) { "Entity $index lost its data across a grow" }
		}
	}

	@Test
	fun `a system iterating the columns cannot structurally change the world`() {
		repeat(4) { world.add(Probe("e$it")) }
		val extra = Probe("late")

		// Adding during a walk reallocates the columns; removing swaps a different entity into the index just
		// visited. Both corrupt a walk silently, so both throw.
		assertThrows(IllegalStateException::class.java) {
			world.store.forEachSlot { world.add(extra) }
		}
		assertThrows(IllegalStateException::class.java) {
			world.store.forEachSlot { world.remove(world.entities.get(0)) }
		}
	}

	@Test
	fun `bulk column iteration and facade access agree`() {
		val entities = ArrayList<Probe>()
		for (index in 0 until 16) {
			val probe = world.add(Probe("e$index"))
			probe.setPosition(index.toFloat(), 0f, 0f)
			probe.setVelocity(1f, 0f, 0f)
			entities.add(probe)
		}

		// The fast path, written the way a real system would write it.
		val dt = 0.5f
		val px = world.store.x
		val vx = world.store.vx
		world.store.forEachSlot { slot -> px[slot] += vx[slot] * dt }

		// The point of the whole design: the entity sees what the system wrote.
		for (index in entities.indices) {
			assertEquals(index + dt, entities[index].x) { "Column write not visible through the entity facade" }
		}
	}

	@Test
	fun `vectors are a boundary, not storage`() {
		val a = world.add(Probe("a"))
		a.setPosition(3f, 4f, 0f)
		val out = Vector3()

		assertEquals(5f, a.positionInto(out).len())
		// The returned vector is a copy: mutating it must not write back into the columns.
		out.set(100f, 100f, 100f)
		assertEquals(3f, a.x)
	}

	@Test
	fun `clear unbinds every entity`() {
		val a = world.add(Probe("a"))
		val b = world.add(Probe("b"))
		world.clear()

		assertEquals(0, world.size)
		assertEquals(0, world.store.count)
		assertFalse(a.isBound)
		assertFalse(b.isBound)
		assertThrows(IllegalStateException::class.java) { b.y }
	}

	@Test
	fun `a bulk loop through the facade is detected`() {
		repeat(64) { world.add(Probe("e$it")) }
		MetaEntityWorld.resetAccounting()

		// Exactly the mistake the design invites: correct, readable, and ~4x slower than a plain object layout.
		repeat(64) {
			for (index in 0 until world.entities.size) {
				val entity = world.entities.get(index)
				entity.x += entity.velocityX
			}
		}

		assertTrue(MetaEntityWorld.facadeAccessesThisFrame > MetaEntityWorld.BULK_ACCESS_WARNING_THRESHOLD) {
			"Facade accounting counted only ${MetaEntityWorld.facadeAccessesThisFrame}; the trap would go unreported"
		}
		MetaEntityWorld.endFrame()
		assertEquals(0, MetaEntityWorld.facadeAccessesThisFrame, "endFrame must reset the tally")
	}

	@Test
	fun `individual entity access stays well under the bulk threshold`() {
		val player = world.add(Probe("player"))
		MetaEntityWorld.resetAccounting()
		// A frame's worth of ordinary gameplay code touching a handful of entities.
		repeat(60) { player.setPosition(it.toFloat(), 0f, 0f) }
		assertTrue(MetaEntityWorld.facadeAccessesThisFrame < MetaEntityWorld.BULK_ACCESS_WARNING_THRESHOLD) {
			"Ordinary per-entity code must not trip the bulk-loop warning"
		}
	}
}
