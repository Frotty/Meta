package de.fatox.meta.ui.windows

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.Separator
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.extensions.onChange
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

/**
 * Created by Frotty on 08.05.2016.
 */
abstract class MetaWindow(
	title: String,
	resizable: Boolean = false,
	closeButton: Boolean = false,
) : VisWindow(title, if (resizable) "resizable" else "default") {
	protected val uiManager: UIManager by lazyInject()
	protected val assetProvider: AssetProvider by lazyInject()
	protected val metaData: MetaData by lazyInject()

	var contentTable: Table = VisTable()

	private var startDrag = false

	init {
		titleTable.left().padLeft(2f)
		titleLabel.setAlignment(Align.left)
		if (closeButton) {
			addExitButton()
		}
		// Separator
		titleTable.top()
		titleTable.row().height(2f)
		titleTable.add(Separator()).growX().padTop(2f).colspan(if (closeButton) 2 else 1)
		titleTable.padTop(2f)
		if (resizable) {
			padBottom(6f)
			isResizable = true
		}
		contentTable.top().pad(5f, 1f, 1f, 1f)
		add(contentTable).top().grow()
		row()
	}

	private fun addExitButton() {
		val titleLabel = titleLabel
		val titleTable = titleTable

		val closeButton = VisImageButton("close-window")
		closeButton.setColor(1f, 1f, 1f, 0.2f)
		titleTable.add(closeButton).padRight(-padRight + 0.7f)
		closeButton.apply {
			onChange { close() }
			addListener(object : ClickListener() {
				override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
					event?.cancel()
					return true
				}
			})
		}

		if (titleLabel.labelAlign == Align.center && titleTable.children.size == 2)
			titleTable.getCell(titleLabel).padLeft(closeButton.width * 2)
	}

	fun setDefaultSize(width: Float, height: Float) {
		setDefault(x, y, width, height)
	}

	fun setDefaultPos(x: Float, y: Float) {
		setDefault(x, y, width, height)
	}

	fun setDefault(x: Float, y: Float, width: Float, height: Float) {
		setPosition(x, y)
		setSize(width, height)
	}

	override fun draw(batch: Batch, parentAlpha: Float) {
		super.draw(batch, parentAlpha)
		if (isDragging) {
			startDrag = true
		} else if (startDrag) {
			startDrag = false
			uiManager.updateWindow(this)
		}
	}

	override fun setVisible(visible: Boolean) {
		super.setVisible(visible)
		setColor(1f, 1f, 1f, 0f)
		addAction(Actions.alpha(0.9f, 0.75f))
	}

	override fun setResizable(isResizable: Boolean) {
		super.setResizable(isResizable)
		if (VisUI.getSkin().has("resizeable", WindowStyle::class.java)) {
			style = VisUI.getSkin().get(if (isResizable) "resizeable" else "default", WindowStyle::class.java)
		}
	}

	public override fun close() {
		super.close()
		uiManager.closeWindow(this)
	}
}
