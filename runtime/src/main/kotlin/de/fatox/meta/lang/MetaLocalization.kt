package de.fatox.meta.lang

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.I18NBundle
import de.fatox.meta.api.lang.Localization
import de.fatox.meta.api.lang.MetaLanguage
import de.fatox.meta.reactive.ReactiveValue
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import java.util.ResourceBundle
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
	private val rootBundle: I18NBundle?
	private val fallbackBundles: List<I18NBundle>
	private var currentBundles: List<I18NBundle>
	private val currentLanguageSignal: Signal<MetaLanguage>
	override val currentLanguage: ReactiveValue<MetaLanguage> get() = currentLanguageSignal

	init {
		require(languages.isNotEmpty()) { "At least one language is required" }
		require(languages.map { normalizeTag(it.tag) }.distinct().size == languages.size) {
			"Language tags must be unique"
		}
		fallbackLanguage = languageByTag(fallbackLanguageTag)
			?: error("Fallback language '$fallbackLanguageTag' is not advertised")
		rootBundle = loadRootBundle()
		fallbackBundles = loadBundles(fallbackLanguage)
		require(fallbackBundles.isNotEmpty() || rootBundle != null) {
			"No localization catalog found for '${fallbackLanguage.tag}' or the root language"
		}
		val initial = resolveLanguage(preferredLanguageTag) ?: fallbackLanguage
		currentBundles = if (initial == fallbackLanguage) fallbackBundles else loadBundles(initial)
		currentLanguageSignal = signal(initial)
	}

	override fun selectLanguage(tag: String): Boolean {
		val language = resolveLanguage(tag) ?: return false
		if (language == currentLanguageSignal.peek()) return true
		currentBundles = if (language == fallbackLanguage) fallbackBundles else loadBundles(language)
		currentLanguageSignal.value = language
		return true
	}

	override operator fun get(key: String): String {
		currentLanguageSignal.value
		return lookup(currentBundles, key) ?: lookup(fallbackBundles, key) ?: lookupRoot(key) ?: key
	}

	override fun format(key: String, vararg args: Any): String {
		currentLanguageSignal.value
		return format(currentBundles, key, args)
			?: format(fallbackBundles, key, args)
			?: rootBundle?.let { format(it, key, args) }
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

	private fun loadBundles(language: MetaLanguage): List<I18NBundle> {
		val baseName = bundleFileHandle.nameWithoutExtension()
		val parent = bundleFileHandle.parent()
		val candidates = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_DEFAULT)
			.getCandidateLocales(baseName, language.locale)
		return candidates.asSequence()
			.filter { it != Locale.ROOT }
			.map { locale ->
				val suffix = locale.toString()
				parent.child("${baseName}_$suffix")
			}
			.filter { it.sibling("${it.name()}.properties").exists() }
			.map { I18NBundle.createBundle(it, Locale.ROOT) }
			.toList()
	}

	private fun loadRootBundle(): I18NBundle? =
		if (bundleFileHandle.sibling("${bundleFileHandle.name()}.properties").exists()) {
			I18NBundle.createBundle(bundleFileHandle, Locale.ROOT)
		} else null

	private fun lookupRoot(key: String): String? = rootBundle?.let { lookup(it, key) }

	private fun lookup(bundles: List<I18NBundle>, key: String): String? {
		for (index in bundles.indices) lookup(bundles[index], key)?.let { return it }
		return null
	}

	private fun format(bundles: List<I18NBundle>, key: String, args: Array<out Any>): String? {
		for (index in bundles.indices) format(bundles[index], key, args)?.let { return it }
		return null
	}

	private fun lookup(bundle: I18NBundle, key: String): String? =
		runCatching { bundle[key] }.getOrNull()

	private fun format(bundle: I18NBundle, key: String, args: Array<out Any>): String? =
		runCatching { bundle.format(key, *args) }.getOrNull()

	private companion object {
		fun normalizeTag(tag: String): String = Locale.forLanguageTag(tag).toLanguageTag().lowercase(Locale.ROOT)
	}
}
