package de.fatox.meta.ui.components

import com.kotcrab.vis.ui.widget.VisTextField
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.ui.MetaType

/**
 * A [VisTextField] that renders with the Meta TTF font instead of VisUI's baked-glyph skin font, so text input
 * matches [MetaLabel]/[MetaTextButton]. Use this (not a raw `VisTextField`) wherever the user types.
 *
 * It clones the skin style once at construction (so the shared skin style is never mutated) and swaps in the TTF
 * font for both the entered text and the message/placeholder.
 */
open class MetaTextField @JvmOverloads constructor(
	text: String = "",
	size: Int = MetaType.BODY,
	fontProvider: FontProvider = inject(),
) : VisTextField(text) {

	init {
		val font = fontProvider.getFont(size, FontType.REGULAR)
		style = VisTextFieldStyle(style).apply {
			this.font = font
			this.messageFont = font
		}
	}
}
