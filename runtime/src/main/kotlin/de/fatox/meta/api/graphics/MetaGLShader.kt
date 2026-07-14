package de.fatox.meta.api.graphics

import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.GdxRuntimeException

/**
 * Created by Frotty on 29.06.2016.
 */
abstract class MetaGLShader(val shaderHandle: GLShaderHandle) : Shader {
	//    private val metaErrorHandler = MetaErrorHandler()

	val shaderProgram: ShaderProgram = ShaderProgram(shaderHandle.vertexHandle, shaderHandle.fragmentHandle)

	init {
		if (!shaderProgram.isCompiled) {
			val log = shaderProgram.log
			shaderProgram.dispose()
			throw GdxRuntimeException("Shader compile failed for ${shaderHandle.data.name}: $log")
		}
	}

	override fun compareTo(other: Shader): Int {
		return 0
	}

	override fun canRender(instance: Renderable): Boolean {
		return true
	}

	override fun end() {
	}

	override fun dispose() {
		shaderProgram.dispose()
	}

	override fun toString(): String {
		return "MSH: " + shaderHandle.data.name
	}
}
