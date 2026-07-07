package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.batch
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaFocusable
import de.fatox.meta.ui.MetaSkin

/** Meta-styled button container for custom child layouts. */
open class MetaButtonContainer :
	Button(MetaSkin.skin().get(MetaSkin.BUTTON, ButtonStyle::class.java)),
	MetaFocusable {
	private val focusStyle = MetaButtonFocusStyle(this, style, MetaSkin::focusedButtonStyle)
	private val disabledTint = MetaDisabledTint(this)

	val checkedValue: Signal<Boolean> = signal(isChecked)
	val disabledValue: Signal<Boolean> = signal(isDisabled)

	init {
		addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				checkedValue.value = isChecked
			}
		})
	}

	override fun setMetaFocused(focused: Boolean) {
		focusStyle.setFocused(focused)
	}

	override fun setDisabled(isDisabled: Boolean) {
		super.setDisabled(isDisabled)
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
