package de.fatox.meta.shader

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import de.fatox.meta.Primitives
import de.fatox.meta.api.extensions.use
import de.fatox.meta.api.graphics.RenderBufferHandle
import de.fatox.meta.api.graphics.Renderer
import de.fatox.meta.api.model.RenderBufferData
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.entity.Meta3DEntity
import de.fatox.meta.graphics.renderer.FullscreenQuad
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.components.MetaLabel

class EditorSceneRenderer : Renderer {
	private val batch: SpriteBatch by lazyInject()
	private val cam: PerspectiveCamera by lazyInject()
	private val primitives: Primitives by lazyInject()
	private val uiManager: UIManager by lazyInject()

	private val grid: Meta3DEntity = Meta3DEntity(Vector3.Zero, primitives.terrainGrid, 1f)

	var sceneHandle: MetaSceneHandle? = null
		set(value) {
			field = value
			rebuildCache()
		}

	private val staticModelCache = ModelCache()
	private var renderContext: RenderContext = RenderContext(DefaultTextureBinder(DefaultTextureBinder.LRU))

	private var modelBatch: ModelBatch = ModelBatch(renderContext)

	private val fsquad = FullscreenQuad(1f)
	private var lastComposition: ShaderComposition? = null
	private val noCompositionTable = Table().apply {
		add(MetaLabel("No composition selected", 20)).pad(128f).center()
	}

	override fun render(x: Float, y: Float) {
		val activeScene = sceneHandle ?: run {
			noCompositionTable.remove()
			return
		}
		val composition = activeScene.shaderComposition ?: run {
			if (noCompositionTable.parent == null) {
				val table = noCompositionTable
				uiManager.addTable(table, true, true)
			}
			return
		}
		noCompositionTable.remove()
		val bufferHandles = composition.bufferHandles

		for (bufferIndex in 0 until bufferHandles.size) {
			val bufferHandle = bufferHandles[bufferIndex]
			renderContext.begin()
			bufferHandle.begin()
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

			if (bufferHandle.data.inType === RenderBufferData.IN.GEOMETRY) {
				modelBatch.begin(cam)
				modelBatch.render(staticModelCache, bufferHandle.metaShader)
				modelBatch.end()
			} else {
				bufferHandle.metaShader.begin(cam, renderContext)
				fsquad.render(bufferHandle.metaShader.shaderProgram)
				bufferHandle.metaShader.end()
			}

			bufferHandle.end()
			renderContext.end()
			for (textureIndex in 0 until bufferHandle.colorTextures.size) {
				val texture = bufferHandle.colorTextures[textureIndex]
				val name = "s_pass${bufferIndex}_$textureIndex"
				UniformAssignments.customAssignments.put(name) { prog, _, context, _ ->
					prog.setUniformi(name, context.textureBinder.bind(texture))
				}
			}
		}

		renderContext.begin()
		Gdx.gl20.glViewport(0, 0, cam.viewportWidth.toInt(), cam.viewportHeight.toInt())
		val outputBuffer = composition.outputBuffer
		if (outputBuffer != null) {
			if (outputBuffer.data.inType === RenderBufferData.IN.GEOMETRY) {
				modelBatch.begin(cam)
				modelBatch.render(staticModelCache, outputBuffer.metaShader)
				modelBatch.end()
			} else {
				outputBuffer.metaShader.begin(cam, renderContext)
				fsquad.render(outputBuffer.metaShader.shaderProgram)
				outputBuffer.metaShader.end()
			}
		}

		renderContext.end()
		UniformAssignments.customAssignments.clear()

		HdpiUtils.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
		debugAll(x, y, bufferHandles)

	}

	private fun debugAll(x: Float, y: Float, bufferHandles: Array<RenderBufferHandle>) {
		batch.disableBlending()
		try {
			batch.use {
				var debugScreens = 1f
				for (bufferIndex in 0 until bufferHandles.size) {
					val textureCount = bufferHandles[bufferIndex].colorTextures.size
					debugScreens += if (textureCount == 0) 1 else textureCount
				}

				var count = 0

				for (bufferIndex in 0 until bufferHandles.size) {
					val bufferHandle = bufferHandles[bufferIndex]
					val height = bufferHandle.height
					val width = bufferHandle.width
					val colorTextures = bufferHandle.colorTextures
					for (textureIndex in 0 until colorTextures.size) {
						val texture = colorTextures[textureIndex]
						val fl = 0.75f
						batch.draw(texture, x + width / debugScreens * count.toFloat() * fl, y, width / debugScreens * fl, height / debugScreens * fl, 0f, 0f, 1f, 1f)
						count++
					}
				}
			}
		} finally {
			batch.enableBlending()
		}
	}

	override fun rebuild(width: Int, height: Int) {
		val currentComposition = sceneHandle?.shaderComposition
		if (currentComposition != null) {
			if (currentComposition === lastComposition) {
				resize(width, height)
			} else {
				lastComposition = currentComposition
				create(width, height)
			}
		}
	}

	private fun create(width: Int, height: Int) {
		renderContext.textureBinder.resetCounts()
		resize(width, height)
	}

	private fun resize(width: Int, height: Int) {
		if (width > 0 && height > 0) {
			resizeCam(width, height)

			val composition = sceneHandle?.shaderComposition ?: return
			for (index in 0 until composition.bufferHandles.size) {
				composition.bufferHandles[index].rebuild(width, height)
			}
		}
	}

	private fun resizeCam(width: Int, height: Int) {
		cam.viewportWidth = width.toFloat()
		cam.viewportHeight = height.toFloat()
		cam.update(true)
	}

	override fun rebuildCache() {
		staticModelCache.begin()
		val activeScene = sceneHandle
		if (activeScene != null) {
			if (activeScene.data.showGrid) {
				staticModelCache.add(grid.actorModel)
			}
			for (entity in activeScene.entityManager.staticEntities) {
				staticModelCache.add(entity.actorModel)
			}
		}
		staticModelCache.end()
	}

}
