package de.fatox.meta.ui.windows

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Scaling
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import de.fatox.meta.api.graphics.RenderBufferHandle
import de.fatox.meta.api.model.RenderBufferData
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.injection.Singleton
import de.fatox.meta.shader.MetaShaderComposer
import de.fatox.meta.shader.MetaShaderLibrary
import de.fatox.meta.shader.ShaderComposition
import de.fatox.meta.ui.components.MetaClickListener
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.RenderBufferButton
import de.fatox.meta.ui.dialogs.ShaderCompositionWizard
import java.util.*

/**
 * Created by Frotty on 29.07.2016.
 */
@Singleton
class ShaderComposerWindow : MetaWindow("Shader Composer", true, true) {
    private val shaderLibrary: MetaShaderLibrary by lazyInject()
    private val shaderComposer: MetaShaderComposer by lazyInject()

    private var renderSelectbox: VisSelectBox<String>? = null
    private var bufferTable: VisTable? = null
    private var addButton: VisImageButton? = null

    private var handles: Array<RenderBufferHandle> = Array()

    init {
        setupEmpty()
        shaderComposer.addListener { this.onRemoveBuffer() }
        shaderLibrary.addListener {
			if (shaderComposer.currentComposition != null) {
				loadComposition(shaderComposer.currentComposition!!)
			}
        }
    }

    private fun setupEmpty() {
        val visImageButton = VisImageButton(assetProvider.getDrawable("ui/appbar.page.add.png"))
        visImageButton.addListener(object : MetaClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                uiManager.showDialog(ShaderCompositionWizard::class.java)
            }
        })
        visImageButton.image.setScaling(Scaling.fill)
        visImageButton.image.setSize(24f, 24f)

        bufferTable = VisTable()
        bufferTable!!.top().left()
        renderSelectbox = VisSelectBox()
        renderSelectbox!!.items = Array()
        renderSelectbox!!.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                val selectedComp = shaderComposer.getComposition(renderSelectbox!!.selected)
                if (selectedComp != null && shaderComposer.currentComposition != selectedComp) {
                    shaderComposer.currentComposition = selectedComp
                    loadComposition(selectedComp)
                }
            }
        })
        contentTable.left()
        contentTable.add(visImageButton).size(24f).top().left().padRight(2f)
        contentTable.add<VisSelectBox<String>>(renderSelectbox).width(256f).left()
        contentTable.add().growX()
        contentTable.row().padTop(2f)
        contentTable.add<VisTable>(bufferTable).colspan(3).grow()

        if (shaderComposer.compositions.size > 0) {
            for (i in 0 until shaderComposer.compositions.size) {
                addComposition(shaderComposer.compositions.get(i))
            }
        }
    }

    private fun setupNewBufferButton() {
        addButton = VisImageButton(assetProvider.getDrawable("ui/appbar.layer.add.png"))
        addButton!!.addListener(object : MetaClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                onAddBuffer()
            }
        })
        addButton!!.image.setAlign(Align.center)
        bufferTable!!.add<VisImageButton>(addButton).size(175f, 168f).left()
    }

    private fun onAddBuffer() {
        shaderComposer.addRenderBuffer(RenderBufferData(shaderLibrary.defaultShaderPath))
    }

    private fun onRemoveBuffer() {
        loadComposition(Objects.requireNonNull<ShaderComposition>(shaderComposer.currentComposition))
    }

    private fun loadBuffers(buffers: Array<RenderBufferHandle>) {
        bufferTable!!.clear()
        for (buffer in buffers) {
            loadBuffer(buffer)
        }
    }

    private fun loadBuffer(buffer: RenderBufferHandle) {
        val newButton = RenderBufferButton(buffer, if (handles.size > 0) handles.peek() else null)
        bufferTable!!.add<RenderBufferButton>(newButton).padRight(2f)
        bufferTable!!.add(MetaLabel(">", 14)).center().padRight(2f)

    }

    fun addComposition(shaderComposition: ShaderComposition) {
        val items = Array(renderSelectbox!!.items)
        items.add(shaderComposition.data.name)
        renderSelectbox!!.items = items
        renderSelectbox!!.selected = shaderComposition.data.name

        loadComposition(shaderComposition)
    }

    private fun loadComposition(shaderComposition: ShaderComposition) {
        bufferTable!!.clear()
        if (shaderComposition.data.renderBuffers.size > 0) {
            // Load existing
            loadBuffers(shaderComposition.bufferHandles)
        }
        setupNewBufferButton()
    }
}
