/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fatox.meta.graphics.renderer
//
//import com.badlogic.gdx.Application
//import com.badlogic.gdx.Gdx
//import com.badlogic.gdx.Input
//import com.badlogic.gdx.InputMultiplexer
//import com.badlogic.gdx.assets.AssetManager
//import com.badlogic.gdx.graphics.*
//import com.badlogic.gdx.graphics.Texture.TextureFilter
//import com.badlogic.gdx.graphics.g2d.SpriteBatch
//import com.badlogic.gdx.graphics.g3d.*
//import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
//import com.badlogic.gdx.graphics.g3d.utils.*
//import com.badlogic.gdx.graphics.glutils.GLOnlyTextureData
//import com.badlogic.gdx.graphics.glutils.ShaderProgram
//import com.badlogic.gdx.math.MathUtils
//import com.badlogic.gdx.math.Matrix3
//import com.badlogic.gdx.math.Vector3
//import com.badlogic.gdx.tests.utils.GdxTest
//import com.badlogic.gdx.utils.*
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import java.util.*
//
///** MRT test compliant with GLES 3.0, with per pixel lighting and normal and specular mapping.
// * Thanks to http://www.blendswap.com/blends/view/73922 for the cannon model, licensed under CC-BY-SA
// *
// * / ** @author Tomski  */
//class MultipleRenderTargetTest : GdxTest() {
//	var renderContext: RenderContext? = null
//	var frameBuffer: MRTFrameBuffer? = null
//	var camera: PerspectiveCamera? = null
//	var cameraController: FirstPersonCameraController? = null
//	var mrtSceneShader: ShaderProgram? = null
//	var batch: SpriteBatch? = null
//	var quad: Mesh? = null
//	var shaderProvider: ShaderProvider? = null
//	var renderableSorter: RenderableSorter? = null
//	var modelCache: ModelCache? = null
//	var floorInstance: ModelInstance? = null
//	var cannon: ModelInstance? = null
//	var lights: ExposedArray<Light> = ExposedArray<Light>()
//	var renderables: ExposedArray<Renderable> = ExposedArray<Renderable>()
//	var renerablePool = RenderablePool()
//	val NUM_LIGHTS = 10
//	fun create() {
//		//use default prepend shader code for batch, some gpu drivers are less forgiving
//		batch = SpriteBatch()
//		ShaderProgram.pedantic = false //depth texture not currently sampled
//		modelCache = ModelCache()
//		ShaderProgram.prependVertexCode = if (Gdx.app.type == Application.ApplicationType.Desktop) "#version 140\n #extension GL_ARB_explicit_attrib_location : enable\n" else "#version 300 es\n"
//		ShaderProgram.prependFragmentCode = if (Gdx.app.type == Application.ApplicationType.Desktop) "#version 140\n #extension GL_ARB_explicit_attrib_location : enable\n" else "#version 300 es\n"
//		renderContext = RenderContext(DefaultTextureBinder(DefaultTextureBinder.ROUNDROBIN))
//		shaderProvider = object : BaseShaderProvider() {
//			override fun createShader(renderable: Renderable): Shader {
//				return MRTShader(renderable)
//			}
//		}
//		renderableSorter = object : DefaultRenderableSorter() {
//			override fun compare(o1: Renderable, o2: Renderable): Int {
//				return o1.shader.compareTo(o2.shader)
//			}
//		}
//		mrtSceneShader = ShaderProgram(Gdx.files.internal("data/g3d/shaders/mrtscene.vert"),
//			Gdx.files.internal("data/g3d/shaders/mrtscene.frag"))
//		if (!mrtSceneShader!!.isCompiled) {
//			println(mrtSceneShader!!.log)
//		}
//		quad = createFullScreenQuad()
//		camera = PerspectiveCamera(67, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
//		camera!!.near = 1f
//		camera!!.far = 100f
//		camera!!.position[3f, 5f] = 10f
//		camera!!.lookAt(0f, 2f, 0f)
//		camera!!.up[0f, 1f] = 0f
//		camera!!.update()
//		cameraController = FirstPersonCameraController(camera)
//		cameraController!!.setVelocity(50f)
//		Gdx.input.inputProcessor = cameraController
//		frameBuffer = MRTFrameBuffer(Gdx.graphics.width, Gdx.graphics.height, 3)
//		val assetManager = AssetManager()
//		assetManager.load("data/g3d/materials/cannon.g3db", Model::class.java)
//		assetManager.finishLoading()
//		val scene = assetManager.get<Model>("data/g3d/materials/cannon.g3db")
//		cannon = ModelInstance(scene, "Cannon_LP")
//		cannon!!.transform.setToTranslationAndScaling(0f, 0f, 0f, 0.001f, 0.001f, 0.001f)
//		val modelBuilder = ModelBuilder()
//		for (i in 0 until NUM_LIGHTS) {
//			modelBuilder.begin()
//			val light = Light()
//			light.color[MathUtils.random(1f), MathUtils.random(1f)] = MathUtils.random(1f)
//			light.position[MathUtils.random(-10f, 10f), MathUtils.random(10f, 15f)] = MathUtils.random(-10f, 10f)
//			light.vy = MathUtils.random(10f, 20f)
//			light.vx = MathUtils.random(-10f, 10f)
//			light.vz = MathUtils.random(-10f, 10f)
//			val meshPartBuilder: MeshPartBuilder = modelBuilder.part("light", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position or VertexAttributes.Usage.ColorPacked or VertexAttributes.Usage.Normal.toLong().toInt(), Material())
//			meshPartBuilder.setColor(light.color.x, light.color.y, light.color.z, 1f)
//			meshPartBuilder.sphere(0.2f, 0.2f, 0.2f, 10, 10)
//			light.lightInstance = ModelInstance(modelBuilder.end())
//			lights.add(light)
//		}
//		modelBuilder.begin()
//		val meshPartBuilder: MeshPartBuilder = modelBuilder.part("floor", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position or VertexAttributes.Usage.ColorPacked or VertexAttributes.Usage.Normal.toLong().toInt(), Material())
//		meshPartBuilder.setColor(0.2f, 0.2f, 0.2f, 1f)
//		meshPartBuilder.box(0f, -0.1f, 0f, 20f, 0.1f, 20f)
//		floorInstance = ModelInstance(modelBuilder.end())
//		Gdx.input.inputProcessor = InputMultiplexer(this, cameraController)
//	}
//
//	fun keyDown(keycode: Int): Boolean {
//		if (keycode == Input.Keys.SPACE) {
//			for (light in lights) {
//				light.vy = MathUtils.random(10f, 20f)
//				light.vx = MathUtils.random(-10f, 10f)
//				light.vz = MathUtils.random(-10f, 10f)
//			}
//		}
//		return super.keyDown(keycode)
//	}
//
//	var track = 0f
//	fun render() {
//		track += Gdx.graphics.deltaTime
//		Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
//		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
//		cameraController!!.update(Gdx.graphics.deltaTime)
//		renderContext!!.begin()
//		frameBuffer!!.begin()
//		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
//		renerablePool.flush()
//		renderables.clear()
//		modelCache!!.begin(camera)
//		modelCache!!.add(cannon)
//		modelCache!!.add(floorInstance)
//		for (light in lights) {
//			light.update(Gdx.graphics.deltaTime)
//			modelCache!!.add(light.lightInstance)
//		}
//		modelCache!!.end()
//		modelCache!!.getRenderables(renderables, renerablePool)
//		for (renderable in renderables) {
//			renderable.shader = shaderProvider!!.getShader(renderable)
//		}
//		renderableSorter!!.sort(camera, renderables)
//		var currentShader: Shader? = null
//		for (i in 0 until renderables.size) {
//			val renderable: Renderable = renderables.get(i)
//			if (currentShader !== renderable.shader) {
//				currentShader?.end()
//				currentShader = renderable.shader
//				currentShader.begin(camera, renderContext)
//			}
//			currentShader!!.render(renderable)
//		}
//		currentShader?.end()
//		frameBuffer!!.end()
//		mrtSceneShader!!.begin()
//		mrtSceneShader!!.setUniformi("u_diffuseTexture",
//			renderContext!!.textureBinder.bind(frameBuffer!!.getColorBufferTexture(DIFFUSE_ATTACHMENT)))
//		mrtSceneShader!!.setUniformi("u_normalTexture",
//			renderContext!!.textureBinder.bind(frameBuffer!!.getColorBufferTexture(NORMAL_ATTACHMENT)))
//		mrtSceneShader!!.setUniformi("u_positionTexture",
//			renderContext!!.textureBinder.bind(frameBuffer!!.getColorBufferTexture(POSITION_ATTACHMENT)))
//		mrtSceneShader!!.setUniformi("u_depthTexture", renderContext!!.textureBinder.bind(frameBuffer!!.getColorBufferTexture(DEPTH_ATTACHMENT)))
//		for (i in 0 until lights.size) {
//			val light: Light = lights.get(i)
//			mrtSceneShader!!.setUniformf("lights[$i].lightPosition", light.position)
//			mrtSceneShader!!.setUniformf("lights[$i].lightColor", light.color)
//		}
//		mrtSceneShader!!.setUniformf("u_viewPos", camera!!.position)
//		mrtSceneShader!!.setUniformMatrix("u_inverseProjectionMatrix", camera!!.invProjectionView)
//		quad!!.render(mrtSceneShader, GL30.GL_TRIANGLE_FAN)
//		mrtSceneShader!!.end()
//		renderContext!!.end()
//		batch!!.disableBlending()
//		batch!!.begin()
//		batch!!.draw(frameBuffer!!.getColorBufferTexture(DIFFUSE_ATTACHMENT), 0f, 0f, Gdx.graphics.width / 4f,
//			Gdx.graphics.height / 4f, 0f, 0f, 1f, 1f)
//		batch!!.draw(frameBuffer!!.getColorBufferTexture(NORMAL_ATTACHMENT), Gdx.graphics.width / 4f, 0f,
//			Gdx.graphics.width / 4f, Gdx.graphics.height / 4f, 0f, 0f, 1f, 1f)
//		batch!!.draw(frameBuffer!!.getColorBufferTexture(POSITION_ATTACHMENT), 2 * Gdx.graphics.width / 4f, 0f,
//			Gdx.graphics.width / 4f, Gdx.graphics.height / 4f, 0f, 0f, 1f, 1f)
//		batch!!.draw(frameBuffer!!.getColorBufferTexture(DEPTH_ATTACHMENT), 3 * Gdx.graphics.width / 4f, 0f,
//			Gdx.graphics.width / 4f, Gdx.graphics.height / 4f, 0f, 0f, 1f, 1f)
//		batch!!.end()
//	}
//
//	fun dispose() {
//		frameBuffer!!.dispose()
//		batch!!.dispose()
//		cannon!!.model.dispose()
//		floorInstance!!.model.dispose()
//		for (light in lights) {
//			light.lightInstance!!.model.dispose()
//		}
//		mrtSceneShader!!.dispose()
//		quad!!.dispose()
//	}
//
//	fun createFullScreenQuad(): Mesh {
//		val verts = FloatArray(20)
//		var i = 0
//		verts[i++] = (-1).toFloat()
//		verts[i++] = (-1).toFloat()
//		verts[i++] = 0
//		verts[i++] = 0f
//		verts[i++] = 0f
//		verts[i++] = 1f
//		verts[i++] = (-1).toFloat()
//		verts[i++] = 0
//		verts[i++] = 1f
//		verts[i++] = 0f
//		verts[i++] = 1f
//		verts[i++] = 1f
//		verts[i++] = 0
//		verts[i++] = 1f
//		verts[i++] = 1f
//		verts[i++] = (-1).toFloat()
//		verts[i++] = 1f
//		verts[i++] = 0
//		verts[i++] = 0f
//		verts[i++] = 1f
//		val mesh = Mesh(true, 4, 0,
//			VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
//			VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"))
//		mesh.setVertices(verts)
//		return mesh
//	}
//
//	class Light {
//		var position = Vector3()
//		var color = Vector3()
//		var lightInstance: ModelInstance? = null
//		var vy = 0f
//		var vx = 0f
//		var vz = 0f
//		fun update(deltaTime: Float) {
//			vy += -30f * deltaTime
//			position.y += vy * deltaTime
//			position.x += vx * deltaTime
//			position.z += vz * deltaTime
//			if (position.y < 0.1f) {
//				vy *= -0.70f
//				position.y = 0.1f
//			}
//			if (position.x < -5) {
//				vx = -vx
//				position.x = -5f
//			}
//			if (position.x > 5) {
//				vx = -vx
//				position.x = 5f
//			}
//			if (position.z < -5) {
//				vz = -vz
//				position.z = -5f
//			}
//			if (position.z > 5) {
//				vz = -vz
//				position.z = 5f
//			}
//			lightInstance!!.transform.setToTranslation(position)
//		}
//	}
//
//	internal class MRTShader(renderable: Renderable) : Shader {
//		var shaderProgram: ShaderProgram
//		var attributes: Long
//		var context: RenderContext? = null
//		var matrix3 = Matrix3()
//		override fun init() {}
//		override fun compareTo(other: Shader): Int {
//			//quick and dirty shader sort
//			if ((other as MRTShader).attributes == attributes) return 0
//			return if (other.attributes and TextureAttribute.Normal == 1L) -1 else 1
//		}
//
//		override fun canRender(instance: Renderable): Boolean {
//			return attributes == instance.material.mask
//		}
//
//		override fun begin(camera: Camera, context: RenderContext) {
//			this.context = context
//			shaderProgram.begin()
//			shaderProgram.setUniformMatrix("u_projViewTrans", camera.combined)
//			context.setDepthTest(GL20.GL_LEQUAL)
//			context.setCullFace(GL20.GL_BACK)
//		}
//
//		override fun render(renderable: Renderable) {
//			val material = renderable.material
//			val diffuseTexture = material[TextureAttribute.Diffuse] as TextureAttribute
//			val normalTexture = material[TextureAttribute.Normal] as TextureAttribute
//			val specTexture = material[TextureAttribute.Specular] as TextureAttribute
//			if (diffuseTexture != null) {
//				shaderProgram.setUniformi("u_diffuseTexture", context!!.textureBinder.bind(diffuseTexture.textureDescription.texture))
//			}
//			if (normalTexture != null) {
//				shaderProgram.setUniformi("u_normalTexture", context!!.textureBinder.bind(normalTexture.textureDescription.texture))
//			}
//			if (specTexture != null) {
//				shaderProgram.setUniformi("u_specularTexture", context!!.textureBinder.bind(specTexture.textureDescription.texture))
//			}
//			shaderProgram.setUniformMatrix("u_worldTrans", renderable.worldTransform)
//			shaderProgram.setUniformMatrix("u_normalMatrix", matrix3.set(renderable.worldTransform).inv().transpose())
//			renderable.meshPart.render(shaderProgram)
//		}
//
//		override fun end() {}
//		override fun dispose() {
//			shaderProgram.dispose()
//		}
//
//		companion object {
//			var tmpAttributes = Attributes()
//		}
//
//		init {
//			var prefix = ""
//			if (renderable.material.has(TextureAttribute.Normal)) {
//				prefix += "#define texturedFlag\n"
//			}
//			val vert = Gdx.files.internal("data/g3d/shaders/mrt.vert").readString()
//			val frag = Gdx.files.internal("data/g3d/shaders/mrt.frag").readString()
//			shaderProgram = ShaderProgram(prefix + vert, prefix + frag)
//			if (!shaderProgram.isCompiled) {
//				throw GdxRuntimeException(shaderProgram.log)
//			}
//			renderable.material.set(tmpAttributes)
//			attributes = tmpAttributes.mask
//		}
//	}
//
//	class MRTFrameBuffer(
//		/** width  */
//		private val width: Int,
//		/** height  */
//		private val height: Int,
//		numColorAttachments: Int
//	) : Disposable {
//		/** the color buffer texture  */
//		private var colorTextures: ExposedArray<Texture>? = null
//
//		/** the framebuffer handle  */
//		private var framebufferHandle = 0
//		private fun createColorTexture(
//			min: TextureFilter, mag: TextureFilter, internalformat: Int, format: Int,
//			type: Int
//		): Texture {
//			val data = GLOnlyTextureData(width, height, 0, internalformat, format, type)
//			val result = Texture(data)
//			result.setFilter(min, mag)
//			result.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
//			return result
//		}
//
//		private fun createDepthTexture(): Texture {
//			val data = GLOnlyTextureData(width, height, 0, GL30.GL_DEPTH_COMPONENT32F, GL30.GL_DEPTH_COMPONENT,
//				GL30.GL_FLOAT)
//			val result = Texture(data)
//			result.setFilter(TextureFilter.Nearest, TextureFilter.Nearest)
//			result.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
//			return result
//		}
//
//		private fun disposeColorTexture(colorTexture: Texture) {
//			colorTexture.dispose()
//		}
//
//		private fun build() {
//			val gl = Gdx.gl20
//
//			// iOS uses a different framebuffer handle! (not necessarily 0)
//			if (!defaultFramebufferHandleInitialized) {
//				defaultFramebufferHandleInitialized = true
//				if (Gdx.app.type == Application.ApplicationType.iOS) {
//					val intbuf = ByteBuffer.allocateDirect(16 * Integer.SIZE / 8).order(ByteOrder.nativeOrder())
//						.asIntBuffer()
//					gl.glGetIntegerv(GL20.GL_FRAMEBUFFER_BINDING, intbuf)
//					defaultFramebufferHandle = intbuf[0]
//				} else {
//					defaultFramebufferHandle = 0
//				}
//			}
//			colorTextures = ExposedArray<Texture>()
//			framebufferHandle = gl.glGenFramebuffer()
//			gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle)
//
//			//rgba
//			val diffuse = createColorTexture(TextureFilter.Nearest, TextureFilter.Nearest, GL30.GL_RGBA8,
//				GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE)
//			//rgb
//			val normal = createColorTexture(TextureFilter.Nearest, TextureFilter.Nearest, GL30.GL_RGB8,
//				GL30.GL_RGB, GL30.GL_UNSIGNED_BYTE)
//			//rgb
//			val position = createColorTexture(TextureFilter.Nearest, TextureFilter.Nearest, GL30.GL_RGB8,
//				GL30.GL_RGB, GL30.GL_UNSIGNED_BYTE)
//			val depth = createDepthTexture()
//			colorTextures.add(diffuse)
//			colorTextures.add(normal)
//			colorTextures.add(position)
//			colorTextures.add(depth)
//			gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_TEXTURE_2D,
//				diffuse.textureObjectHandle, 0)
//			gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL30.GL_TEXTURE_2D,
//				normal.textureObjectHandle, 0)
//			gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT2, GL30.GL_TEXTURE_2D,
//				position.textureObjectHandle, 0)
//			gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, GL20.GL_TEXTURE_2D,
//				depth.textureObjectHandle, 0)
//			val buffer = BufferUtils.newIntBuffer(3)
//			buffer.put(GL30.GL_COLOR_ATTACHMENT0)
//			buffer.put(GL30.GL_COLOR_ATTACHMENT1)
//			buffer.put(GL30.GL_COLOR_ATTACHMENT2)
//			buffer.position(0)
//			Gdx.gl30.glDrawBuffers(3, buffer)
//			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, 0)
//			gl.glBindTexture(GL20.GL_TEXTURE_2D, 0)
//			val result = gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER)
//			gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, defaultFramebufferHandle)
//			if (result != GL20.GL_FRAMEBUFFER_COMPLETE) {
//				for (colorTexture in colorTextures) disposeColorTexture(colorTexture)
//				gl.glDeleteFramebuffer(framebufferHandle)
//				check(result != GL20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT) { "frame buffer couldn't be constructed: incomplete attachment" }
//				check(result != GL20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS) { "frame buffer couldn't be constructed: incomplete dimensions" }
//				check(result != GL20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT) { "frame buffer couldn't be constructed: missing attachment" }
//				check(result != GL20.GL_FRAMEBUFFER_UNSUPPORTED) { "frame buffer couldn't be constructed: unsupported combination of formats" }
//				throw IllegalStateException("frame buffer couldn't be constructed: unknown error $result")
//			}
//		}
//
//		/** Releases all resources associated with the FrameBuffer.  */
//		override fun dispose() {
//			val gl = Gdx.gl20
//			for (textureAttachment in colorTextures) {
//				disposeColorTexture(textureAttachment)
//			}
//			gl.glDeleteFramebuffer(framebufferHandle)
//			if (buffers[Gdx.app] != null) buffers[Gdx.app].removeValue(this, true)
//		}
//
//		/** Makes the frame buffer current so everything gets drawn to it.  */
//		fun bind() {
//			Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle)
//		}
//
//		/** Binds the frame buffer and sets the viewport accordingly, so everything gets drawn to it.  */
//		fun begin() {
//			bind()
//			setFrameBufferViewport()
//		}
//
//		/** Sets viewport to the dimensions of framebuffer. Called by [.begin].  */
//		protected fun setFrameBufferViewport() {
//			Gdx.gl20.glViewport(0, 0, colorTextures.first().getWidth(), colorTextures.first().getHeight())
//		}
//		/** Unbinds the framebuffer and sets viewport sizes, all drawing will be performed to the normal framebuffer from here on.
//		 *
//		 * @param x the x-axis position of the viewport in pixels
//		 * @param y the y-asis position of the viewport in pixels
//		 * @param width the width of the viewport in pixels
//		 * @param height the height of the viewport in pixels
//		 */
//		/** Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on.  */
//		@JvmOverloads
//		fun end(x: Int = 0, y: Int = 0, width: Int = Gdx.graphics.width, height: Int = Gdx.graphics.height) {
//			unbind()
//			Gdx.gl20.glViewport(x, y, width, height)
//		}
//
//		fun getColorBufferTexture(index: Int): Texture {
//			return colorTextures.get(index)
//		}
//
//		/** @return the height of the framebuffer in pixels
//		 */
//		fun getHeight(): Int {
//			return colorTextures.first().getHeight()
//		}
//
//		/** @return the width of the framebuffer in pixels
//		 */
//		fun getWidth(): Int {
//			return colorTextures.first().getWidth()
//		}
//
//		/** @return the depth of the framebuffer in pixels (if applicable)
//		 */
//		val depth: Int
//			get() = colorTextures.first().getDepth()
//
//		companion object {
//			/** the frame buffers  */
//			private val buffers: Map<Application, ExposedArray<MRTFrameBuffer>> = HashMap<Application, ExposedArray<MRTFrameBuffer>>()
//
//			/** the default framebuffer handle, a.k.a screen.  */
//			private var defaultFramebufferHandle = 0
//
//			/** true if we have polled for the default handle already.  */
//			private var defaultFramebufferHandleInitialized = false
//
//			/** Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on.  */
//			fun unbind() {
//				Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, 0)
//			}
//
//			private fun addManagedFrameBuffer(app: Application, frameBuffer: MRTFrameBuffer) {
//				var managedResources: ExposedArray<MRTFrameBuffer?>? = buffers[app]
//				if (managedResources == null) managedResources = ExposedArray<MRTFrameBuffer>()
//				managedResources.add(frameBuffer)
//				buffers.put(app, managedResources)
//			}
//
//			fun getManagedStatus(builder: StringBuilder): StringBuilder {
//				builder.append("Managed buffers/app: { ")
//				for (app in buffers.keys) {
//					builder.append(buffers[app].size)
//					builder.append(" ")
//				}
//				builder.append("}")
//				return builder
//			}
//
//			val managedStatus: String
//				get() = getManagedStatus(StringBuilder()).toString()
//		}
//
//		init {
//			build()
//			addManagedFrameBuffer(Gdx.app, this)
//		}
//	}
//
//	protected class RenderablePool : Pool<Renderable>() {
//		protected var obtained: ExposedArray<Renderable> = ExposedArray<Renderable>()
//		override fun newObject(): Renderable {
//			return Renderable()
//		}
//
//		override fun obtain(): Renderable {
//			val renderable = super.obtain()
//			renderable.environment = null
//			renderable.material = null
//			renderable.meshPart["", null, 0, 0] = 0
//			renderable.shader = null
//			obtained.add(renderable)
//			return renderable
//		}
//
//		fun flush() {
//			super.freeAll(obtained)
//			obtained.clear()
//		}
//	}
//
//	companion object {
//		var DIFFUSE_ATTACHMENT = 0
//		var NORMAL_ATTACHMENT = 1
//		var POSITION_ATTACHMENT = 2
//		var DEPTH_ATTACHMENT = 3
//	}
//}