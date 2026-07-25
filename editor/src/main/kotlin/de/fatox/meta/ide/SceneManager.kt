package de.fatox.meta.ide

import com.badlogic.gdx.files.FileHandle
import de.fatox.meta.shader.MetaSceneHandle

interface SceneManager {
	fun createNew(name: String): MetaSceneHandle
	fun loadScene(projectFile: FileHandle)
	fun saveScene(sceneHandle: MetaSceneHandle)
}