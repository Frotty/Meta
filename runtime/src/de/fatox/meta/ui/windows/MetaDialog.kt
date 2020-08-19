package de.fatox.meta.ui.windows

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import de.fatox.meta.ui.components.MetaClickListener

/**
 * Created by Frotty on 04.06.2016.
 */
abstract class MetaDialog(title: String = "", hasCloseButton: Boolean) : MetaWindow(title, false, hasCloseButton) {
	protected val buttonTable = VisTable()
	@JvmField
	protected val statusLabel = VisLabel()
	var dialogListener: DialogListener? = null
	private var buttonCount = 0

	interface DialogListener {
		fun onResult(any: Any?)
	}

	fun <T : Button?> addButton(button: Button, align: Int, result: Any?): T {
		button.addListener(object : MetaClickListener() {
			override fun clicked(event: InputEvent, x: Float, y: Float) {
				if (!button.isDisabled && dialogListener != null) {
					dialogListener!!.onResult(result)
				}
			}
		})
		if (buttonCount > 0) {
			buttonTable.add().growX()
		}
		buttonCount++
		buttonTable.add(button).align(align)
		return button as T
	}

	open fun show() {
		// Set color invisible for fade in to work
		centerWindow()
		setColor(1f, 1f, 1f, 0f)
		addAction(Actions.alpha(0.95f, 0.75f))
		Gdx.input.isCursorCatched = false
	}

	init {
		if (hasCloseButton) {
			val btn = titleTable.cells[titleTable.cells.size - 1].actor
			(btn as? VisImageButton)?.addListener { event: Event? ->
				dialogListener!!.onResult(null)
				false
			}
		}
		contentTable.top().padTop(4f)
		statusLabel.setAlignment(Align.center)
		statusLabel.wrap = true
		add(statusLabel).growX()
		row()
		add(buttonTable).bottom().growX()
	}
}