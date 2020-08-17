package de.fatox.meta.desktop

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import de.fatox.meta.api.graphics.GLShaderHandle
import de.fatox.meta.api.graphics.RenderBufferHandle
import de.fatox.meta.api.model.GLShaderData
import de.fatox.meta.api.model.RenderBufferData
import de.fatox.meta.shader.MetaGeoShader

object TestLauncher {

    @JvmStatic
    fun main(arg: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration()
        config.setResizable(true)
        config.setTitle("Tomskerino")
        config.setWindowPosition(960 - 980 / 2, 540 - 360 / 2)
        config.setWindowedMode(980, 360)
        config.useOpenGL3(true, 3, 2)
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4)
        Lwjgl3Application(TestApp(), config)
    }

}

class TestApp : ApplicationListener {

    private lateinit var mrtFrameBuffer: RenderBufferHandle

    override fun render() {
        mrtFrameBuffer.begin()
        mrtFrameBuffer.end(0f, 0f)
    }

    override fun pause() {
    }

    override fun resume() {
    }
    override fun resize(width: Int, height: Int) {
        mrtFrameBuffer.rebuild(width, height)
    }


        override fun create() {
            val metaGeoShader = MetaGeoShader(GLShaderHandle(Gdx.files.internal(""), Gdx.files.internal("shaders/ssao.vert"), Gdx.files.internal("shaders/ssao.frag"), GLShaderData()))
            val data = RenderBufferData()
        data.hasDepth = true
        mrtFrameBuffer = RenderBufferHandle(data, metaGeoShader)
    }

    override fun dispose() {
         //TODO To change body of created functions use File | Settings | File Templates.
    }
}