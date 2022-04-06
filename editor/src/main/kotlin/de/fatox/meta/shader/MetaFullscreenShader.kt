package de.fatox.meta.shader

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.Renderable
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

	private lateinit var camera: Camera

	override fun init() {
	}

	override fun render(renderable: Renderable) {
		renderable.meshPart.render(shaderProgram)
	}

	override fun begin(camera: Camera, context: RenderContext) {
		this.camera = camera
		shaderProgram.bind()

		UniformAssignments.assignCustomUniforms(shaderProgram, camera, context)
	}
}
