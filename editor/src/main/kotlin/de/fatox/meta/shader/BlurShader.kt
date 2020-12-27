package de.fatox.meta.shader

import de.fatox.meta.graphics.renderer.FullscreenShader
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.utils.RenderContext

/**
 * Created by Frotty on 20.05.2016.
 */
class BlurShader : FullscreenShader() {
    override var program: ShaderProgram? = null
        private set
    private val s_inputTex = 0
    private val temp = Matrix4()
    override fun init() {
        val vert = Gdx.files.internal("shaders/ssaoblur.vert").readString()
        val frag = Gdx.files.internal("shaders/ssaoblur.frag").readString()
        program = ShaderProgram(vert, frag)
        if (!program!!.isCompiled) throw GdxRuntimeException(program!!.log)
    }

    override fun compareTo(other: Shader): Int {
        return 0
    }

    override fun canRender(instance: Renderable): Boolean {
        return true
    }

    override fun begin(camera: Camera, context: RenderContext) {
        program!!.begin()
        program!!.setUniformf("u_resolution", camera.viewportWidth, camera.viewportHeight)
    }

    override fun end() {
        program!!.end()
    }

    override fun dispose() {
        program!!.dispose()
    }
}