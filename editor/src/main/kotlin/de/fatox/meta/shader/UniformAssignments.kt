package de.fatox.meta.shader

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.ObjectMap
import de.fatox.meta.api.extensions.forEachEntryReentrant

object UniformAssignments {
	val customAssignments = ObjectMap<String, (ShaderProgram, Camera, RenderContext, Renderable?) -> Unit>()

	fun assignCustomUniforms(program: ShaderProgram, cam: Camera, context: RenderContext, renderable: Renderable? = null) {
		customAssignments.forEachEntryReentrant { name, assignment ->
			if (program.hasUniform(name)) {
				assignment.invoke(program, cam, context, renderable)
			}
		}
	}

	fun ShaderProgram.assignCameraUniforms(cam: Camera) {
		if (hasUniform("u_camPos")) setUniformf("u_camPos", cam.position)
		if (hasUniform("u_projTrans")) setUniformMatrix("u_projTrans", cam.combined)
	}

	fun ShaderProgram.assignRenderableUniforms(cam: Camera, renderable: Renderable) {
		if (hasUniform("u_worldTrans")) setUniformMatrix("u_worldTrans", renderable.worldTransform)
		if (hasUniform("u_normalTrans")) {
			setUniformMatrix("u_normalTrans", tmpM3.set(renderable.worldTransform).inv().transpose())
		}
		if (hasUniform("u_mvpTrans")) {
			setUniformMatrix("u_mvpTrans", tempM4.set(cam.combined).mul(renderable.worldTransform))
		}
	}

	private val tmpM3 = Matrix3()
	private val tempM4 = Matrix4()
}
