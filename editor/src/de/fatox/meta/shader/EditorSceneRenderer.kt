package de.fatox.meta.shader

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
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
import de.fatox.meta.api.dao.RenderBufferData
import de.fatox.meta.api.graphics.RenderBufferHandle
import de.fatox.meta.api.graphics.Renderer
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.assets.MetaData
import de.fatox.meta.entity.Meta3DEntity
import de.fatox.meta.graphics.renderer.FullscreenQuad
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.Inject
import de.fatox.meta.ui.components.MetaLabel
import java.util.*

/**
 * Created by Frotty on 17.04.2017.
 */
class EditorSceneRenderer : Renderer {
    @Inject
    private val batch: SpriteBatch? = null
    @Inject
    private val shaderComposer: MetaShaderComposer? = null
    @Inject
    private val projectManager: ProjectManager? = null
    @Inject
    private val cam: PerspectiveCamera? = null
    @Inject
    private val primitives: Primitives? = null
    @Inject
    private val metaData: MetaData? = null
    @Inject
    private val uiManager: UIManager? = null
    private val grid: Meta3DEntity

    private var sceneHandle: MetaSceneHandle? = null

    private val modelCache = ModelCache()
    private var modelBatch: ModelBatch? = null

    private val fsquad = FullscreenQuad(1f)
    private val compositeQuad = FullscreenQuad(1f)

    private var renderContext: RenderContext? = null
    private var lastComposition: ShaderComposition? = null

    init {
        Meta.inject(this)
        grid = Meta3DEntity(Vector3.Zero, primitives!!.linegrid, 1f)
    }

    override fun render(x: Float, y: Float) {
        if (renderContext == null) return
        if (sceneHandle!!.shaderComposition == null) {
            val table = Table()
            table.add(MetaLabel("No composition selected", 20)).pad(128f).center()
            uiManager!!.addTable(table, true, true)
        } else {
            val bufferHandles = sceneHandle!!.shaderComposition.bufferHandles
            for (bufferHandle in bufferHandles) {
                renderContext!!.begin()
                bufferHandle.begin()
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

                if (bufferHandle.data.inType === RenderBufferData.IN.GEOMETRY) {
                    modelBatch!!.begin(cam)
                    modelBatch!!.render(modelCache, bufferHandle.metaShader)
                    modelBatch!!.end()
                }
                bufferHandle.end()
                renderContext!!.end()
            }

            renderContext!!.begin()

            modelBatch!!.begin(cam)
            modelBatch!!.render(modelCache, sceneHandle!!.shaderComposition.outputBuffer.metaShader)
            modelBatch!!.end()
            renderContext!!.end()

            debugAll(x, y, bufferHandles)
        }
    }

    private fun debugAll(x: Float, y: Float, bufferHandles: Array<RenderBufferHandle>) {
        batch!!.disableBlending()
        batch.begin()
        var debugScreens = 1f
        for (bufferHandle in bufferHandles) {
            for (ignored in Objects.requireNonNull<Array<Texture>>(bufferHandle.colorTextures)) {
                debugScreens++
            }

        }
        var count = 0
        for (bufferHandle in bufferHandles) {
            val height = bufferHandle.height
            val width = bufferHandle.width
            val colorTextures = bufferHandle.colorTextures
            for (texture in colorTextures!!) {
                batch.draw(texture, x + width / debugScreens * count.toFloat() * 0.75f, y, width / debugScreens * 0.75f, height / debugScreens * 0.75f, 0f, 0f, 1f, 1f)
                count++
            }

        }
        batch.end()
    }

    override fun rebuild(width: Int, height: Int) {
        val currentComposition = sceneHandle!!.shaderComposition
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
        renderContext = RenderContext(DefaultTextureBinder(DefaultTextureBinder.WEIGHTED))
        modelBatch?.dispose()
        modelBatch = ModelBatch(renderContext)
        resize(width, height)
    }

    private fun resize(width: Int, height: Int) {
        resizeCam(width, height)

        val composition = shaderComposer!!.currentComposition
        for (bufferHandle in composition!!.bufferHandles) {
            bufferHandle.rebuild(width, height)
        }
    }

    private fun resizeCam(width: Int, height: Int) {
        cam!!.viewportWidth = width.toFloat()
        cam.viewportHeight = height.toFloat()
        cam.update()
    }

    override fun rebuildCache() {
        modelCache.begin()
        if (sceneHandle!!.data.showGrid) {
            modelCache.add(grid.actorModel)
        }
        for (entity in sceneHandle!!.entityManager.staticEntities) {
            modelCache.add(entity.actorModel)
        }
        modelCache.end()
    }


    fun setSceneHandle(sceneHandle: MetaSceneHandle) {
        this.sceneHandle = sceneHandle
        rebuildCache()
    }
}
