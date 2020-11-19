package de.fatox.meta

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.PosModifier
import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.api.ui.WindowConfig
import de.fatox.meta.api.ui.register
import de.fatox.meta.assets.MetaData
import de.fatox.meta.assets.get
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.modules.MetaEditorModule
import de.fatox.meta.modules.MetaUIModule
import de.fatox.meta.screens.MetaEditorScreen
import de.fatox.meta.screens.SplashScreen
import de.fatox.meta.ui.dialogs.*
import de.fatox.meta.ui.windows.*

class EditorMeta(posM: PosModifier) : Meta(posM) {

	private val metaData: MetaData by lazyInject()
	private val assetProvider: AssetProvider by lazyInject()

	override fun config() {
		uiManager.posModifier = this.modifier
		val array = Array<FileHandle>()
		array.add(Gdx.files.internal("data/"))
		MetaInject.global {
			singleton<Screen> {
				SplashScreen {
					assetProvider.loadRawAssetsFromFolder(Gdx.files.internal("."))
					array.forEach { assetProvider.loadPackedAssetsFromFolder(it) }
					val audioVideoData: MetaAudioVideoData = metaData["audioVideoData"]
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
		register("X_Window") { AssetDiscovererWindow }
		register { ShaderComposerWindow }
		register { PrimitivesWindow }
		register { SceneOptionsWindow }
		register { CameraWindow }
		register { ShaderCompositionWizard }
		register { ShaderWizardDialog }
		register("B_Dialog") { ProjectWizardDialog }
		register { OpenProjectDialog() }
		register { SceneWizardDialog }
		register { MetaKeyRebindDialog() }
		register {ShaderLibraryWindow}
	}

	override fun MetaInject.injection() {
		MetaEditorModule()
		MetaUIModule
	}

	override fun ScreenConfig.screens() {
		register { MetaEditorScreen() }
	}

}
