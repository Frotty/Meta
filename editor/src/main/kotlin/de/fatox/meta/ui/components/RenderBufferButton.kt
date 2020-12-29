package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.*
import de.fatox.meta.Meta
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.graphics.GLShaderHandle
import de.fatox.meta.api.graphics.RenderBufferHandle
import de.fatox.meta.api.model.RenderBufferData
import de.fatox.meta.api.model.RenderBufferData.IN.FULLSCREEN
import de.fatox.meta.api.model.RenderBufferData.IN.GEOMETRY
import de.fatox.meta.injection.Inject
import de.fatox.meta.shader.MetaShaderComposer
import de.fatox.meta.shader.MetaShaderLibrary
import de.fatox.meta.util.GoldenRatio

/**
 * A Button displayed in the shader composer representing one shaderpass
 * They can be moved left & right and be deleted.
 */
class RenderBufferButton(text: String, size: Int) : Button(VisUI.getSkin().get(VisTextButton.VisTextButtonStyle::class.java)) {
    @Inject
    private lateinit var assetProvider: AssetProvider
    @Inject
    private lateinit var shaderLibrary: MetaShaderLibrary
    @Inject
    private lateinit var shaderComposer: MetaShaderComposer

    private val nameLabel: MetaLabel = MetaLabel(text, size, Color.WHITE)
    private val inSelect = VisSelectBox<RenderBufferData.IN>()
    private val shaderSelect = VisSelectBox<GLShaderHandle>()
    private val depthCheckBox = VisCheckBox("depth", false)
    lateinit var handle: RenderBufferHandle
    var prevHandle: RenderBufferHandle? = null

    constructor(handle: RenderBufferHandle, prevHandle: RenderBufferHandle?) : this("Pass", 11) {
        this.handle = handle
        this.prevHandle = prevHandle
        Meta.inject(this)

        color = Color.GRAY
        pad(GoldenRatio.C * 10, GoldenRatio.A * 20, GoldenRatio.C * 10, GoldenRatio.A * 20)
        inSelect.setItems(GEOMETRY, FULLSCREEN)
        inSelect.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                shaderComposer.setType(handle, inSelect.selected)
            }
        })
        shaderSelect.items = shaderLibrary.getLoadedShaders()
        shaderSelect.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                shaderComposer.changeShader(handle, shaderSelect.selected)
            }
        })

        depthCheckBox.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                shaderComposer.changeDepth(handle, depthCheckBox.isChecked)
            }
        })

        nameLabel
        nameLabel.setAlignment(Align.center)
        top()
        clear()
        add(nameLabel).colspan(2).center().growX()
        row().padBottom(2f)

        val codeImage = VisImage(assetProvider.getDrawable("ui/appbar.page.code.png"))
        codeImage.setScaling(Scaling.fit)
        add(codeImage).size(26f).padRight(2f).left()
        add(shaderSelect).growX()
        row().padTop(2f)

        val boxImage = VisImage(assetProvider.getDrawable("ui/appbar.box.png"))
        boxImage.setScaling(Scaling.fit)
        add(boxImage).size(26f).padRight(2f).left()
        add<VisSelectBox<RenderBufferData.IN>>(inSelect).growX()
        row().padTop(2f)

        val dImage = VisImage(assetProvider.getDrawable("ui/appbar.grade.d.png"))
        dImage.setScaling(Scaling.fit)
        add(dImage).size(26f).padRight(2f).left()
        add(depthCheckBox).growX()
        row().padTop(2f)

        inSelect.selected = handle.data.inType
        shaderSelect.selected = handle.metaShader.shaderHandle
        depthCheckBox.isChecked = handle.data.hasDepth

        row()

        setupUniforms()

        setupFooter()
    }

    private fun setupUniforms() {
        val table = Table()
        table.center()
        table.add(VisLabel("Uniforms"))
        table.row()

        add(table).colspan(2)
        row()
    }

    private fun setupFooter() {
        val table = Table()
        table.center()
        val moveLeftBtn = VisImageButton(assetProvider.getDrawable("ui/appbar.chevron.left.png"))
        moveLeftBtn.image.setScaling(Scaling.fill)
        table.add(moveLeftBtn).size(26f)
        val deleteBtn = VisImageButton(assetProvider.getDrawable("ui/appbar.delete.png"))
        deleteBtn.addListener(object : MetaClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                shaderComposer.removeBufferHandle(handle)
            }
        })
        deleteBtn.image.setScaling(Scaling.fill)
        table.add(deleteBtn).size(26f)
        val moveRightBtn = VisImageButton(assetProvider.getDrawable("ui/appbar.chevron.right.png"))
        moveRightBtn.image.setScaling(Scaling.fill)
        table.add(moveRightBtn).size(26f)

        add(table).growX().center().colspan(2)
    }
}
