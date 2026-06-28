package de.fatox.meta.ui.windows

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisTable
import de.fatox.meta.api.extensions.onClick
import de.fatox.meta.ui.components.MetaLabel

/**
 * Created by Frotty on 04.06.2016.
 */
abstract class MetaDialog(title: String = "", hasCloseButton: Boolean) : MetaWindow(title, false, hasCloseButton) {
	protected val buttonTable = VisTable()
	protected val statusLabel = MetaLabel("", 14)

	var dialogListener: DialogListener = EmptyListener
	private var buttonCount = 0

	fun interface DialogListener {
		fun onResult(any: Any?)
	}

	object EmptyListener : DialogListener {
		override fun onResult(any: Any?): Unit = Unit
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
		clearActions()
		addAction(Actions.alpha(0.95f, 0.75f))
		Gdx.input.isCursorCatched = false
		focusDialog()
	}

	/**
	 * Makes this dialog the front-most actor and grabs keyboard & scroll focus so it is immediately editable -
	 * keyboard input lands in its first text field (or, lacking one, the dialog itself). Called on [show] and again
	 * by the UI manager whenever this dialog becomes the top-most one (e.g. after a dialog stacked above it closes).
	 */
	open fun focusDialog() {
		val stage = stage ?: return
		toFront()
		stage.keyboardFocus = firstTextField() ?: this
		stage.scrollFocus = this
	}

	private fun firstTextField(group: Group = this): TextField? {
		for (child in group.children) {
			if (child is TextField) return child
			if (child is Group) firstTextField(child)?.let { return it }
		}
		return null
	}

	/** Called once when this dialog is detached from the stage, through ANY path (close, screen change, remove). */
	protected open fun onHidden() {}

	/**
	 * Single robust hook for detachment: scene2d routes every removal path - [close], a parent's `clearChildren`, a
	 * direct [remove], a screen change - through `setStage(null)`. Tying teardown to it (rather than only to
	 * [close]) guarantees the shared backdrop and any listener state are reset no matter how the dialog goes away.
	 */
	override fun setStage(stage: Stage?) {
		val wasOnStage = this.stage != null
		super.setStage(stage)
		if (wasOnStage && stage == null) {
			onHidden()
			uiManager.onDialogRemoved(this)
		}
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
		statusLabel.setWrap(true)
		add(statusLabel).growX()
		row()
		add(buttonTable).bottom().growX()
	}
}