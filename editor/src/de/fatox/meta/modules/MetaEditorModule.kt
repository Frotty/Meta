package de.fatox.meta.modules

import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.kotcrab.vis.ui.widget.file.FileChooser
import de.fatox.meta.Primitives
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.graphics.Renderer
import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.assets.MetaAssetProvider
import de.fatox.meta.ide.*
import de.fatox.meta.injection.MetaInject.Companion.global
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.injection.Named
import de.fatox.meta.injection.Provides
import de.fatox.meta.injection.Singleton
import de.fatox.meta.lang.MetaLanguageBundle
import de.fatox.meta.screens.MetaEditorScreen
import de.fatox.meta.shader.EditorSceneRenderer
import de.fatox.meta.shader.MetaShaderComposer
import de.fatox.meta.shader.MetaShaderLibrary

class MetaEditorModule {
	init {
		global {
			singleton<Renderer> { EditorSceneRenderer() }
			singleton { Primitives() }
			singleton { AssetDiscoverer() }
			singleton("default") { MetaShaderComposer() }
			singleton("default") { MetaShaderLibrary() }
			singleton<AssetProvider>("default") { MetaAssetProvider() }

			singleton("open") { FileChooser(FileChooser.Mode.OPEN) }
			singleton("save") { FileChooser(FileChooser.Mode.SAVE) }

			singleton("visui\\uiskin.json", "visuiSkin")
		}
	}

	@Provides
	@Singleton
	fun renderer(): Renderer = inject()

	@Provides
	@Singleton
	fun primitives(): Primitives = inject()

	@Provides
	@Singleton
	fun projectManager(projectManager: MetaProjectManager): ProjectManager {
		return projectManager
	}

	@Provides
	@Singleton
	@Named("default")
	fun shaderComposer(): MetaShaderComposer = inject("default")

	@Provides
	@Singleton
	@Named("default")
	fun shaderLibrary(): MetaShaderLibrary = inject("default")

	@Provides
	@Singleton
	fun sceneManager(sceneManager: MetaSceneManager): SceneManager {
		return sceneManager
	}

	@Provides
	@Singleton
	fun assetManager(): AssetDiscoverer = inject()

	@Provides
	@Singleton
	@Named("default")
	fun firstScreen(editorScreen: MetaEditorScreen): Screen {
		return editorScreen
	}

	@Provides
	@Singleton
	@Named("default")
	fun languageBundle(metaLanguageBundle: MetaLanguageBundle): LanguageBundle {
		return metaLanguageBundle
	}

	@Provides
	@Singleton
	@Named("open")
	fun openFileChooser(): FileChooser = inject("open")

	@Provides
	@Singleton
	@Named("save")
	fun saveFileChooser(): FileChooser = inject("save")

	@Provides
	@Singleton
	@Named("visuiSkin")
	fun uiSkinPath(): String = inject("visuiSkin")

	@Provides
	@Singleton
	fun shapeRenderer(): ShapeRenderer? {
		return null
	}
}