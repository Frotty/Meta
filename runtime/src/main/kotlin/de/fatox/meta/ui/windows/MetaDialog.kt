package de.fatox.meta.ui.windows

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.TextField
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
	protected val buttonTable = MetaTable()
	protected val statusLabel = MetaLabel("", 14)

	var dialogListener: DialogListener = EmptyListener
	private var buttonCount = 0
	private val uiControlHelper: UiControlHelper by lazyInject()
	private var previousCursorCatched = false
	private var cursorCatchedCaptured = false

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
		add(statusLabel).growX()
		row()
		add(buttonTable).bottom().growX().pad(MetaSpacing.SM)
	}
}
