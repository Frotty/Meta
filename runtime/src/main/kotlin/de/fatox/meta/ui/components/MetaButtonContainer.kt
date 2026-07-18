package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import de.fatox.meta.api.extensions.cursorPointer
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.batch
import de.fatox.meta.reactive.signal
import de.fatox.meta.reactive.subscribe
import de.fatox.meta.ui.MetaFocusable
import de.fatox.meta.ui.MetaButtonTier
import de.fatox.meta.ui.MetaSkin

/** Meta-styled button container for custom child layouts. */
open class MetaButtonContainer(
	tier: MetaButtonTier = MetaButtonTier.SECONDARY,
) :
	Button(MetaSkin.buttonStyle(tier)),
	MetaFocusable {
	private val focusStyle = MetaButtonFocusStyle(this, style, MetaSkin::focusedButtonStyle)
	private val disabledTint = MetaDisabledTint(this)

	val checkedValue: Signal<Boolean> = signal(isChecked)
	val disabledValue: Signal<Boolean> = signal(isDisabled)
	@Suppress("unused")
	private val checkedBinding = checkedValue.subscribe { setChecked(checkedValue.peek()) }
	@Suppress("unused")
	private val disabledBinding = disabledValue.subscribe { setDisabled(disabledValue.peek()) }
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
				checkedValue.value = isChecked
			}
		})
	}

	override fun childrenChanged() {
		super.childrenChanged()
		isolateNestedButtons(this)
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
			if (child is Button) child.addListener(nestedButtonIsolation)
			if (child is Group) isolateNestedButtons(child)
		}
	}

	override fun setMetaFocused(focused: Boolean) {
		focusStyle.setFocused(focused)
	}

	override fun setDisabled(isDisabled: Boolean) {
		super.setDisabled(isDisabled)
		touchable = if (isDisabled) Touchable.disabled else Touchable.enabled
		batch {
			disabledValue.value = isDisabled
			disabledTint.apply(isDisabled)
		}
	}

	override fun setChecked(isChecked: Boolean) {
		super.setChecked(isChecked)
		checkedValue.value = this.isChecked
	}
}
