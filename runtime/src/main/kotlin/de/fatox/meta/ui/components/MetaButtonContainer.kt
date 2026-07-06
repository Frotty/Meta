package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.ui.Button
import de.fatox.meta.ui.MetaFocusable
import de.fatox.meta.ui.MetaSkin

/** Meta-styled button container for custom child layouts. */
open class MetaButtonContainer :
	Button(MetaSkin.skin().get(MetaSkin.BUTTON, ButtonStyle::class.java)),
	MetaFocusable {
	private val focusStyle = MetaButtonFocusStyle(this, style, MetaSkin::focusedButtonStyle)
	private val disabledTint = MetaDisabledTint(this)

	override fun setMetaFocused(focused: Boolean) {
		focusStyle.setFocused(focused)
	}

	override fun setDisabled(isDisabled: Boolean) {
		super.setDisabled(isDisabled)
		disabledTint.apply(isDisabled)
	}
}
