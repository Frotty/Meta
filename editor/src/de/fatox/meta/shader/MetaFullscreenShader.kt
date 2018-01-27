package de.fatox.meta.shader

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import de.fatox.meta.api.graphics.GLShaderHandle
import de.fatox.meta.api.graphics.MetaGLShader

/**
 * Created by Frotty on 20.05.2016.
 */
class MetaFullscreenShader(shaderHandle: GLShaderHandle) : MetaGLShader(shaderHandle) {
    private var program: ShaderProgram? = null

    private val s_albedoTex: Int = 0
    private var s_depthTex: Int = 0
    private var u_nearDistance: Int = 0
    private var u_farDistance: Int = 0
    private val temp = Matrix4()

    fun getProgram(): ShaderProgram? {
        return program
    }
    override fun init() {
        super.init()
        //        s_albedoTex = program.getUniformLocation("s_albedoTex");
        s_depthTex = program!!.getUniformLocation("s_depthTex")
        u_nearDistance = program!!.getUniformLocation("u_cameraNear")
        u_farDistance = program!!.getUniformLocation("u_cameraFar")

    }

    override fun render(renderable: Renderable?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun compareTo(other: Shader): Int {
        return 0
    }

    override fun canRender(instance: Renderable): Boolean {
        return true
    }

    override fun begin(camera: Camera, context: RenderContext) {
        program!!.begin()
        program!!.setUniformf(u_nearDistance, camera.near)
        program!!.setUniformf(u_farDistance, camera.far)
        program!!.setUniformMatrix("u_invProjTrans", camera.invProjectionView)
        program!!.setUniformMatrix("u_projTrans", camera.projection)
    }


    override fun end() {
        program!!.end()
    }

    override fun dispose() {
        program!!.dispose()
    }
}
