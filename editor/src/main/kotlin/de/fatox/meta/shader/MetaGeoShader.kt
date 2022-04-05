package de.fatox.meta.shader

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.Attributes
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.math.Vector3
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.get
import de.fatox.meta.api.graphics.GLShaderHandle
import de.fatox.meta.api.graphics.MetaGLShader
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.shader.UniformAssignments.assignCameraUniforms
import de.fatox.meta.shader.UniformAssignments.assignRenderableUniforms

/**
 * Created by Frotty on 29.06.2016.
 *
 * Geometry Shader (when chosen geometry shaderpass)
 */
class MetaGeoShader(shaderHandle: GLShaderHandle) : MetaGLShader(shaderHandle) {
	private lateinit var camera: Camera
	private lateinit var context: RenderContext
	private var u_projTrans: Int = -1
	private var u_worldTrans: Int = -1
	private var u_normalTrans: Int = -1
	private var u_mvpTrans: Int = -1
	private var u_diffuseColor: Int = -1
	private var s_diffuseTex: Int = -1
	private var s_normalTex: Int = -1

	private lateinit var whiteTex: Texture
	private lateinit var emptyNormals: Texture

	private val assetProvider: AssetProvider by lazyInject()

	override fun init() {
		u_projTrans = shaderProgram.getUniformLocation("u_projViewTrans")
		u_worldTrans = shaderProgram.getUniformLocation("u_worldTrans")
		u_normalTrans = shaderProgram.getUniformLocation("u_normalTrans")
		u_mvpTrans = shaderProgram.getUniformLocation("u_mvpTrans")
		u_diffuseColor = shaderProgram.getUniformLocation("u_diffuseColor")
		s_diffuseTex = shaderProgram.getUniformLocation("s_diffuseTex")
		s_normalTex = shaderProgram.getUniformLocation("s_normalTex")

		// FIXME pixmap needs to be disposed
		val pixmap = Pixmap(1, 1, Pixmap.Format.RGB888)
		pixmap.drawPixel(0, 0, Color.WHITE.toIntBits())
		whiteTex = Texture(pixmap)
		emptyNormals = assetProvider["models/empty_n.png"]
	}

	override fun begin(camera: Camera, context: RenderContext) {
		this.camera = camera
		this.context = context
		shaderProgram.bind()

		shaderProgram.assignCameraUniforms(camera)

		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
	}

	private fun Attributes.getTexture(type: Long): Texture? {
		return get(TextureAttribute::class.java, type)?.textureDescription?.texture
	}

	override fun render(renderable: Renderable) {
		shaderProgram.assignRenderableUniforms(camera, renderable)

		// Bind Textures
		// Diffuse-
		val diffuseTex = renderable.material.getTexture(TextureAttribute.Diffuse) ?: whiteTex
		shaderProgram.setUniformi(s_diffuseTex, context.textureBinder.bind(diffuseTex))
		// Normal Map (for different lighting on a plane)
		val normalTex = renderable.material.getTexture(TextureAttribute.Normal) ?: emptyNormals
		shaderProgram.setUniformi(s_normalTex, context.textureBinder.bind(normalTex))

		if (renderable.material.has(ColorAttribute.Diffuse)) {
			val col = renderable.material.get(ColorAttribute.Diffuse) as ColorAttribute
			shaderProgram.setUniformf(u_diffuseColor, col.color.r, col.color.g, col.color.b)
		} else {
			tempV.set(1f, 1f, 1f)
			shaderProgram.setUniformf(u_diffuseColor, tempV)
		}

		renderable.meshPart.render(shaderProgram)
	}

	override fun dispose() {
		super.dispose()
		whiteTex.dispose()
	}

	companion object {
		private val tempV = Vector3()
	}
}
