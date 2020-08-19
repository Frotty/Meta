package de.fatox.meta.shader

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.GdxRuntimeException
import de.fatox.meta.Meta.Companion.inject
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.camera.ArcCamControl
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

class GBufferShader : Shader {
	var program: ShaderProgram? = null
	var camera: Camera? = null
	var context: RenderContext? = null
	var u_projTrans = 0
	var u_worldTrans = 0
	private var u_normalTrans = 0
	private var u_mvpTrans = 0
	private var u_mat = 0
	private var u_mvTrans = 0
	private var s_diffuseTex = 0
	private var s_normalTex = 0
	private var u_diffuseColor = 0
	private var u_camPos = 0
	private var whiteTex: Texture? = null
	private var emptyNormals: Texture? = null

	private val assetProvider: AssetProvider by lazyInject()

	override fun init() {
		inject(this)
		val vert = Gdx.files.internal("shaders/gbuffer.vert").readString()
		val frag = Gdx.files.internal("shaders/gbuffer.frag").readString()
		program = ShaderProgram(vert, frag)
		if (!program!!.isCompiled) throw GdxRuntimeException(program!!.log)
		u_projTrans = program!!.getUniformLocation("u_projViewTrans")
		u_worldTrans = program!!.getUniformLocation("u_worldTrans")
		u_normalTrans = program!!.getUniformLocation("u_normalTrans")
		u_mvpTrans = program!!.getUniformLocation("u_mvpTrans")
		u_mat = program!!.getUniformLocation("u_mat")
		u_mvTrans = program!!.getUniformLocation("u_mvTrans")
		u_diffuseColor = program!!.getUniformLocation("u_diffuseColor")
		s_diffuseTex = program!!.getUniformLocation("s_diffuseTex")
		s_normalTex = program!!.getUniformLocation("s_normalTex")
		u_camPos = program!!.getUniformLocation("u_camPos")
		val pixmap = Pixmap(1, 1, Pixmap.Format.RGB888)
		pixmap.drawPixel(0, 0, Color.WHITE.toIntBits())
		whiteTex = Texture(pixmap)
		emptyNormals = assetProvider.getResource("models/empty_n.png", Texture::class.java)
	}

	override fun dispose() {
		program!!.dispose()
	}

	override fun begin(camera: Camera, context: RenderContext) {
		this.camera = camera
		this.context = context
		program!!.begin()
		program!!.setUniformMatrix(u_projTrans, camera.combined)
		program!!.setUniformMatrix(u_mvTrans, camera.view)
		program!!.setUniformf(u_camPos, camera.position)
		context.setDepthTest(GL20.GL_LEQUAL)
		context.setCullFace(GL20.GL_BACK)
	}

	private val tmpM = Matrix3()
	val temp = Matrix4()
	var tempV = Vector3()
	override fun render(renderable: Renderable) {
		program!!.setUniformMatrix(u_worldTrans, renderable.worldTransform)
		tmpM.set(renderable.worldTransform).inv().transpose()
		program!!.setUniformMatrix(u_normalTrans, tmpM)
		temp.set(camera!!.combined).mul(renderable.worldTransform)
		program!!.setUniformMatrix(u_mvpTrans, temp)
		tempV[.1f, 1f] = 0f
		program!!.setUniformf(u_mat, tempV)

		// Bind Textures
		// Diffuse-
		val diffuseTex = renderable.material[TextureAttribute.Diffuse] as TextureAttribute
		if (diffuseTex != null) {
			program!!.setUniformi(s_diffuseTex, context!!.textureBinder.bind(diffuseTex.textureDescription.texture))
		} else {
			program!!.setUniformi(s_diffuseTex, context!!.textureBinder.bind(whiteTex))
		}
		// Normal Map (for different lighting on a plane)
		val normalTex = renderable.material[TextureAttribute.Normal] as TextureAttribute
		if (normalTex != null) {
			if (ArcCamControl.yes) program!!.setUniformi(s_normalTex, context!!.textureBinder.bind(normalTex.textureDescription.texture)) else program!!.setUniformi(s_normalTex, context!!.textureBinder.bind(emptyNormals))
		} else {
			program!!.setUniformi(s_normalTex, context!!.textureBinder.bind(emptyNormals))
		}
		val col = renderable.material[ColorAttribute.Diffuse] as ColorAttribute
		if (col != null) {
			program!!.setUniformf(u_diffuseColor, col.color.r, col.color.g, col.color.b)
		} else {
			tempV[1f, 1f] = 1f
			program!!.setUniformf(u_diffuseColor, tempV)
		}
		renderable.meshPart.render(program)
	}

	override fun end() {
		program!!.end()
	}

	override fun compareTo(other: Shader): Int {
		return 0
	}

	override fun canRender(instance: Renderable): Boolean {
		return true
	}

	companion object {
		private val idtMatrix = Matrix4()
	}
}