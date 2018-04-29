package de.fatox.meta.shader

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import de.fatox.meta.Meta
import de.fatox.meta.Primitives
import de.fatox.meta.api.graphics.RenderBufferHandle
import de.fatox.meta.api.graphics.Renderer
import de.fatox.meta.api.model.RenderBufferData
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.entity.Meta3DEntity
import de.fatox.meta.graphics.renderer.FullscreenQuad
import de.fatox.meta.injection.Inject
import de.fatox.meta.ui.components.MetaLabel

/**
 * Created by Frotty on 17.04.2017.
 */
class EditorSceneRenderer : Renderer {
    @Inject
    private lateinit var batch: SpriteBatch
    @Inject
    private lateinit var shaderComposer: MetaShaderComposer
    @Inject
    private lateinit var cam: PerspectiveCamera
    @Inject
    private lateinit var primitives: Primitives
    @Inject
    private lateinit var uiManager: UIManager

    private val grid: Meta3DEntity

    var sceneHandle: MetaSceneHandle? = null
        set(value) {
            field = value
            rebuildCache()
        }

    private val staticModelCache = ModelCache()
    private var renderContext: RenderContext = RenderContext(DefaultTextureBinder(DefaultTextureBinder.WEIGHTED))

    private var modelBatch: ModelBatch = ModelBatch(renderContext)

    private val fsquad = FullscreenQuad(1f)
    private var lastComposition: ShaderComposition? = null

    init {
        Meta.inject(this)
        grid = Meta3DEntity(Vector3.Zero, primitives.terraingrid, 1f)
    }

    override fun render(x: Float, y: Float) {

        if (sceneHandle != null) {
            if (sceneHandle?.shaderComposition == null) {
                val table = Table()
                table.add(MetaLabel("No composition selected", 20)).pad(128f).center()
                uiManager.addTable(table, true, true)
            } else {
                val bufferHandles = sceneHandle?.shaderComposition?.bufferHandles

                for ((i, bufferHandle) in bufferHandles!!.withIndex()) {
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

                    bufferHandle.end(x, y)
                    renderContext.end()
                    var j = 0
                    bufferHandle.colorTextures.forEach({
                        val name = "s_pass${i}_$j"
                        UniformAssignments.customAssignments.put(name, { prog, cam, context, renderable ->
                            prog.setUniformi(name, context.textureBinder.bind(it))
                        })
                        j++
                    })

                }

                renderContext.begin()
                Gdx.gl20.glViewport(0, 0, cam.viewportWidth.toInt(), cam.viewportHeight.toInt())
                if (sceneHandle?.shaderComposition?.outputBuffer?.data?.inType === RenderBufferData.IN.GEOMETRY) {
                    modelBatch.begin(cam)
                    modelBatch.render(staticModelCache, sceneHandle?.shaderComposition?.outputBuffer?.metaShader)
                    modelBatch.end()
                } else {
                    sceneHandle?.shaderComposition?.outputBuffer?.metaShader?.begin(cam, renderContext)
                    fsquad.render(sceneHandle?.shaderComposition?.outputBuffer?.metaShader?.shaderProgram)
                    sceneHandle?.shaderComposition?.outputBuffer?.metaShader?.end()
                }

                renderContext.end()
                UniformAssignments.customAssignments.clear()

                Gdx.gl20.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
                debugAll(x, y, bufferHandles)
            }
        }

    }

    private fun debugAll(x: Float, y: Float, bufferHandles: Array<RenderBufferHandle>) {
        batch.disableBlending()
        batch.begin()
        var debugScreens = 1f
        bufferHandles.forEach({
            debugScreens += if (it.colorTextures.size == 0) 1 else it.colorTextures.size
        })

        var count = 0

        for (bufferHandle in bufferHandles) {
            val height = bufferHandle.height
            val width = bufferHandle.width
            val colorTextures = bufferHandle.colorTextures
            for (texture in colorTextures) {
                val fl = 0.75f
                batch.draw(texture, x + width / debugScreens * count.toFloat() * fl, y, width / debugScreens * fl, height / debugScreens * fl, 0f, 0f, 1f, 1f)
                count++
            }

        }
        batch.end()
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

            val composition = shaderComposer.currentComposition
            for (bufferHandle in composition?.bufferHandles!!) {
                bufferHandle.rebuild(width, height)
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
        if (sceneHandle?.data?.showGrid!!) {
            staticModelCache.add(grid.actorModel)
        }
        for (entity in sceneHandle?.entityManager?.staticEntities!!) {
            staticModelCache.add(entity.actorModel)
        }
        staticModelCache.end()
    }

}
