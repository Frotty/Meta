package de.fatox.meta.graphics.font

class FontInfo(
	val normalFontPath: String,
	val boldFontPath: String,
	val monoFontPath: String,
	val iconFontPath: String = DEFAULT_ICON_FONT_PATH,
) {
	companion object {
		const val DEFAULT_ICON_FONT_PATH = "fonts/remixicon.ttf"
	}
}
