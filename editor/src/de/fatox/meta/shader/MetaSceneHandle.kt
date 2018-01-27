package de.fatox.meta.shader

import com.badlogic.gdx.files.FileHandle
import de.fatox.meta.Meta
import de.fatox.meta.Primitives
import de.fatox.meta.api.dao.MetaSceneData
import de.fatox.meta.api.entity.EntityManager
import de.fatox.meta.entity.Meta3DEntity
import de.fatox.meta.entity.MetaEntityManager
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.Inject

/**
 * Created by Frotty on 18.04.2017.
 */
class MetaSceneHandle(var data: MetaSceneData, var shaderComposition: ShaderComposition?, val sceneFile: FileHandle) {
    var entityManager: EntityManager<Meta3DEntity> = MetaEntityManager()
    @Inject
    private lateinit var primitives: Primitives
    @Inject
    private lateinit var projectManager: ProjectManager

    init {
        Meta.inject(this)
    }

}
