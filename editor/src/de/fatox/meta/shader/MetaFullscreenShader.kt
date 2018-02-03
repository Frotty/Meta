package de.fatox.meta.shader

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.math.Matrix4
import de.fatox.meta.api.graphics.GLShaderHandle
import de.fatox.meta.api.graphics.MetaGLShader

/**
 * Created by Frotty on 20.05.2016.
 *
 * Fullscreen Shader (for fullscreen option, renders to fsquad)
 */
class MetaFullscreenShader(shaderHandle: GLShaderHandle) : MetaGLShader(shaderHandle) {
    private val s_albedoTex: Int = 0
    private var s_depthTex: Int = 0
    private var u_nearDistance: Int = 0
    private var u_farDistance: Int = 0
    private val temp = Matrix4()

    override fun init() {
        //        s_albedoTex = program.getUniformLocation("s_albedoTex");
    }

    override fun render(renderable: Renderable?) {

    }

    override fun compareTo(other: Shader): Int {
        return 0
    }

    override fun canRender(instance: Renderable): Boolean {
        return true
    }

    override fun begin(camera: Camera, context: RenderContext) {
        shaderProgram.begin()
        shaderProgram.setUniformf(u_nearDistance, camera.near)
        shaderProgram.setUniformf(u_farDistance, camera.far)
//        shaderProgram?.setUniformMatrix("u_invProjTrans", camera.invProjectionView)
//        shaderProgram?.setUniformMatrix("u_projTrans", camera.projection)
    }


    override fun end() {
        shaderProgram.end()
    }

    override fun dispose() {
        shaderProgram.dispose()
    }
}
