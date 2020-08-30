package de.fatox.meta.lang

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.I18NBundle
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.api.extensions.error
import de.fatox.meta.api.lang.AvailableLanguages
import de.fatox.meta.api.lang.LanguageBundle
import java.util.*

private val log = MetaLoggerFactory.logger {}

class MetaLanguageBundle : LanguageBundle {
	private lateinit var currentBundle: I18NBundle

	private val currentLanguage: AvailableLanguages? = null

	override fun loadBundle(lang: AvailableLanguages) {
		val baseFileHandle = Gdx.files.internal("lang/MetagineBundle")
		val locale = Locale(lang.toString().toLowerCase(), lang.toString(), "")
		try {
			currentBundle = I18NBundle.createBundle(baseFileHandle, locale)
		} catch (e: Throwable) {
			log.error(e) { e.localizedMessage }
			return
		}
		log.debug { "Loaded: $lang" }
		log.debug { "Current Locale: ${currentBundle.locale}" }
	}

	/**
	 * Returns the S
	 *
	 * @param key
	 */
	override fun get(key: String): String {
		return currentBundle[key]
	}

	override fun format(key: String, vararg args: Any): String {
		return currentBundle.format(key, *args)
	}

	init {
		loadBundle(AvailableLanguages.EN)
	}
}