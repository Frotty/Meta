package de.fatox.meta.api.entity

interface EntityManager<ENTITY : Entity<*>> {

	val entities: Iterable<ENTITY>

	val staticEntities: Iterable<ENTITY>

	fun addEntity(entity: ENTITY)

	fun addStaticEntity(entity: ENTITY)

	fun removeEntity(entity: ENTITY)

	fun clear()
}
