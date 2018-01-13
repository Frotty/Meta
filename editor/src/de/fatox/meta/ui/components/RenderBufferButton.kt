package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.*
import de.fatox.meta.Meta
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.dao.RenderBufferData
import de.fatox.meta.api.dao.RenderBufferData.IN.FULLSCREEN
import de.fatox.meta.api.dao.RenderBufferData.IN.GEOMETRY
import de.fatox.meta.api.graphics.GLShaderHandle
import de.fatox.meta.api.graphics.RenderBufferHandle
import de.fatox.meta.injection.Inject
import de.fatox.meta.shader.MetaShaderComposer
import de.fatox.meta.shader.MetaShaderLibrary
import de.fatox.meta.util.GoldenRatio

/**
 * Created by Frotty on 04.06.2016.
 */
class RenderBufferButton(text: String, size: Int) : Button(VisUI.getSkin().get<VisTextButton.VisTextButtonStyle>(VisTextButton.VisTextButtonStyle::class.java)) {
    private val nameLabel: MetaLabel
    @Inject
    private val assetProvider: AssetProvider? = null
    @Inject
    private val shaderLibrary: MetaShaderLibrary? = null
    @Inject
    private val shaderComposer: MetaShaderComposer? = null

    private val inSelect = VisSelectBox<RenderBufferData.IN>()
    private val shaderSelect = VisSelectBox<GLShaderHandle>()
    private val depthCheckBox = VisCheckBox("depth", false)

    var text: CharSequence
        get() = nameLabel.text
        set(text:CharSequence) = nameLabel.setText(text)

    private lateinit var handle: RenderBufferHandle

    constructor(handle: RenderBufferHandle) : this("Pass", 11) {
        this.handle = handle
        inSelect.selected = handle.data.inType
        shaderSelect.selected = handle.metaShader.shaderHandle
        depthCheckBox.isChecked = handle.data.hasDpeth
    }

    init {
        Meta.inject(this)
        color = Color.GRAY
        pad(GoldenRatio.C * 10, GoldenRatio.A * 20, GoldenRatio.C * 10, GoldenRatio.A * 20)
        inSelect.setItems(GEOMETRY, FULLSCREEN)
        shaderSelect.items = shaderLibrary!!.loadedShaders

        nameLabel = MetaLabel(text, size, Color.WHITE)
        nameLabel.setAlignment(Align.center)
        top()
        clear()
        add(nameLabel).colspan(2).center().growX()
        row().padTop(2f)

        val codeImage = VisImage(assetProvider!!.getDrawable("ui/appbar.page.code.png"))
        codeImage.setScaling(Scaling.fit)
        add(codeImage).size(24f).padRight(2f).left()
        add(shaderSelect).growX()
        row().padTop(2f)

        val boxImage = VisImage(assetProvider.getDrawable("ui/appbar.box.png"))
        boxImage.setScaling(Scaling.fit)
        add(boxImage).size(24f).padRight(2f).left()
        add<VisSelectBox<RenderBufferData.IN>>(inSelect).growX()
        row().padTop(2f)

        val dImage = VisImage(assetProvider.getDrawable("ui/appbar.grade.d.png"))
        dImage.setScaling(Scaling.fit)
        add(dImage).size(24f).padRight(2f).left()
        add(depthCheckBox).growX()
        row().padTop(2f)


        val table = Table()
        table.center()
        val moveLeftBtn = VisImageButton(assetProvider.getDrawable("ui/appbar.chevron.left.png"))
        moveLeftBtn.image.setScaling(Scaling.fill)
        table.add(moveLeftBtn).size(24f)
        val deleteBtn = VisImageButton(assetProvider.getDrawable("ui/appbar.delete.png"))
        deleteBtn.addListener(object : MetaClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                shaderComposer?.currentComposition?.removeBufferHandle(handle);
            }
        })
        deleteBtn.image.setScaling(Scaling.fill)
        table.add(deleteBtn).size(24f)
        val moveRightBtn = VisImageButton(assetProvider.getDrawable("ui/appbar.chevron.right.png"))
        moveRightBtn.image.setScaling(Scaling.fill)
        table.add(moveRightBtn).size(24f)

        add(table).growX().center().colspan(2)
        pack()

    }


}
