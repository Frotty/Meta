package de.fatox.meta.entity

import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.warn

private val log = MetaLoggerFactory.logger {}

/**
 * The entities in a scene, and the columns holding their transforms.
 *
 * ```kotlin
 * val world = MetaEntityWorld()
 * val rock = world.add(Asteroid(model))
 * rock.setPosition(3f, 0f, 0f)
 *
 * // A system: linear over the columns, no entity objects touched.
 * val px = world.store.x; val vx = world.store.vx
 * world.store.forEachSlot { i -> px[i] += vx[i] * dt }
 *
 * world.remove(rock)      // rock is unbound; touching its transform now throws
 * ```
 *
 * ### Two views of the same data, on purpose
 *
 * [store] is the fast one and [entities] is the convenient one. They are not alternatives to pick between: a
 * system that runs over everything every frame reads the store, and code that deals with one entity reads the
 * entity. The measured cost of confusing the two is severe enough that [strictAccounting] watches for it.
 */
class MetaEntityWorld(initialCapacity: Int = MetaTransformStore.DEFAULT_CAPACITY) {
	/** The transform columns. Read these in systems; see [MetaTransformStore]. */
	@JvmField
	val store: MetaTransformStore = MetaTransformStore(initialCapacity)

	private val liveEntities = Array<MetaEntity>(initialCapacity)

	/**
	 * Every live entity, in slot order.
	 *
	 * Iterate this to act on entities *as objects* - calling their methods, reading their own fields. Do not
	 * iterate it to read transforms in bulk; that is what [store] is for, and it is roughly an order of magnitude
	 * quicker.
	 */
	val entities: Array<MetaEntity> get() = liveEntities

	/** How many entities are live. Always equal to `store.count`. */
	val size: Int get() = liveEntities.size

	/**
	 * Adds [entity], binds it to a slot and returns it for chaining.
	 *
	 * Adding an entity that is already in a world is a mistake rather than a no-op: it would leak its old slot and
	 * leave two owners believing they hold it.
	 */
	fun <E : MetaEntity> add(entity: E): E {
		check(!entity.isBound) {
			"${entity::class.simpleName} is already in a world. Remove it before adding it somewhere else."
		}
		val slot = store.allocate(entity)
		entity.bind(store, slot)
		liveEntities.add(entity)
		return entity
	}

	/**
	 * Removes [entity], frees its slot and unbinds it.
	 *
	 * Removal is O(1): the last entity is swapped into the freed slot in both the columns and [entities], and its
	 * slot index is patched. Nothing else is safe to assume about ordering after a removal.
	 */
	fun remove(entity: MetaEntity): Boolean {
		if (!entity.isBound || entity.store !== store) return false
		val slot = entity.slot
		val last = liveEntities.size - 1
		// Mirror the store's swap so entities and slots stay in lockstep; the store patches the moved entity.
		liveEntities.set(slot, liveEntities.get(last))
		liveEntities.removeIndex(last)
		store.release(slot)
		entity.unbind()
		return true
	}

	/** Removes every entity and unbinds all of them. */
	fun clear() {
		store.clear()
		liveEntities.clear()
	}

	/**
	 * Reports whether [entities] and the columns agree.
	 *
	 * The one invariant this design can violate silently: a swap that patched one and not the other leaves an
	 * entity reading a different entity's transform, and nothing about that looks wrong at the call site. Cheap
	 * enough to assert at the end of a frame while a scene is being brought up.
	 */
	fun validate() {
		check(liveEntities.size == store.count) {
			"Entity list holds ${liveEntities.size} but the columns hold ${store.count}"
		}
		for (slot in 0 until liveEntities.size) {
			val entity = liveEntities.get(slot)
			check(entity.slot == slot) {
				"${entity::class.simpleName} thinks it is in slot ${entity.slot} but the world has it at $slot"
			}
			check(store.ownerOf(slot) === entity) {
				"Slot $slot is owned by ${store.ownerOf(slot)} in the columns but ${entity::class.simpleName} in " +
					"the world"
			}
		}
	}

	companion object {
		/**
		 * Warns once when transforms are read through [MetaEntity] often enough to look like a bulk loop.
		 *
		 * The performance trap in this design is invisible: `for (e in world.entities) e.x += e.velocityX * dt`
		 * is the obvious thing to write, reads perfectly well, and measured **4x slower than the plain object
		 * layout it replaces**. No test catches it, because it is correct.
		 *
		 * So the runtime counts facade accesses per frame and says something when the count only makes sense as a
		 * loop. On by default because the cost is one increment and the alternative is that nobody finds out;
		 * disable with `-Dmeta.entities.strict=false` once a project has its own profiling.
		 */
		@JvmStatic
		var strictAccounting: Boolean = System.getProperty("meta.entities.strict") != "false"

		/** Accesses in the current frame. Reset by [endFrame]. */
		@JvmStatic
		var facadeAccessesThisFrame: Int = 0
			internal set

		private var warned = false

		/**
		 * Call once per frame. Reports a facade access count that can only be a bulk loop, then resets it.
		 *
		 * Warns once rather than every frame: this is a design smell to go and fix, not a condition to monitor,
		 * and a message repeating sixty times a second is one people turn logging off to escape.
		 */
		@JvmStatic
		fun endFrame() {
			if (strictAccounting && !warned && facadeAccessesThisFrame > BULK_ACCESS_WARNING_THRESHOLD) {
				warned = true
				log.warn {
					"MetaEntity transform accessors were used $facadeAccessesThisFrame times in one frame. That " +
						"is a bulk loop over the facade, which measured ~4x slower than a plain object layout. " +
						"Read MetaEntityWorld.store's columns in systems and keep the entity accessors for " +
						"individual entities. Silence with -Dmeta.entities.strict=false."
				}
			}
			facadeAccessesThisFrame = 0
		}

		/** Lets a test assert the warning fires, and lets a game re-arm it after fixing a hot spot. */
		@JvmStatic
		fun resetAccounting() {
			facadeAccessesThisFrame = 0
			warned = false
		}

		/** Above this many facade accesses in one frame, it is a loop rather than gameplay code. */
		const val BULK_ACCESS_WARNING_THRESHOLD: Int = 2_000
	}
}
