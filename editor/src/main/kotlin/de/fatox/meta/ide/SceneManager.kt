package de.fatox.meta.ide

import com.badlogic.gdx.files.FileHandle
import de.fatox.meta.api.model.MetaSceneData
import de.fatox.meta.shader.MetaSceneHandle

/**
 * Created by Frotty on 15.06.2016.
 */
interface SceneManager {
	fun createNew(name: String): MetaSceneHandle
	fun loadScene(projectFile: FileHandle)
	fun saveScene(sceneData: MetaSceneData)
}