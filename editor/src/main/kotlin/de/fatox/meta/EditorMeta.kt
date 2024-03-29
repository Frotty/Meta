package de.fatox.meta

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.*
import de.fatox.meta.api.ui.WindowConfig
import de.fatox.meta.api.ui.register
import de.fatox.meta.api.ui.registerSingleton
import de.fatox.meta.assets.MetaData
import de.fatox.meta.assets.get
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.modules.MetaEditorModule
import de.fatox.meta.modules.MetaUIModule
import de.fatox.meta.screens.MetaEditorScreen
import de.fatox.meta.ui.dialogs.*
import de.fatox.meta.ui.windows.*

class EditorMeta(
	windowHandler: WindowHandler,
	monitorHandler: MonitorHandler,
	soundHandler: SoundHandler,
	graphicsHandler: GraphicsHandler
) : Meta(windowHandler, monitorHandler, soundHandler, graphicsHandler) {

	private val metaData: MetaData by lazyInject()
	private val assetProvider: AssetProvider by lazyInject()

	override fun config() {
		uiManager.windowHandler = this.windowHandler
		val array = Array<FileHandle>()
		array.add(Gdx.files.internal("data/"))
		MetaInject.global {
			singleton<Screen> {
				SplashScreen {
					assetProvider.loadRawAssetsFromFolder(Gdx.files.internal("."))
					array.forEach { assetProvider.loadPackedAssetsFromFolder(it) }
					val audioVideoData = metaData[audioVideoDataKey]
					Gdx.app.postRunnable {
						uiManager.moveWindow(audioVideoData.x, audioVideoData.y)
						audioVideoData.apply()
						changeScreen(MetaEditorScreen())
					}
				}
			}
		}
	}

	override fun WindowConfig.windows() {
		registerSingleton("X_Window") { AssetDiscovererWindow() }
		registerSingleton { ShaderComposerWindow() }
		registerSingleton { PrimitivesWindow() }
		registerSingleton { SceneOptionsWindow() }
		registerSingleton { CameraWindow() }
		registerSingleton { ShaderCompositionWizard() }
		registerSingleton { ShaderWizardDialog() }
		registerSingleton("B_Dialog") { ProjectWizardDialog() }
		register { OpenProjectDialog() }
		registerSingleton { SceneWizardDialog() }
		register { MetaKeyRebindDialog() }
		registerSingleton { ShaderLibraryWindow() }
	}

	override fun MetaInject.injection() {
		MetaEditorModule()
		MetaUIModule
	}

	override fun ScreenConfig.screens() {
		register { MetaEditorScreen() }
	}

}
