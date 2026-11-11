package de.fatox.meta.shader

import com.badlogic.gdx.files.FileHandle
import de.fatox.meta.api.entity.EntityManager
import de.fatox.meta.api.model.MetaSceneData
import de.fatox.meta.entity.Meta3DEntity
import de.fatox.meta.entity.MetaEntityManager

class MetaSceneHandle(var data: MetaSceneData, var shaderComposition: ShaderComposition?, val sceneFile: FileHandle) {
	var entityManager: EntityManager<Meta3DEntity> = MetaEntityManager()

}
