package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.ui.TextField
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaType

/**
 * Text input using Meta's generated field drawables and TTF font. Prefer this over raw Scene2D/VisUI fields.
 */
open class MetaTextField @JvmOverloads constructor(
	text: String = "",
	size: Int = MetaType.BODY,
	fontProvider: FontProvider = inject(),
) : TextField(text, textFieldStyle(size, fontProvider)) {

	var isFocusBorderEnabled: Boolean = true
		set(value) {
			field = value
			style = TextFieldStyle(style).apply {
				if (!value) focusedBackground = background
			}
		}

	companion object {
		fun textFieldStyle(
			size: Int = MetaType.BODY,
			fontProvider: FontProvider = inject(),
			styleName: String = MetaSkin.TEXT_FIELD,
		): TextFieldStyle {
			val skin = MetaSkin.skin()
			val baseStyle = if (skin.has(styleName, TextFieldStyle::class.java)) {
				skin.get(styleName, TextFieldStyle::class.java)
			} else {
				skin.get(TextFieldStyle::class.java)
			}
			val font = fontProvider.getFont(size, FontType.REGULAR)
			return TextFieldStyle(baseStyle).apply {
				this.font = font
				messageFont = font
			}
		}
	}
}
