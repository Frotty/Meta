package de.fatox.meta.lang

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.I18NBundle
import de.fatox.meta.Meta.Companion.inject
import de.fatox.meta.api.lang.AvailableLanguages
import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.lang.MetaLanguageBundle
import org.slf4j.LoggerFactory
import java.util.*

class MetaLanguageBundle : LanguageBundle {
    private var currentBundle: I18NBundle? = null
    private val currentLanguage: AvailableLanguages? = null
    override fun loadBundle(lang: AvailableLanguages) {
        val baseFileHandle = Gdx.files.internal("lang/MetagineBundle")
        val locale = Locale(lang.toString().toLowerCase(), lang.toString(), "")
        try {
            currentBundle = I18NBundle.createBundle(baseFileHandle, locale)
        } catch (e: Exception) {
            log.error(e.localizedMessage)
        }
        log.debug("Loaded: $lang")
        log.debug("Current Locale: " + currentBundle!!.locale)
    }

    /**
     * Returns the S
     *
     * @param key
     */
    override fun get(key: String): String {
        return currentBundle!![key]
    }

    override fun format(key: String, vararg args: Any): String {
        return currentBundle!!.format(key, *args)
    }

    companion object {
        private val log = LoggerFactory.getLogger(MetaLanguageBundle::class.java)
    }

    init {
        inject(this)
        loadBundle(AvailableLanguages.EN)
    }
}