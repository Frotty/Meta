package de.fatox.meta.api.lang

import de.fatox.meta.reactive.ReactiveValue

/**
 * Application localization with a required fallback language. Reading text inside a reactive effect or binding also
 * observes [currentLanguage], so existing widgets can update in place when the player changes language.
 */
interface Localization {
	val languages: List<MetaLanguage>
	val currentLanguage: ReactiveValue<MetaLanguage>

	/** Selects an advertised language by BCP 47 tag. Returns false without changing state for an unknown tag. */
	fun selectLanguage(tag: String): Boolean

	/** Returns translated text, falling back to the fallback catalog and finally to [key]. */
	operator fun get(key: String): String

	/** Formats translated text using the selected language, with the same fallback behavior as [get]. */
	fun format(key: String, vararg args: Any): String
}
