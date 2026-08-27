package de.fatox.meta.entity

import com.badlogic.gdx.math.Vector3

/**
 * A thing in the world: a real object with identity, methods and whatever fields you give it - whose *transform*
 * happens to live in [MetaTransformStore]'s columns rather than in fields of its own.
 *
 * Subclass it and add what your game needs as ordinary fields:
 *
 * ```kotlin
 * class Asteroid(val model: ModelInstance) : MetaEntity() {
 *     var health = 100
 *     fun explode() { … }
 * }
 *
 * val rock = world.add(Asteroid(model))
 * rock.x = 12f                  // reads and writes the column
 * rock.setVelocity(0f, -9.8f, 0f)
 * ```
 *
 * ### When to use this and when to use the columns
 *
 * Use these accessors for **individual** entities - the player, a projectile you just spawned, the thing under the
 * cursor. Each access costs a couple of nanoseconds more than a plain field, which is irrelevant when you touch a
 * handful per frame.
 *
 * Do **not** loop over many entities through these accessors. Measured at 100k entities, that was 4x slower than
 * the plain object layout this replaces - it pays object chasing *and* column indirection, getting the worst of
 * both. Bulk work reads the columns:
 *
 * ```kotlin
 * val px = store.x; val vx = store.vx                    // hoist once
 * store.forEachSlot { i -> px[i] += vx[i] * dt }         // linear, vectorizable
 * ```
 *
 * [MetaEntityWorld.strictAccounting] exists to catch the mistake rather than trusting anyone to remember it.
 *
 * ### Lifetime
 *
 * An entity is *bound* between [MetaEntityWorld.add] and [MetaEntityWorld.remove]. Touching an unbound entity's
 * transform throws immediately and says so, rather than reading whatever now occupies its old slot - which is the
 * silent bug this layout would otherwise invite.
 */
abstract class MetaEntity {
	/**
	 * Where this entity's data sits, or [UNBOUND].
	 *
	 * Changes when another entity is removed and this one is swapped down to keep the columns dense, so it is not
	 * an identity and must never be stored. Hold the entity.
	 */
	@JvmField
	internal var slot: Int = UNBOUND

	@JvmField
	internal var store: MetaTransformStore? = null

	/** True while this entity has a slot, i.e. between `world.add` and `world.remove`. */
	val isBound: Boolean get() = slot != UNBOUND

	internal fun bind(store: MetaTransformStore, slot: Int) {
		this.store = store
		this.slot = slot
	}

	/** Called by the store when a swap moves this entity to a different slot. */
	internal fun rebind(slot: Int) {
		this.slot = slot
	}

	internal fun unbind() {
		store = null
		slot = UNBOUND
	}

	/**
	 * The store backing this entity, or a thrown explanation.
	 *
	 * Every accessor goes through here. A branch on a field that is almost always the same value costs close to
	 * nothing and predicts perfectly, and it converts "this entity silently reads another entity's position" into
	 * a message naming the class and the mistake.
	 */
	private fun requireStore(): MetaTransformStore {
		// Counted here rather than in each accessor: this is the one funnel every transform read and write passes
		// through, so the tally cannot drift out of step with the thing it is measuring.
		if (MetaEntityWorld.strictAccounting) MetaEntityWorld.facadeAccessesThisFrame++
		return store ?: throw IllegalStateException(
			"${this::class.simpleName} is not in a world, so it has no transform. Add it with world.add(entity) " +
				"before reading or writing its position; an entity that was removed must be added again.",
		)
	}

	var x: Float
		get() = requireStore().x[slot]
		set(value) { requireStore().x[slot] = value }

	var y: Float
		get() = requireStore().y[slot]
		set(value) { requireStore().y[slot] = value }

	var z: Float
		get() = requireStore().z[slot]
		set(value) { requireStore().z[slot] = value }

	var velocityX: Float
		get() = requireStore().vx[slot]
		set(value) { requireStore().vx[slot] = value }

	var velocityY: Float
		get() = requireStore().vy[slot]
		set(value) { requireStore().vy[slot] = value }

	var velocityZ: Float
		get() = requireStore().vz[slot]
		set(value) { requireStore().vz[slot] = value }

	var rotation: Float
		get() = requireStore().rotation[slot]
		set(value) { requireStore().rotation[slot] = value }

	var scale: Float
		get() = requireStore().scale[slot]
		set(value) { requireStore().scale[slot] = value }

	/** Sets all three position components in one bounds-checked step. */
	fun setPosition(x: Float, y: Float, z: Float = 0f) {
		val store = requireStore()
		store.x[slot] = x
		store.y[slot] = y
		store.z[slot] = z
	}

	/** Sets all three velocity components in one bounds-checked step. */
	fun setVelocity(x: Float, y: Float, z: Float = 0f) {
		val store = requireStore()
		store.vx[slot] = x
		store.vy[slot] = y
		store.vz[slot] = z
	}

	/**
	 * Copies this entity's position into [out] and returns it.
	 *
	 * The boundary with libGDX's vector types, which are the right tool for *transient* maths and the wrong one
	 * for storage - a `Vector3` field per entity is exactly the layout this class exists to avoid. Take a copy,
	 * compute, write back with [setPosition].
	 */
	fun positionInto(out: Vector3): Vector3 {
		val store = requireStore()
		return out.set(store.x[slot], store.y[slot], store.z[slot])
	}

	/** Copies this entity's velocity into [out] and returns it. See [positionInto]. */
	fun velocityInto(out: Vector3): Vector3 {
		val store = requireStore()
		return out.set(store.vx[slot], store.vy[slot], store.vz[slot])
	}

	/** Reads live column values, so an entity prints its real state in a debugger or a log. */
	override fun toString(): String {
		val store = store ?: return "${this::class.simpleName}(unbound)"
		return "${this::class.simpleName}(slot=$slot, pos=[${store.x[slot]}, ${store.y[slot]}, ${store.z[slot]}], " +
			"vel=[${store.vx[slot]}, ${store.vy[slot]}, ${store.vz[slot]}], rot=${store.rotation[slot]}, " +
			"scale=${store.scale[slot]})"
	}

	internal companion object {
		const val UNBOUND: Int = -1
	}
}
