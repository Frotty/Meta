package de.fatox.meta.entity

import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.entity.EntityManager

class MetaEntityManager : EntityManager<Meta3DEntity> {
	override val entities: Array<Meta3DEntity> = Array<Meta3DEntity>()
	override val staticEntities: Array<Meta3DEntity> = Array<Meta3DEntity>()

	override fun addEntity(entity: Meta3DEntity) {
		entities.add(entity)
	}

	override fun addStaticEntity(entity: Meta3DEntity) {
		staticEntities.add(entity)
	}

	override fun removeEntity(entity: Meta3DEntity) {
		staticEntities.removeValue(entity, true)
		entities.removeValue(entity, true)
	}

	override fun clear() {
		entities.clear()
		staticEntities.clear()
	}
}
