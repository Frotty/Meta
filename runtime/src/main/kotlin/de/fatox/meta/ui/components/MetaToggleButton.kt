package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.ui.Button
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaType

/** Text button variant that participates in ButtonGroup-style checked selection. */
class MetaToggleButton @JvmOverloads constructor(
	text: String = "",
	checked: Boolean = false,
	size: Int = MetaType.BODY,
	type: FontType = FontType.REGULAR,
) : MetaTextButton(text, size, type) {
	init {
		installMetaStyle(Button.ButtonStyle(MetaSkin.skin().get(MetaSkin.BUTTON_TOGGLE, Button.ButtonStyle::class.java)))
		isChecked = checked
	}
}
