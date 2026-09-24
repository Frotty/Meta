package de.fatox.meta.api.lang

import java.util.Locale

/** A language offered by an application. [displayName] should be written in that language. */
data class MetaLanguage(
	val tag: String,
	val displayName: String,
	val locale: Locale = Locale.forLanguageTag(tag),
) {
	init {
		require(tag.isNotBlank()) { "Language tag must not be blank" }
		require(displayName.isNotBlank()) { "Language display name must not be blank" }
		require(locale.language.isNotBlank()) { "Invalid language tag: $tag" }
	}
}
