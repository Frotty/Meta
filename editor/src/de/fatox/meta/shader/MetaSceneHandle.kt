package de.fatox.meta.shader

import com.badlogic.gdx.files.FileHandle
import de.fatox.meta.Primitives
import de.fatox.meta.api.entity.EntityManager
import de.fatox.meta.api.model.MetaSceneData
import de.fatox.meta.entity.Meta3DEntity
import de.fatox.meta.entity.MetaEntityManager
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

/**
 * Created by Frotty on 18.04.2017.
 */
class MetaSceneHandle(var data: MetaSceneData, var shaderComposition: ShaderComposition?, val sceneFile: FileHandle) {
    var entityManager: EntityManager<Meta3DEntity> = MetaEntityManager()

    private val primitives: Primitives by lazyInject()
    private val projectManager: ProjectManager by lazyInject()
}
