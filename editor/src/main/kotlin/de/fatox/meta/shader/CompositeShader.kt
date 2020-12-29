package de.fatox.meta.shader

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.GdxRuntimeException
import de.fatox.meta.graphics.renderer.FullscreenShader

/**
 * Created by Frotty on 20.05.2016.
 */
class CompositeShader : FullscreenShader() {
    private var program: ShaderProgram? = null
    private val s_albedoTex = 0
    private var s_depthTex = 0
    private var u_nearDistance = 0
    private var u_farDistance = 0
    override fun getProgram(): ShaderProgram? {
        return program
    }

    private val temp = Matrix4()
    override fun init() {
        val vert = Gdx.files.internal("shaders/composite.vert").readString()
        val frag = Gdx.files.internal("shaders/composite.frag").readString()
        program = ShaderProgram(vert, frag)
        if (!program!!.isCompiled) throw GdxRuntimeException(program!!.log)

        //        s_albedoTex = program.getUniformLocation("s_albedoTex");
        s_depthTex = program!!.getUniformLocation("s_depthTex")
        u_nearDistance = program!!.getUniformLocation("u_cameraNear")
        u_farDistance = program!!.getUniformLocation("u_cameraFar")
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