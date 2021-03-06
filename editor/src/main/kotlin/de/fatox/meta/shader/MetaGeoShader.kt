package de.fatox.meta.shader

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.math.Vector3
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.graphics.GLShaderHandle
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

/**
 * Created by Frotty on 29.06.2016.
 *
 * Geometry Shader (when chosen geometry shaderpass)
 */
class MetaGeoShader(shaderHandle: GLShaderHandle) : de.fatox.meta.api.graphics.MetaGLShader(shaderHandle) {
	private var camera: Camera? = null
	private var context: RenderContext? = null
	private var u_projTrans: Int = -1
	private var u_worldTrans: Int = -1
	private var u_normalTrans: Int = -1
	private var u_mvpTrans: Int = -1
	private var u_diffuseColor: Int = -1
	private var s_diffuseTex: Int = -1
	private var s_normalTex: Int = -1
	private var u_camPos: Int = -1

	private val assetProvider: AssetProvider by lazyInject()

	override fun init() {
		u_projTrans = shaderProgram.getUniformLocation("u_projViewTrans")
		u_worldTrans = shaderProgram.getUniformLocation("u_worldTrans")
		u_normalTrans = shaderProgram.getUniformLocation("u_normalTrans")
		u_mvpTrans = shaderProgram.getUniformLocation("u_mvpTrans")
		u_diffuseColor = shaderProgram.getUniformLocation("u_diffuseColor")
		s_diffuseTex = shaderProgram.getUniformLocation("s_diffuseTex")
		s_normalTex = shaderProgram.getUniformLocation("s_normalTex")

		val pixmap = Pixmap(1, 1, Pixmap.Format.RGB888)
		pixmap.drawPixel(0, 0, Color.WHITE.toIntBits())
		whiteTex = Texture(pixmap)
		emptyNormals = assetProvider.getResource("models/empty_n.png", Texture::class.java)
	}

	override fun begin(camera: Camera, context: RenderContext) {
		this.camera = camera
		this.context = context
		shaderProgram.bind()

		UniformAssignments.assignCameraUniforms(shaderProgram, camera)

		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
	}

	override fun render(renderable: Renderable) {
		UniformAssignments.assignRenderableUniforms(shaderProgram, this.camera!!, renderable)

		// Bind Textures
		// Diffuse-
		if (renderable.material.has(TextureAttribute.Diffuse)) {
			val diffuseTex = renderable.material.get(TextureAttribute.Diffuse) as TextureAttribute
			shaderProgram.setUniformi(s_diffuseTex, context!!.textureBinder.bind(diffuseTex.textureDescription.texture))
		} else {
			shaderProgram.setUniformi(s_diffuseTex, context!!.textureBinder.bind(whiteTex))
		}
		// Normal Map (for different lighting on a plane)
		if (renderable.material.has(TextureAttribute.Normal)) {
			val normalTex = renderable.material.get(TextureAttribute.Normal) as TextureAttribute
			shaderProgram.setUniformi(s_normalTex, context!!.textureBinder.bind(normalTex.textureDescription.texture))
		} else {
			shaderProgram.setUniformi(s_normalTex, context!!.textureBinder.bind(emptyNormals))
		}

		if (renderable.material.has(ColorAttribute.Diffuse)) {
			val col = renderable.material.get(ColorAttribute.Diffuse) as ColorAttribute
			shaderProgram.setUniformf(u_diffuseColor, col.color.r, col.color.g, col.color.b)
		} else {
			tempV.set(1f, 1f, 1f)
			shaderProgram.setUniformf(u_diffuseColor, tempV)
		}

		renderable.meshPart.render(shaderProgram)
	}

	companion object {
		private val tempV = Vector3()
		private var whiteTex: Texture? = null
		private var emptyNormals: Texture? = null
	}
}
