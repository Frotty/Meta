package de.fatox.meta.lang

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.I18NBundle
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.api.extensions.error
import de.fatox.meta.api.lang.AvailableLanguages
import de.fatox.meta.api.lang.LanguageBundle

private val log = MetaLoggerFactory.logger {}

class MetaLanguageBundle(private val bundleFileHandle: FileHandle) : LanguageBundle {
	private lateinit var currentBundle: I18NBundle

	override lateinit var currentLanguage: AvailableLanguages
		private set

	override fun loadBundle(lang: AvailableLanguages) {
		try {
			currentBundle = I18NBundle.createBundle(bundleFileHandle, lang.locale)
			currentLanguage = lang
			log.debug { "Loaded: $lang" }
			log.debug { "Current Locale: ${currentBundle.locale}" }
		} catch (e: Exception) {
			log.error(e) { e.localizedMessage }
		}
	}

	override operator fun get(key: String): String {
		return currentBundle[key]
	}

	override fun format(key: String, vararg args: Any): String {
		return currentBundle.format(key, *args)
	}

	init {
		loadBundle(AvailableLanguages.EN)
	}
}