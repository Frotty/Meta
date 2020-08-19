package de.fatox.meta.graphics.renderer

class BufferRenderer {
//	private val batch: SpriteBatch by lazyInject()
//	private val shaderLibrary: ShaderLibrary by lazyInject()
//	private val cam: PerspectiveCamera by lazyInject()
//	private val entityManager: EntityManager<Meta3DEntity> by lazyInject()
//
//	private val modelCache: ModelCache = ModelCache()
//	private var modelBatch: ModelBatch? = null
//	private val fsquad = FullscreenQuad(1)
//	private var mrtFrameBuffer: MRTFrameBuffer? = null
//	private var lightingBuffer: FrameBuffer? = null
//	private var shadowBuffer: FrameBuffer? = null
//	private var renderContext: RenderContext? = null
//	private val lights: Array<LightEntity> = Array()
//	private var compositeQuad: FullscreenQuad? = null
//	fun rebuildCache() {
//		modelCache.begin()
//		for (entity in entityManager.getEntities()) {
//			modelCache.add(entity.getActor())
//		}
//		modelCache.end()
//	}
//
//	fun render(x: Float, y: Float) {
//		ShaderProgram.pedantic = false
//		mrtFrameBuffer.begin()
//		renderContext.begin()
//		Gdx.gl30.glClear(GL30.GL_COLOR_BUFFER_BIT or GL30.GL_DEPTH_BUFFER_BIT)
//		val shaderInfo: Shader = shaderLibrary.getActiveShaders().get(0)
//		modelBatch.begin(cam)
//		modelBatch.render(modelCache, shaderInfo)
//		modelBatch.end()
//		mrtFrameBuffer.end()
//		renderContext.end()
//		renderContext.begin()
//		val shaderInfo2: Shader = shaderLibrary.getActiveShaders().get(1)
//		val shader: LightShader = shaderInfo2 as LightShader
//		shader.begin(cam, renderContext)
//		shader.getProgram().setUniformf("u_inverseScreenSize", 1.0f / mrtFrameBuffer.getWidth(), 1.0f / mrtFrameBuffer.getHeight())
//		val depthBind: Int = renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(3))
//		shader.getProgram().setUniformi("s_depth", depthBind)
//		val normalBind: Int = renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(1))
//		shader.getProgram().setUniformi("s_normal", normalBind)
//		for (le in lights) {
//			shader.getProgram().setUniformf("u_lightColor", le.color)
//			shader.render(le.volumeSphere.getRenderable(Renderable()))
//		}
//		shader.end()
//		renderContext.end()
//		renderContext.begin()
//		lightingBuffer.begin()
//		Gdx.gl.glClearColor(0f, 0f, 0f, 1)
//		Gdx.gl30.glClear(GL30.GL_COLOR_BUFFER_BIT or GL30.GL_DEPTH_BUFFER_BIT)
//		val mrtSceneShader = shaderLibrary.getOutputShader().getShader() as FullscreenShader
//		mrtSceneShader.begin(cam, renderContext)
//		val albedoBind: Int = renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(0))
//		mrtSceneShader.program!!.setUniformi("s_albedoTex", albedoBind)
//		mrtSceneShader.program!!.setUniformi("s_normalTex", normalBind)
//		mrtSceneShader.program!!.setUniformi("s_depthTex", depthBind)
//		//        mrtSceneShader.getProgram().setUniformi("s_lightTex", renderContext.textureBinder.bind(lightingBuffer.getColorBufferTexture()));
//		fsquad.render(mrtSceneShader.program)
//		mrtSceneShader.end()
//		lightingBuffer.end()
//		val blurShader = shaderLibrary.getActiveShaders().get(2) as FullscreenShader
//		blurShader.begin(cam, renderContext)
//		blurShader.program!!.setUniformi("s_albedoTex", albedoBind)
//		blurShader.program!!.setUniformi("s_inputTex", renderContext.textureBinder.bind(lightingBuffer.getColorBufferTexture()))
//		compositeQuad!!.render(blurShader.program)
//		blurShader.end()
//		renderContext.end()
//		debugAll(x, y)
//	}
//
//	private fun debugAll(x: Float, y: Float) {
//		batch.disableBlending()
//		batch.begin()
//		val debugScreens = 4f
//		val width: Float = mrtFrameBuffer.getWidth()
//		val height: Float = mrtFrameBuffer.getHeight()
//		batch.draw(mrtFrameBuffer.getColorBufferTexture(0), x, y, width / debugScreens, height / debugScreens, 0f, 0f, 1f, 1f)
//		batch.draw(mrtFrameBuffer.getColorBufferTexture(1), x + width / debugScreens, y, width / debugScreens, height / debugScreens, 0f, 0f, 1f, 1f)
//		batch.draw(mrtFrameBuffer.getColorBufferTexture(3), x + 2 * width / debugScreens, y, width / debugScreens, height / debugScreens, 0f, 0f, 1f, 1f)
//		batch.draw(lightingBuffer.getColorBufferTexture(), x + 3 * width / debugScreens, y, width / debugScreens, height / debugScreens, 0f, 0f, 1f, 1f)
//		batch.end()
//	}
//
//	fun rebuild(width: Int, height: Int) {
//		if (mrtFrameBuffer != null) {
//			if (width == mrtFrameBuffer.getWidth() && height == mrtFrameBuffer.getHeight()) {
//				return
//			}
//			mrtFrameBuffer.dispose()
//			lightingBuffer.dispose()
//			shadowBuffer.dispose()
//			compositeQuad!!.dispose()
//			cam.viewportWidth = width
//			cam.viewportHeight = height
//			cam.update()
//		}
//		mrtFrameBuffer = MRTFrameBuffer(width, height)
//		lightingBuffer = FrameBuffer(Pixmap.Format.RGB888, width, height, false)
//		shadowBuffer = FrameBuffer(Pixmap.Format.RGB888, width, height, true)
//		renderContext = RenderContext(DefaultTextureBinder(DefaultTextureBinder.ROUNDROBIN, 5))
//		modelBatch = ModelBatch(renderContext)
//		compositeQuad = FullscreenQuad(1f - (1f - height.toFloat() / Gdx.graphics.getHeight() as Float) * 2f)
//		batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
//	}
//
//	init {
//		cam.update()
//		rebuild(Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
//		for (i in -1..0) {
//			for (j in -1..0) {
//				lights.add(LightEntity(Vector3(i * 15, 5, j * 15), 10, Vector3(MathUtils.random(0.1f, 0.9f), MathUtils.random(0.1f, 0.9f),
//					MathUtils.random(0.1f, 0.9f))))
//			}
//		}
//	}
}