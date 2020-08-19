package de.fatox.meta.modules

import com.kotcrab.vis.ui.widget.file.FileChooser
import de.fatox.meta.Primitives
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.graphics.Renderer
import de.fatox.meta.assets.MetaAssetProvider
import de.fatox.meta.ide.AssetDiscoverer
import de.fatox.meta.ide.MetaProjectManager
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.MetaInject.Companion.global
import de.fatox.meta.shader.EditorSceneRenderer
import de.fatox.meta.shader.MetaShaderComposer
import de.fatox.meta.shader.MetaShaderLibrary

class MetaEditorModule {
	init {
		global {
			singleton<ProjectManager> { MetaProjectManager() }
			singleton<Renderer> { EditorSceneRenderer() }
			singleton { Primitives }
			singleton { AssetDiscoverer() }
			singleton("default") { MetaShaderComposer() }
			singleton("default") { MetaShaderLibrary() }
			singleton<AssetProvider>("default") { MetaAssetProvider() }

			singleton("open") { FileChooser(FileChooser.Mode.OPEN) }
			singleton("save") { FileChooser(FileChooser.Mode.SAVE) }

			singleton("visui\\uiskin.json", "visuiSkin")
		}
	}
}