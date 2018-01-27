package de.fatox.meta.api.graphics

import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import de.fatox.meta.Meta
import de.fatox.meta.error.MetaError
import de.fatox.meta.error.MetaErrorHandler

/**
 * Created by Frotty on 29.06.2016.
 */
abstract class MetaGLShader(var shaderHandle: GLShaderHandle) : Shader {
    private val metaErrorHandler = MetaErrorHandler()

    var shaderProgram: ShaderProgram? = null

    init {
        Meta.inject(this)
    }

    override fun init() {
        shaderProgram = ShaderProgram(shaderHandle.vertexHandle, shaderHandle.fragmentHandle)
        if (!shaderProgram?.isCompiled!!) {
            metaErrorHandler.add(object : MetaError("Shader compilation failed", "") {
                override fun gotoError() {
                    // TODO
                }
            })
        }
    }

    override fun compareTo(other: Shader): Int {
        return 0
    }

    override fun canRender(instance: Renderable): Boolean {
        return shaderProgram != null
    }

    override fun end() {
        shaderProgram?.end()
    }

    override fun dispose() {
        shaderProgram?.dispose()
    }

    override fun toString(): String {
        return "MSH: " + shaderHandle.data.name
    }

    companion object {

        private val tempM3 = Matrix3()
        private val tempM4 = Matrix4()
        private val tempV3 = Vector3()
    }
}
