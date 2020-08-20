package de.fatox.meta.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.FPSLogger
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.g2d.SpriteBatch

import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.api.ui.register
import de.fatox.meta.assets.MetaData
import de.fatox.meta.ide.SceneManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.MetaEditorUI
import de.fatox.meta.ui.dialogs.*
import de.fatox.meta.ui.windows.*

class MetaEditorScreen : ScreenAdapter() {
	private val uiManager: UIManager by lazyInject()
	private val uiRenderer: UIRenderer by lazyInject()
	private val spriteBatch: SpriteBatch by lazyInject()
	private val fontProvider: FontProvider by lazyInject()
	private val metaEditorUISetup: MetaEditorUI by lazyInject()
	private val metaData: MetaData by lazyInject()
	private val assetProvider: AssetProvider by lazyInject()
	private val sceneManager: SceneManager by lazyInject()

	private var isInited = false
	private val fpsLogger = FPSLogger()
	override fun show() {
		if (!isInited) {
			uiManager.register("X_Window") { AssetDiscovererWindow }
			uiManager.register { ShaderComposerWindow }
			uiManager.register { PrimitivesWindow }
			uiManager.register { SceneOptionsWindow }
			uiManager.register { CameraWindow }
			uiManager.register { ShaderCompositionWizard }
			uiManager.register { ShaderWizardDialog }
			uiManager.register { ProjectWizardDialog }
			uiManager.register { OpenProjectDialog() }
			uiManager.register { SceneWizardDialog }
			uiManager.register { MetaKeyRebindDialog() }
			uiManager.changeScreen(javaClass.name)
			setupEditorUi()
			isInited = true
		} else {
			uiManager.changeScreen(javaClass.name)
		}
	}

	override fun render(delta: Float) {
		uiRenderer.update()
		clearFrame()
		uiRenderer.draw()
		//        fpsLogger.log();
	}

	private fun clearFrame() {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
		Gdx.gl.glClearColor(0.16862746f, 0.16862746f, 0.16862746f, 1f)
		Gdx.gl.glClearDepthf(1.0f)
		Gdx.gl30.glClear(GL30.GL_COLOR_BUFFER_BIT or GL30.GL_DEPTH_BUFFER_BIT or
			if (Gdx.graphics.bufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0)
	}

	private fun setupEditorUi() {
		metaEditorUISetup.setup()
	}

	override fun resize(width: Int, height: Int) {
		if (isInited && width > 120 && height > 0) {
			uiManager.resize(width, height)
			if (!Gdx.graphics.isFullscreen) {
				val audioVideoData = metaData.get("audioVideoData", MetaAudioVideoData::class.java)
				audioVideoData.width = width
				audioVideoData.height = height
				audioVideoData.x = uiManager.posModifier.x
				audioVideoData.y = uiManager.posModifier.y
				metaData.save("audioVideoData", audioVideoData)
			}
		}
	}
}