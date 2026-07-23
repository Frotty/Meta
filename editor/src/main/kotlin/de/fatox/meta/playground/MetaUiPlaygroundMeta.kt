package de.fatox.meta.playground

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import de.fatox.meta.Meta
import de.fatox.meta.ScreenConfig
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.GraphicsHandler
import de.fatox.meta.api.MonitorHandler
import de.fatox.meta.api.SoundHandler
import de.fatox.meta.api.WindowHandler
import de.fatox.meta.api.ui.WindowConfig
import de.fatox.meta.api.ui.register
import de.fatox.meta.api.ui.registerSingleton
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.modules.MetaEditorModule
import de.fatox.meta.modules.MetaUIModule
import de.fatox.meta.register
import de.fatox.meta.ui.components.MetaColorPicker

class MetaUiPlaygroundMeta(
	windowHandler: WindowHandler,
	monitorHandler: MonitorHandler,
	soundHandler: SoundHandler,
	graphicsHandler: GraphicsHandler,
) : Meta(windowHandler, monitorHandler, soundHandler, graphicsHandler) {
	private val assetProvider: AssetProvider by lazyInject()

	override fun config() {
		uiManager.windowHandler = windowHandler
		MetaInject.global {
			singleton<Screen> {
				MetaUiPlaygroundScreen {
					assetProvider.loadRawAssetsFromFolder(Gdx.files.internal("."))
					val dataFolders = Array<FileHandle>()
					dataFolders.add(Gdx.files.internal("data/"))
					for (folder in dataFolders) assetProvider.loadPackedAssetsFromFolder(folder)
				}
			}
		}
	}

	override fun WindowConfig.windows() {
		register { PlaygroundSampleWindow() }
		registerSingleton { DockToolsPlaygroundWindow() }
		registerSingleton { DockLayersPlaygroundWindow() }
		registerSingleton { DockInspectorPlaygroundWindow() }
		registerSingleton { DockActivityPlaygroundWindow() }
		registerSingleton { TypographyPlaygroundWindow() }
		registerSingleton { ControlsPlaygroundWindow() }
		registerSingleton { SelectionPlaygroundWindow() }
		registerSingleton { ColorPickerPlaygroundWindow() }
		registerSingleton { CollectionsPlaygroundWindow() }
		registerSingleton { MetaColorPicker(isAllowAlphaEdit = true) }
	}

	@Suppress("UNUSED_EXPRESSION")
	override fun MetaInject.injection() {
		MetaEditorModule()
		MetaUIModule
	}

	override fun ScreenConfig.screens() {
		register { MetaUiPlaygroundScreen() }
	}
}
