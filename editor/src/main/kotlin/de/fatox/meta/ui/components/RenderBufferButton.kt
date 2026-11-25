package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import de.fatox.meta.api.graphics.GLShaderHandle
import de.fatox.meta.api.graphics.RenderBufferHandle
import de.fatox.meta.api.model.RenderBufferData
import de.fatox.meta.api.model.RenderBufferData.IN.FULLSCREEN
import de.fatox.meta.api.model.RenderBufferData.IN.GEOMETRY
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.shader.MetaShaderComposer
import de.fatox.meta.shader.MetaShaderLibrary
import de.fatox.meta.util.GoldenRatio

/**
 * Controls the shader, input type, and depth attachment for one composition pass.
 */
class RenderBufferButton(text: String, size: Int) : MetaButtonContainer() {
	private val shaderLibrary: MetaShaderLibrary by lazyInject()
	private val shaderComposer: MetaShaderComposer by lazyInject()

	private val nameLabel: MetaLabel = MetaLabel(text, size, Color.WHITE)
	private val inSelect = MetaSelectBox<RenderBufferData.IN>()
	private val shaderSelect = MetaSelectBox<GLShaderHandle>()
	private val depthCheckBox = MetaToggleButton("Depth", false, 11)
	lateinit var handle: RenderBufferHandle
	var prevHandle: RenderBufferHandle? = null

	constructor(handle: RenderBufferHandle, prevHandle: RenderBufferHandle?) : this("Pass", 11) {
		this.handle = handle
		this.prevHandle = prevHandle


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

		nameLabel.setAlignment(Align.center)
		top()
		clear()
		add(nameLabel).colspan(2).center().growX()
		row().padBottom(2f)

		add(MetaIcon("ri-code-s-slash-line", 22)).size(26f).padRight(2f).left()
		add(shaderSelect).growX()
		row().padTop(2f)

		add(MetaIcon("ri-box-3-line", 22)).size(26f).padRight(2f).left()
		add(inSelect).growX()
		row().padTop(2f)

		add(MetaIcon("ri-stack-line", 22)).size(26f).padRight(2f).left()
		add(depthCheckBox).growX()
		row().padTop(2f)

		inSelect.selected = handle.data.inType
		shaderSelect.selected = handle.metaShader.shaderHandle
		depthCheckBox.isChecked = handle.data.hasDepth

		row()
		setupFooter()
	}

	private fun setupFooter() {
		val table = MetaTable()
		table.center()
		val deleteBtn = MetaIconButton("ri-delete-bin-line")
		deleteBtn.addListener(object : ClickListener() {
			override fun clicked(event: InputEvent, x: Float, y: Float) {
				shaderComposer.removeBufferHandle(handle)
			}
		})
		table.add(deleteBtn).size(26f)

		add(table).growX().center().colspan(2)
	}
}
