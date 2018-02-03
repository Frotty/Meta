package de.fatox.meta.api.lang


interface LanguageBundle {
    fun loadBundle(lang: AvailableLanguages)

    operator fun get(key: String): String

    fun format(key: String, vararg args: Any): String
}
