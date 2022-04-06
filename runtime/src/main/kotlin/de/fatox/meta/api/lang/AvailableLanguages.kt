package de.fatox.meta.api.lang

import java.util.*

enum class AvailableLanguages {
	EN, DE, HU, PL, ;

	val locale: Locale = Locale(name.lowercase(Locale.ENGLISH), name, "")
}
