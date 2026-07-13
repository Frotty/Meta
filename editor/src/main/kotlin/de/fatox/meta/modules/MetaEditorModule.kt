package de.fatox.meta.modules

import de.fatox.meta.Primitives
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.graphics.Renderer
import de.fatox.meta.assets.MetaAssetProvider
import de.fatox.meta.ide.*
import de.fatox.meta.injection.MetaInject.Companion.global
import de.fatox.meta.shader.EditorSceneRenderer
import de.fatox.meta.shader.MetaShaderComposer
import de.fatox.meta.shader.MetaShaderLibrary
import de.fatox.meta.ui.components.MetaFileChooser

class MetaEditorModule {
	init {
		global {
			singleton<SceneManager> { MetaSceneManager() }
			singleton<ProjectManager> { MetaProjectManager() }
			singleton<Renderer> { EditorSceneRenderer() }
			singleton { Primitives() }
			singleton { AssetDiscoverer() }
			singleton("default") { MetaShaderComposer() }
			singleton("default") { MetaShaderLibrary() }
			singleton<AssetProvider>("default") { MetaAssetProvider() }

			singleton("open") { MetaFileChooser(MetaFileChooser.OPEN) }
			singleton("save") { MetaFileChooser(MetaFileChooser.SAVE) }
		}
	}
}
