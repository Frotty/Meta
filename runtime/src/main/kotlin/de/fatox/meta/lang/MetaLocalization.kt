package de.fatox.meta.lang

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.I18NBundle
import de.fatox.meta.api.lang.Localization
import de.fatox.meta.api.lang.MetaLanguage
import de.fatox.meta.reactive.ReactiveValue
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import java.util.Locale

/** File-backed [Localization]. Catalogs use libGDX's standard `base[_language].properties` convention. */
class MetaLocalization(
	private val bundleFileHandle: FileHandle,
	languages: List<MetaLanguage>,
	fallbackLanguageTag: String,
	preferredLanguageTag: String? = null,
) : Localization {
	override val languages = languages.toList()
	private val fallbackLanguage: MetaLanguage
	private val fallbackBundle: I18NBundle
	private var currentBundle: I18NBundle
	private val currentLanguageSignal: Signal<MetaLanguage>
	override val currentLanguage: ReactiveValue<MetaLanguage> get() = currentLanguageSignal

	init {
		require(languages.isNotEmpty()) { "At least one language is required" }
		require(languages.map { normalizeTag(it.tag) }.distinct().size == languages.size) {
			"Language tags must be unique"
		}
		fallbackLanguage = languageByTag(fallbackLanguageTag)
			?: error("Fallback language '$fallbackLanguageTag' is not advertised")
		fallbackBundle = loadBundle(fallbackLanguage)
		val initial = resolveLanguage(preferredLanguageTag) ?: fallbackLanguage
		currentBundle = if (initial == fallbackLanguage) fallbackBundle else loadBundle(initial)
		currentLanguageSignal = signal(initial)
	}

	override fun selectLanguage(tag: String): Boolean {
		val language = resolveLanguage(tag) ?: return false
		if (language == currentLanguageSignal.peek()) return true
		val bundle = if (language == fallbackLanguage) fallbackBundle else loadBundle(language)
		currentBundle = bundle
		currentLanguageSignal.value = language
		return true
	}

	override operator fun get(key: String): String {
		currentLanguageSignal.value
		return lookup(currentBundle, key) ?: lookup(fallbackBundle, key) ?: key
	}

	override fun format(key: String, vararg args: Any): String {
		currentLanguageSignal.value
		return format(currentBundle, key, args)
			?: format(fallbackBundle, key, args)
			?: key
	}

	private fun languageByTag(tag: String): MetaLanguage? {
		val normalized = normalizeTag(tag)
		return languages.firstOrNull { normalizeTag(it.tag) == normalized }
	}

	private fun resolveLanguage(tag: String?): MetaLanguage? {
		if (tag.isNullOrBlank()) return null
		languageByTag(tag)?.let { return it }
		val languageCode = Locale.forLanguageTag(tag).language
		if (languageCode.isBlank()) return null
		return languages.firstOrNull { it.locale.language.equals(languageCode, ignoreCase = true) }
	}

	private fun loadBundle(language: MetaLanguage): I18NBundle {
		val bundle = I18NBundle.createBundle(bundleFileHandle, language.locale)
		if (bundle.locale == Locale.ROOT || bundle.locale.language.equals(language.locale.language, ignoreCase = true)) {
			return bundle
		}
		// libGDX falls back through Locale.getDefault() before the root catalog. That is useful for implicit locale
		// lookup, but wrong after the player explicitly selected a language: English must stay English on a German OS.
		return I18NBundle.createBundle(bundleFileHandle, Locale.ROOT)
	}

	private fun lookup(bundle: I18NBundle, key: String): String? =
		runCatching { bundle[key] }.getOrNull()

	private fun format(bundle: I18NBundle, key: String, args: Array<out Any>): String? =
		runCatching { bundle.format(key, *args) }.getOrNull()

	private companion object {
		fun normalizeTag(tag: String): String = Locale.forLanguageTag(tag).toLanguageTag().lowercase(Locale.ROOT)
	}
}
