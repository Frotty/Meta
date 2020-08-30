package de.fatox.meta.entity

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.GdxRuntimeException
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug

private val log = MetaLoggerFactory.logger {}

/**
 * Final composite Shader that turns the gbuffers into a composite texture,
 * which is then rendered to a fullscreen Quad.
 */
class LightShader : Shader {
	var program: ShaderProgram? = null
		private set
	private var u_InverseScreenSize = 0
	private var u_WorldTrans = 0
	private var u_ProjTrans = 0
	private var u_ProjViewTrans = 0
	private var u_ViewTrans = 0
	private var u_MvpTrans = 0
	private var u_FarDistance = 0
	private var u_LightPosition = 0
	private var u_NormalTrans = 0
	private var u_CamPos = 0

	//Light
	private var u_LightRadius = 0
	private var u_LightColor = 0
	private var u_invProjTrans = 0
	private var u_MvTrans = 0
	override fun init() {
		val vert = Gdx.files.internal("shaders/lightpoint.vert").readString()
		val frag = Gdx.files.internal("shaders/lightpoint.frag").readString()
		program = ShaderProgram(vert, frag)
		if (!program!!.isCompiled) {
			throw GdxRuntimeException(program!!.log)
		} else {
			log.debug { """
				Shader compiled correctly. Appending log:
				${program!!.log}
				""".trimIndent()
			}
		}
		u_InverseScreenSize = program!!.getUniformLocation("u_inverseScreenSize")
		u_WorldTrans = program!!.getUniformLocation("u_worldTrans")
		u_MvpTrans = program!!.getUniformLocation("u_mvpTrans")
		u_MvTrans = program!!.getUniformLocation("u_mvTrans")
		u_FarDistance = program!!.getUniformLocation("u_farDistance")
		u_LightPosition = program!!.getUniformLocation("u_lightPosition")
		u_ProjTrans = program!!.getUniformLocation("u_projTrans")
		u_ProjViewTrans = program!!.getUniformLocation("u_projViewTrans")
		u_invProjTrans = program!!.getUniformLocation("u_invProjTrans")
		u_ViewTrans = program!!.getUniformLocation("u_viewTrans")
		u_NormalTrans = program!!.getUniformLocation("u_normalTrans")
		u_LightRadius = program!!.getUniformLocation("u_lightRadius")
		u_LightColor = program!!.getUniformLocation("u_lightColor")
		u_CamPos = program!!.getUniformLocation("u_camPos")
	}

	override fun dispose() {
		program!!.dispose()
	}

	private val tmpM = Matrix3()
	val temp = Matrix4()
	var tempV = Vector3()
	private var camera: Camera? = null
	private var context: RenderContext? = null
	override fun render(renderable: Renderable) {
		context!!.setDepthMask(false)
		context!!.setDepthTest(GL20.GL_GEQUAL)
		context!!.setBlending(true, GL20.GL_ONE, GL20.GL_ONE)
		context!!.setCullFace(GL20.GL_FRONT)
		renderable.worldTransform.getTranslation(tempV)
		program!!.setUniformf(u_LightPosition, tempV)
		program!!.setUniformf(u_LightRadius, 50f)
		program!!.setUniformMatrix(u_WorldTrans, renderable.worldTransform)
		temp.set(camera!!.combined).mul(renderable.worldTransform)
		program!!.setUniformMatrix(u_MvpTrans, temp)
		temp.set(camera!!.view).mul(renderable.worldTransform)
		program!!.setUniformMatrix(u_MvTrans, temp)
		tmpM.set(renderable.worldTransform).inv().transpose()
		program!!.setUniformMatrix(u_NormalTrans, tmpM)
		renderable.meshPart.render(program)
	}

	override fun end() { // program.end is no longer necessary
	}

	override fun compareTo(other: Shader): Int {
		return 0
	}

	override fun canRender(renderable: Renderable): Boolean {
		return true
	}

	override fun begin(camera: Camera, context: RenderContext) {
		this.camera = camera
		this.context = context
		program!!.bind()
		program!!.setUniformMatrix(u_ProjTrans, camera.projection)
		program!!.setUniformMatrix(u_invProjTrans, camera.invProjectionView)
		program!!.setUniformMatrix(u_ViewTrans, camera.view)
		program!!.setUniformMatrix(u_ProjViewTrans, camera.combined)
		//        program.setUniformf("u_nearDistance", camera.near);
//        program.setUniformf("u_farDistance", camera.far);
		program!!.setUniformf(u_FarDistance, camera.far)
		program!!.setUniformf(u_CamPos, camera.position)
	}
}