package de.fatox.meta

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.GraphicsHandler
import de.fatox.meta.api.MonitorHandler
import de.fatox.meta.api.SoundHandler
import de.fatox.meta.api.SplashScreen
import de.fatox.meta.api.WindowHandler
import de.fatox.meta.api.model.MetaAudioVideoState
import de.fatox.meta.api.ui.WindowConfig
import de.fatox.meta.api.ui.register
import de.fatox.meta.api.ui.registerSingleton
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.modules.MetaEditorModule
import de.fatox.meta.modules.MetaUIModule
import de.fatox.meta.screens.MetaEditorScreen
import de.fatox.meta.ui.dialogs.OpenProjectDialog
import de.fatox.meta.ui.dialogs.ProjectWizardDialog
import de.fatox.meta.ui.dialogs.SceneWizardDialog
import de.fatox.meta.ui.dialogs.ShaderCompositionWizard
import de.fatox.meta.ui.dialogs.ShaderWizardDialog
import de.fatox.meta.ui.windows.AssetDiscovererWindow
import de.fatox.meta.ui.windows.CameraWindow
import de.fatox.meta.ui.windows.MetaKeyRebindDialog
import de.fatox.meta.ui.windows.PrimitivesWindow
import de.fatox.meta.ui.windows.SceneOptionsWindow
import de.fatox.meta.ui.windows.ShaderComposerWindow
import de.fatox.meta.ui.windows.ShaderLibraryWindow

class EditorMeta(
	windowHandler: WindowHandler,
	monitorHandler: MonitorHandler,
	soundHandler: SoundHandler,
	graphicsHandler: GraphicsHandler
) : Meta(windowHandler, monitorHandler, soundHandler, graphicsHandler) {

	private val assetProvider: AssetProvider by lazyInject()

	override fun config() {
		uiManager.windowHandler = this.windowHandler
		val array = Array<FileHandle>()
		array.add(Gdx.files.internal("data/"))
		MetaInject.global {
			singleton<Screen> {
				SplashScreen(prepareAssets = {
					assetProvider.loadRawAssetsFromFolder(Gdx.files.internal("."))
					for (i in 0 until array.size) assetProvider.loadPackedAssetsFromFolder(array[i])
				}, queueAssets = {
				}, onLoaded = {
					Gdx.app.postRunnable {
						val audioVideoData = MetaAudioVideoState.current()
						uiManager.moveWindow(audioVideoData.x, audioVideoData.y)
						audioVideoData.apply()
						changeScreen(MetaEditorScreen())
					}
				})
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
		MetaUIModule.initialize()
	}

	override fun ScreenConfig.screens() {
		register { MetaEditorScreen() }
	}

}
