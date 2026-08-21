package de.fatox.meta.graphics.font

/**
 * Application font choices. Every configured face is optional at runtime: [MetaFontProvider] falls back to the
 * corresponding font packaged by Meta, then to Meta's code-embedded emergency font if neither face is usable.
 */
class FontInfo(
	val normalFontPath: String = DEFAULT_REGULAR_FONT_PATH,
	val boldFontPath: String = DEFAULT_BOLD_FONT_PATH,
	val monoFontPath: String = DEFAULT_MONO_FONT_PATH,
	val iconFontPath: String = DEFAULT_ICON_FONT_PATH,
) {
	companion object {
		const val DEFAULT_REGULAR_FONT_PATH = "fonts/Montserrat.ttf"
		const val DEFAULT_BOLD_FONT_PATH = "fonts/Montserrat-Bold.ttf"
		const val DEFAULT_MONO_FONT_PATH = "fonts/RobotoMono.ttf"
		const val DEFAULT_ICON_FONT_PATH = "fonts/remixicon.ttf"
	}
}
