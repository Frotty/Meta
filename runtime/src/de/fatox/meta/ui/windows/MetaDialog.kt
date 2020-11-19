package de.fatox.meta.ui.windows

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import de.fatox.meta.api.extensions.onClick

/**
 * Created by Frotty on 04.06.2016.
 */
abstract class MetaDialog(title: String = "", hasCloseButton: Boolean) : MetaWindow(title, false, hasCloseButton) {
	protected val buttonTable = VisTable()
	protected val statusLabel = VisLabel()

	var dialogListener: DialogListener = EmptyListener
	private var buttonCount = 0

	fun interface DialogListener {
		fun onResult(any: Any?)
	}

	object EmptyListener : DialogListener {
		override fun onResult(any: Any?) = Unit
	}

	fun <T : Button> addButton(button: T, align: Int, result: Any?): T {
		button.onClick<Button> {
			if (!button.isDisabled) {
				dialogListener.onResult(result)
			}
		}
		if (buttonCount > 0) {
			buttonTable.add().growX()
		}
		buttonCount++
		buttonTable.add(button).align(align)
		return button
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
			(btn as? VisImageButton)?.addListener {
				dialogListener.onResult(null)
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