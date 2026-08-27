package de.fatox.meta.entity

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * Runs random operation sequences against the column store and an obviously-correct reference model, and asserts
 * they never disagree.
 *
 * ### Why a model rather than more scenarios
 *
 * The column layout trades a simple representation for speed, and every bug it invites is silent: a swap that does
 * not patch a slot, a grow that reorders, a removal that leaves the two views disagreeing. Hand-written cases only
 * cover the sequences someone thought of, and the failure that matters is the one nobody thought of - the
 * fourteenth removal after a grow, with the last entity being the one removed.
 *
 * So the reference model here is deliberately stupid: a `LinkedHashMap` from entity to its transform, which is
 * plainly right and hopelessly slow. Anything the store does must be indistinguishable from it. That turns
 * correctness into a property checked over thousands of generated sequences rather than a list of examples.
 *
 * Seeded, so a failure reproduces exactly; the seed prints with the result.
 */
class MetaEntityWorldDifferentialTest {
	private class Probe(val id: Int) : MetaEntity()

	private class Transform(var x: Float, var y: Float, var z: Float, var vx: Float, var scale: Float)

	@BeforeEach
	fun setUp() = MetaEntityWorld.resetAccounting()

	@AfterEach
	fun tearDown() = MetaEntityWorld.resetAccounting()

	@Test
	fun `the store is indistinguishable from a naive model under random operation sequences`() {
		val random = Random(SEED)
		// Starts small so growth happens early and often rather than once at the end.
		val world = MetaEntityWorld(initialCapacity = 2)
		val model = LinkedHashMap<Probe, Transform>()
		var nextId = 0
		var adds = 0
		var removes = 0
		var clears = 0
		var writes = 0

		repeat(OPERATIONS) { step ->
			when (random.nextInt(100)) {
				// Weighted towards a live population rather than an empty or ever-growing one.
				in 0..44 -> {
					val probe = world.add(Probe(nextId++))
					val t = Transform(0f, 0f, 0f, 0f, 1f)
					model[probe] = t
					adds++
				}
				in 45..74 -> {
					if (model.isNotEmpty()) {
						val victim = model.keys.elementAt(random.nextInt(model.size))
						assertTrue(world.remove(victim)) { "seed=$SEED step=$step: remove rejected a live entity" }
						model.remove(victim)
						removes++
					}
				}
				in 75..96 -> {
					if (model.isNotEmpty()) {
						val target = model.keys.elementAt(random.nextInt(model.size))
						val x = random.nextInt(-500, 500).toFloat()
						val y = random.nextInt(-500, 500).toFloat()
						val z = random.nextInt(-500, 500).toFloat()
						val vx = random.nextInt(-50, 50).toFloat()
						val scale = random.nextInt(1, 8).toFloat()
						target.setPosition(x, y, z)
						target.velocityX = vx
						target.scale = scale
						model[target]!!.apply {
							this.x = x; this.y = y; this.z = z; this.vx = vx; this.scale = scale
						}
						writes++
					}
				}
				else -> {
					world.clear()
					model.keys.forEach { }
					model.clear()
					clears++
				}
			}

			// The store's own cross-check, plus the differential one against the model.
			world.validate()
			assertEquals(model.size, world.size) { "seed=$SEED step=$step: size disagreed with the model" }
			assertEquals(model.size, world.store.count) { "seed=$SEED step=$step: column count disagreed" }

			for ((entity, expected) in model) {
				assertTrue(entity.isBound) { "seed=$SEED step=$step: entity ${entity.id} lost its binding" }
				assertEquals(expected.x, entity.x) { "seed=$SEED step=$step: entity ${entity.id} x diverged" }
				assertEquals(expected.y, entity.y) { "seed=$SEED step=$step: entity ${entity.id} y diverged" }
				assertEquals(expected.z, entity.z) { "seed=$SEED step=$step: entity ${entity.id} z diverged" }
				assertEquals(expected.vx, entity.velocityX) { "seed=$SEED step=$step: entity ${entity.id} vx diverged" }
				assertEquals(expected.scale, entity.scale) { "seed=$SEED step=$step: entity ${entity.id} scale diverged" }
			}
		}

		// A sequence that never grew, never removed or never cleared would pass while testing almost nothing.
		assertTrue(world.store.currentCapacity > 2) { "seed=$SEED: the store never grew" }
		assertTrue(removes > 50) { "seed=$SEED: only $removes removals; swap-remove barely exercised" }
		assertTrue(clears > 0) { "seed=$SEED: clear was never exercised" }
		println("[differential] seed=$SEED adds=$adds removes=$removes writes=$writes clears=$clears " +
			"finalSize=${world.size} capacity=${world.store.currentCapacity}")
	}

	@Test
	fun `bulk column reads match the model after arbitrary churn`() {
		val random = Random(SEED + 1)
		val world = MetaEntityWorld(initialCapacity = 2)
		val model = LinkedHashMap<Probe, Float>()
		var nextId = 0

		repeat(OPERATIONS / 2) {
			if (model.isEmpty() || random.nextBoolean()) {
				val probe = world.add(Probe(nextId++))
				val vx = random.nextInt(-20, 20).toFloat()
				probe.setVelocity(vx, 0f, 0f)
				model[probe] = 0f
			} else {
				val victim = model.keys.elementAt(random.nextInt(model.size))
				world.remove(victim)
				model.remove(victim)
			}
		}

		// The fast path a real system uses, run over a store that has been churned rather than freshly filled.
		val dt = 0.25f
		val px = world.store.x
		val vx = world.store.vx
		world.store.forEachSlot { slot -> px[slot] += vx[slot] * dt }
		for (entity in model.keys) model[entity] = model[entity]!! + entity.velocityX * dt

		for ((entity, expected) in model) {
			assertEquals(expected, entity.x) {
				"seed=${SEED + 1}: entity ${entity.id} disagreed after a bulk column pass over a churned store"
			}
		}
		world.validate()
	}

	private companion object {
		const val SEED = 20260827L
		const val OPERATIONS = 1_200
	}
}
