package de.fatox.meta.shader

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import de.fatox.meta.api.graphics.GLShaderHandle
import de.fatox.meta.api.graphics.MetaGLShader

/** Runs a composition pass over the fullscreen quad. */
class MetaFullscreenShader(shaderHandle: GLShaderHandle) : MetaGLShader(shaderHandle) {
	override fun init() {
	}

	override fun render(renderable: Renderable) {
		renderable.meshPart.render(shaderProgram)
	}

	override fun begin(camera: Camera, context: RenderContext) {
		shaderProgram.bind()

		UniformAssignments.assignCustomUniforms(shaderProgram, camera, context)
	}
}
