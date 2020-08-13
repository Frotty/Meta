package de.fatox.meta.shader

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.ObjectMap

object UniformAssignments {
    val beginAssignments = ObjectMap<String, (program: ShaderProgram, cam: Camera) -> Unit>()
    val renderableAssignments = ObjectMap<String, (program: ShaderProgram, cam: Camera, renderable: Renderable) -> Unit>()
    val customAssignments = ObjectMap<String, (program: ShaderProgram, cam: Camera, context: RenderContext, renderable: Renderable?) -> Unit>()

    fun assignCustomUniforms(program: ShaderProgram, cam: Camera, context: RenderContext, renderable: Renderable?) {
        customAssignments.forEach {
            if (program.hasUniform(it.key)) {
                it.value.invoke(program, cam, context, renderable)
            }
        }
    }

    fun assignCameraUniforms(program: ShaderProgram, cam: Camera) {
        beginAssignments.forEach {
            if (program.hasUniform(it.key)) {
                it.value.invoke(program, cam)
            }
        }
    }


    fun assignRenderableUniforms(program: ShaderProgram, cam: Camera, renderable: Renderable) {
        renderableAssignments.forEach {
            if (program.hasUniform(it.key)) {
                it.value.invoke(program, cam, renderable)
            }
        }
    }

    init {
        beginAssignments.put("u_camPos") { program, cam -> program.setUniformf("u_camPos", cam.position) }
		beginAssignments.put("u_projTrans") { program, cam -> program.setUniformMatrix("u_projTrans", cam.combined) }

		renderableAssignments.put("u_worldTrans") { program, _, renderable -> program.setUniformMatrix("u_worldTrans", renderable.worldTransform) }
		renderableAssignments.put("u_normalTrans") { program, _, renderable ->
			tmpM3.set(renderable.worldTransform).inv().transpose()
			program.setUniformMatrix("u_normalTrans", tmpM3)
		}

		renderableAssignments.put("u_mvpTrans") { program, camera, renderable ->
			tempM4.set(camera.combined).mul(renderable.worldTransform)
			program.setUniformMatrix("u_mvpTrans", tempM4)
		}
	}

    private val tmpM3 = Matrix3()
    private val tempM4 = Matrix4()
}
