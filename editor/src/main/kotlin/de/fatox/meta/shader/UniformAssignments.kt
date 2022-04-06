package de.fatox.meta.shader

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.ObjectMap

object UniformAssignments {
	val customAssignments = ObjectMap<String, (ShaderProgram, Camera, RenderContext, Renderable?) -> Unit>()

	fun assignCustomUniforms(program: ShaderProgram, cam: Camera, context: RenderContext, renderable: Renderable? = null) {
		customAssignments.forEach {
			if (program.hasUniform(it.key)) {
				it.value.invoke(program, cam, context, renderable)
			}
		}
	}

	fun ShaderProgram.assignCameraUniforms(cam: Camera) {
		setIfHasUniform("u_camPos", cam::position, ShaderProgram::setUniformf)
		setIfHasUniform("u_projTrans", cam::combined, ShaderProgram::setUniformMatrix)
	}

	fun ShaderProgram.assignRenderableUniforms(cam: Camera, renderable: Renderable) {
		setIfHasUniform("u_worldTrans", renderable::worldTransform, ShaderProgram::setUniformMatrix)
		setIfHasUniform(
			"u_normalTrans",
			{ tmpM3.set(renderable.worldTransform).inv().transpose() },
			ShaderProgram::setUniformMatrix
		)
		setIfHasUniform(
			"u_mvpTrans",
			{ tempM4.set(cam.combined).mul(renderable.worldTransform) },
			ShaderProgram::setUniformMatrix
		)
	}

	private inline fun <reified T: Any> ShaderProgram.setIfHasUniform(
		name: String,
		valueGetter: () -> T,
		setter: ShaderProgram.(String, T) -> Unit
	) {
		if (hasUniform(name)) setter(name, valueGetter())
	}

	private val tmpM3 = Matrix3()
	private val tempM4 = Matrix4()
}
