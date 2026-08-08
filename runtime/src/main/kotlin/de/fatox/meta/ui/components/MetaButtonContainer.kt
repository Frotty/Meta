package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import de.fatox.meta.api.extensions.cursorPointer
import de.fatox.meta.ui.MetaFocusable
import de.fatox.meta.ui.MetaButtonTier
import de.fatox.meta.ui.MetaSkin

/**
 * Meta-styled button surface for custom child layouts. [checkedValue] and [disabledValue] are bidirectional; nested
 * buttons retain ownership of their own presses instead of activating this container.
 */
open class MetaButtonContainer(
	tier: MetaButtonTier = MetaButtonTier.SECONDARY,
) :
	Button(MetaSkin.buttonStyle(tier)),
	MetaFocusable {
	private val focusStyle = MetaButtonFocusStyle(this, style, MetaSkin::focusedButtonStyle)
	private val disabledTint = MetaDisabledTint(this)
	private val buttonModel = MetaButtonModel(this, disabledTint)

	val checkedValue = buttonModel.checkedValue
	val disabledValue = buttonModel.disabledValue
	private val nestedButtonIsolation = object : InputListener() {
		override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
			// This listener is installed after the nested button's own handlers. The child keeps ownership of the
			// press, while propagation ends before the composed parent can also become pressed or checked.
			event.stop()
			return false
		}
	}

	init {
		cursorPointer()
		addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				buttonModel.syncChecked()
			}
		})
	}

	override fun childrenChanged() {
		super.childrenChanged()
		isolateNestedButtons(this)
		buttonModel.refreshDisabledAppearance()
	}

	override fun layout() {
		// Also catches buttons added later inside an already attached nested group.
		isolateNestedButtons(this)
		super.layout()
	}

	private fun isolateNestedButtons(group: Group) {
		val children = group.children
		for (i in 0 until children.size) {
			val child = children[i]
			if (child is Button && !child.listeners.contains(nestedButtonIsolation, true)) {
				child.addListener(nestedButtonIsolation)
			}
			if (child is Group) isolateNestedButtons(child)
		}
	}

	override fun setMetaFocused(focused: Boolean) {
		focusStyle.setFocused(focused)
	}

	override fun setDisabled(isDisabled: Boolean) {
		super.setDisabled(isDisabled)
		buttonModel.syncDisabled()
	}

	override fun setChecked(isChecked: Boolean) {
		super.setChecked(isChecked)
		buttonModel.syncChecked()
	}
}
