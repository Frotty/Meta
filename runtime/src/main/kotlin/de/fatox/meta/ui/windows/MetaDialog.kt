package de.fatox.meta.ui.windows

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.utils.Align
import de.fatox.meta.api.extensions.onChange
import de.fatox.meta.api.extensions.onClick
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.UiControlHelper
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaIconButton
import de.fatox.meta.ui.components.MetaTable

/**
 * Created by Frotty on 04.06.2016.
 */
abstract class MetaDialog(title: String = "", hasCloseButton: Boolean) :
	MetaWindow(title, false, hasCloseButton, hasHeader = title.isNotBlank()) {
	override val preserveCenterOnAutoFit: Boolean = true
	protected val buttonTable = MetaTable()
	protected val statusLabel = MetaLabel("", 14)
	private val statusCell: Cell<MetaLabel>
	private val buttonCell: Cell<MetaTable>

	var dialogListener: DialogListener = EmptyListener
	private var buttonCount = 0
	private val uiControlHelper: UiControlHelper by lazyInject()
	private var previousCursorCatched = false
	private var cursorCatchedCaptured = false

	/**
	 * Optional contextual bottom overlay shown above the modal backdrop while this dialog is top-most. This is kept
	 * outside the dialog's layout so prompts stay in a stable screen-edge position. A null value intentionally hides
	 * the underlying screen overlay while the dialog owns input.
	 */
	var bottomOverlay: Actor? = null
		set(value) {
			if (field === value) return
			field = value
			if (stage != null) uiManager.onDialogBottomOverlayChanged(this)
		}

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
		fitAndCenterInStage()
		setColor(1f, 1f, 1f, 0f)
		clearActions()
		addAction(Actions.alpha(0.95f, 0.75f))
		// Remember the caller's cursor-catch state so onRemovedFromStage can restore it on EVERY exit path.
		if (!cursorCatchedCaptured) {
			previousCursorCatched = Gdx.input.isCursorCatched
			cursorCatchedCaptured = true
		}
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
		// Dialogs always need a usable pointer. Reassert this when an underlying dialog regains focus after a
		// stacked dialog closes; the closing dialog may have restored a captured pre-dialog cursor state.
		Gdx.input.isCursorCatched = false
		toFront()
		val keyboardTarget = firstTextField()
		stage.keyboardFocus = keyboardTarget ?: this
		stage.scrollFocus = this
		uiControlHelper.focusFirstIn(this, keyboardTarget)
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
	 * Detachment teardown, via [MetaWindow.onRemovedFromStage] (which fires on every removal path - close, a parent's
	 * `clearChildren`, a direct remove, a screen change). This guarantees the shared backdrop and any listener state
	 * are reset no matter how the dialog goes away. ([reactiveScope] is disposed by the base class right after this.)
	 */
	override fun onRemovedFromStage() {
		onHidden()
		if (cursorCatchedCaptured) {
			cursorCatchedCaptured = false
			Gdx.input.isCursorCatched = previousCursorCatched
		}
		uiControlHelper.clearFocusIfInside(this)
		uiManager.onDialogRemoved(this)
	}

	override fun close() {
		// Leave the modal layer immediately so the shared backdrop repositions/clears NOW, instead of lingering over
		// this (about-to-be-invisible) dialog for the whole close fade-out. setStage(null) is still the catch-all for
		// any other disposal path. onDialogRemoved is idempotent, so the later setStage call is a harmless no-op.
		uiManager.onDialogRemoved(this)
		super.close()
	}

	init {
		if (hasCloseButton) {
			// onChange (not a raw EventListener, whose handle() runs for every enter/exit/touch event) so a click
			// fires onResult(null) exactly once.
			val btn = titleTable.cells[titleTable.cells.size - 1].actor
			(btn as? MetaIconButton)?.onChange { dialogListener.onResult(null) }
		}
		contentTable.top()
		statusLabel.setAlignment(Align.center)
		statusLabel.setWrap(true)
		statusCell = add(statusLabel).growX()
		row()
		buttonCell = add(buttonTable).bottom().growX().pad(MetaSpacing.SM)
		updateOptionalRows()
	}

	private fun updateOptionalRows() {
		val hasStatus = statusLabel.text.isNotEmpty()
		statusLabel.isVisible = hasStatus
		statusCell.height(if (hasStatus) Value.prefHeight else Value.zero)

		// A consumer may deliberately move this inherited table into its designed body. Its old outer cell then stays
		// collapsed even when the table contains buttons.
		val hasActions = buttonTable.parent === this && buttonTable.children.size > 0
		buttonTable.isVisible = hasActions
		buttonCell.height(if (hasActions) Value.prefHeight else Value.zero)
		buttonCell.pad(if (hasActions) MetaSpacing.SM else 0f)
	}

	override fun getPrefHeight(): Float {
		updateOptionalRows()
		return super.getPrefHeight()
	}

	override fun layout() {
		updateOptionalRows()
		super.layout()
	}

	override fun act(delta: Float) {
		// Cursor capture can be changed after show() by game/controller input in the same frame. Keep the contract
		// true for the complete lifetime of every visible dialog instead of relying on a one-shot state change.
		if (isVisible && Gdx.input.isCursorCatched) Gdx.input.isCursorCatched = false
		super.act(delta)
	}
}
