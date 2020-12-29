package de.fatox.meta.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.FPSLogger
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import de.fatox.meta.Meta.Companion.inject
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.assets.MetaData
import de.fatox.meta.ide.SceneManager
import de.fatox.meta.injection.Inject
import de.fatox.meta.ui.MetaEditorUI

class MetaEditorScreen : ScreenAdapter() {
    @Inject
    private val uiManager: UIManager? = null

    @Inject
    private val uiRenderer: UIRenderer? = null

    @Inject
    private val spriteBatch: SpriteBatch? = null

    @Inject
    private val fontProvider: FontProvider? = null

    @Inject
    private val metaEditorUISetup: MetaEditorUI? = null

    @Inject
    private val metaData: MetaData? = null

    @Inject
    private val assetProvider: AssetProvider? = null

    @Inject
    private val sceneManager: SceneManager? = null
    private var isInited = false
    private val fpsLogger = FPSLogger()
    override fun show() {
        if (!isInited) {
            inject(this)
            uiManager!!.changeScreen(javaClass.name)
            setupEditorUi()
            isInited = true
        } else {
            uiManager!!.changeScreen(javaClass.name)
        }
    }

    override fun render(delta: Float) {
        uiRenderer!!.update()
        clearFrame()
        uiRenderer.draw()
        //        fpsLogger.log();
    }

    private fun clearFrame() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.16862746f, 0.16862746f, 0.16862746f, 1f)
        Gdx.gl.glClearDepthf(1.0f)
        Gdx.gl30.glClear(
            GL30.GL_COLOR_BUFFER_BIT or GL30.GL_DEPTH_BUFFER_BIT or
                    if (Gdx.graphics.bufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0
        )
    }

    private fun setupEditorUi() {
        metaEditorUISetup!!.setup()
    }

    override fun resize(width: Int, height: Int) {
        if (isInited && width > 120 && height > 0) {
            uiManager!!.resize(width, height)
            if (!Gdx.graphics.isFullscreen) {
                val audioVideoData = metaData!!.get("audioVideoData", MetaAudioVideoData::class.java)
                audioVideoData.width = width
                audioVideoData.height = height
                audioVideoData.x = uiManager.posModifier.getX()
                audioVideoData.y = uiManager.posModifier.getY()
                metaData.save("audioVideoData", audioVideoData)
            }
        }
    }
}