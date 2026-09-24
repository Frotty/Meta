package de.fatox.meta.lang

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.I18NBundle
import de.fatox.meta.api.lang.Localization
import de.fatox.meta.api.lang.MetaLanguage
import de.fatox.meta.reactive.ReactiveValue
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import java.text.MessageFormat
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
	private val rootBundle: LocalizedBundle?
	private val fallbackBundles: List<LocalizedBundle>
	private var currentBundles: List<LocalizedBundle>
	private val currentLanguageSignal: Signal<MetaLanguage>
	override val currentLanguage: ReactiveValue<MetaLanguage> get() = currentLanguageSignal

	init {
		require(languages.isNotEmpty()) { "At least one language is required" }
		require(languages.map { normalizeTag(it.tag) }.distinct().size == languages.size) {
			"Language tags must be unique"
		}
		fallbackLanguage = languageByTag(fallbackLanguageTag)
			?: error("Fallback language '$fallbackLanguageTag' is not advertised")
		rootBundle = loadRootBundle(fallbackLanguage.locale)
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
			?: format(rootBundle, key, args)
			?: key
	}

	private fun languageByTag(tag: String): MetaLanguage? {
		val normalized = normalizeTag(tag)
		return languages.firstOrNull { normalizeTag(it.tag) == normalized }
	}

	private fun resolveLanguage(tag: String?): MetaLanguage? {
		if (tag.isNullOrBlank()) return null
		languageByTag(tag)?.let { return it }
		val locale = Locale.forLanguageTag(tag)
		if (locale.language.isBlank()) return null
		val candidates = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_DEFAULT)
			.getCandidateLocales("", locale)
		for (index in candidates.indices) {
			val candidate = candidates[index]
			if (candidate == Locale.ROOT) continue
			languageByTag(candidate.toLanguageTag())?.let { return it }
		}
		return null
	}

	private fun loadBundles(language: MetaLanguage): List<LocalizedBundle> {
		val baseName = bundleFileHandle.name()
		val parent = bundleFileHandle.parent()
		val control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_DEFAULT)
		val candidates = control
			.getCandidateLocales(baseName, language.locale)
		return candidates.asSequence()
			.filter { it != Locale.ROOT }
			.map { locale ->
				val catalogName = control.toBundleName(baseName, locale)
				locale to parent.child(catalogName)
			}
			.filter { (_, handle) -> handle.sibling("${handle.name()}.properties").exists() }
			.map { (_, handle) -> LocalizedBundle(I18NBundle.createBundle(handle, Locale.ROOT), language.locale) }
			.toList()
	}

	private fun loadRootBundle(locale: Locale): LocalizedBundle? =
		if (bundleFileHandle.sibling("${bundleFileHandle.name()}.properties").exists()) {
			LocalizedBundle(I18NBundle.createBundle(bundleFileHandle, Locale.ROOT), locale)
		} else null

	private fun lookupRoot(key: String): String? = rootBundle?.let { lookup(it.bundle, key) }

	private fun lookup(bundles: List<LocalizedBundle>, key: String): String? {
		for (index in bundles.indices) lookup(bundles[index].bundle, key)?.let { return it }
		return null
	}

	private fun format(bundles: List<LocalizedBundle>, key: String, args: Array<out Any>): String? {
		for (index in bundles.indices) {
			format(bundles[index], key, args)?.let { return it }
		}
		return null
	}

	private fun format(bundle: LocalizedBundle?, key: String, args: Array<out Any>): String? {
		bundle ?: return null
		val pattern = lookup(bundle.bundle, key) ?: return null
		return runCatching {
			MessageFormat(escapeTextFormatterPattern(pattern), bundle.locale).format(args)
		}.getOrNull()
	}

	/** Matches libGDX TextFormatter's MessageFormat escaping without inheriting another catalog's keys. */
	private fun escapeTextFormatterPattern(pattern: String): String {
		val escaped = StringBuilder(pattern.length)
		var index = 0
		while (index < pattern.length) {
			when (pattern[index]) {
				'\'' -> escaped.append("''")
				'{' -> {
					var end = index + 1
					while (end < pattern.length && pattern[end] == '{') end++
					val runLength = end - index
					repeat(runLength / 2) { if (it == 0) escaped.append('\''); escaped.append('{') }
					if (runLength >= 2 && runLength / 2 > 0) escaped.append('\'')
					if (runLength % 2 != 0) escaped.append('{')
					index = end - 1
				}
				else -> escaped.append(pattern[index])
			}
			index++
		}
		return escaped.toString()
	}

	private data class LocalizedBundle(val bundle: I18NBundle, val locale: Locale)

	private fun lookup(bundle: I18NBundle, key: String): String? =
		runCatching { bundle[key] }.getOrNull()?.takeUnless { it == "???$key???" }

	private companion object {
		fun normalizeTag(tag: String): String = Locale.forLanguageTag(tag).toLanguageTag().lowercase(Locale.ROOT)
	}
}
