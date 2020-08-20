package de.fatox.meta.ui.windows

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Button
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
		titleLabel.setAlignment(Align.left)

		titleTable.apply {
			left().padLeft(2f)
			if (closeButton) {
				val exitButton = VisImageButton("close-window").apply {
					setColor(1f, 1f, 1f, 0.2f)
					onChange { close() }
					addListener(object : ClickListener() {
						override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
							event.cancel()
							return true
						}
					})
				}
				add(exitButton).padRight(-padRight + 0.7f)
			}

			// Separator
			top()
			row().height(2f)
			add(Separator()).growX().padTop(2f).colspan(if (closeButton) 2 else 1)
			padTop(2f)
		}

		if (resizable) {
			padBottom(6f)
			super.setResizable(true)
		}
		contentTable.top().pad(5f, 1f, 1f, 1f)
		add(contentTable).top().grow()
		row()
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
