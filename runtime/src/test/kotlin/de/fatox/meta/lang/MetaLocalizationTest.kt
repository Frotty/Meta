package de.fatox.meta.lang

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.I18NBundle
import de.fatox.meta.api.lang.MetaLanguage
import de.fatox.meta.reactive.effect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.Locale

internal class MetaLocalizationTest {

	@TempDir
	lateinit var temporaryDirectory: Path

	@Test
	fun `language changes update reactive readers and preserve fallback text`() {
		val base = catalogs(
			english = "greeting=Hello\nscore=Score: {0}\nenglishOnly=Fallback text\n",
			german = "greeting=Hallo\nscore=Punkte: {0}\n",
		)
		val localization = MetaLocalization(base, LANGUAGES, "en", "de-DE")
		var observed = ""
		var updates = 0
		val binding = effect("localizedText") {
			observed = localization["greeting"]
			updates++
		}

		assertEquals("Deutsch", localization.currentLanguage.value.displayName)
		assertEquals("Hallo", observed)
		assertEquals("Punkte: 12", localization.format("score", 12))
		assertEquals("Fallback text", localization["englishOnly"])
		assertEquals("missing.key", localization["missing.key"])

		assertTrue(localization.selectLanguage("en-US"))
		assertEquals("Hello", observed)
		assertEquals(2, updates)
		assertFalse(localization.selectLanguage("pl"))
		assertEquals("Hello", observed)
		assertEquals(2, updates)
		binding.dispose()
	}

	@Test
	fun `explicit selection does not fall through to the operating system language`() {
		val originalLocale = Locale.getDefault()
		try {
			Locale.setDefault(Locale.GERMAN)
			val localization = MetaLocalization(
				catalogs(english = "greeting=Hello\n", german = "greeting=Hallo\n"),
				LANGUAGES,
				"en",
				"en",
			)

			assertEquals("en", localization.currentLanguage.value.tag)
			assertEquals("Hello", localization["greeting"])
		} finally {
			Locale.setDefault(originalLocale)
		}
	}

	@Test
	fun `configured fallback is checked before root catalog for missing selected entries`() {
		val base = catalogs(english = "rootOnly=Root text\n", german = "fallbackOnly=Deutsch\n")
		val localization = MetaLocalization(
			base,
			LANGUAGES + MetaLanguage("fr", "Français"),
			"de",
			"fr",
		)

		assertEquals("Deutsch", localization["fallbackOnly"])
		assertEquals("Root text", localization["rootOnly"])
	}

	@Test
	fun `regional selection does not load a sibling operating system locale`() {
		val originalLocale = Locale.getDefault()
		try {
			Locale.setDefault(Locale.forLanguageTag("pt-PT"))
			val directory = FileHandle(temporaryDirectory.toFile())
			val base = directory.child("regional")
			directory.child("regional.properties").writeString("greeting=Root\n", false, Charsets.UTF_8.name())
			directory.child("regional_pt_PT.properties").writeString("greeting=Portugal\n", false, Charsets.UTF_8.name())
			val localization = MetaLocalization(
				base,
				listOf(MetaLanguage("en", "English"), MetaLanguage("pt-BR", "Português")),
				"en",
				"pt-BR",
			)

			assertEquals("Root", localization["greeting"])
		} finally {
			Locale.setDefault(originalLocale)
		}
	}

	@Test
	fun `formatted values use the selected catalog locale`() {
		val base = catalogs(
			english = "number={0,number}\ncontraction=It's {0}\n",
			german = "number={0,number}\ncontraction=C'est {0}\n",
		)
		val localization = MetaLocalization(base, LANGUAGES, "en", "de")

		assertEquals("1.234,5", localization.format("number", 1234.5))
		assertEquals("C'est OxRox", localization.format("contraction", "OxRox"))
	}

	@Test
	fun `parent catalog formatting uses the selected regional locale`() {
		val directory = FileHandle(temporaryDirectory.toFile())
		val base = directory.child("regionalFormat")
		directory.child("regionalFormat.properties").writeString("date={0,date,short}\n", false, Charsets.UTF_8.name())
		directory.child("regionalFormat_en.properties").writeString("date={0,date,short}\n", false, Charsets.UTF_8.name())
		val localization = MetaLocalization(
			base,
			listOf(MetaLanguage("en", "English"), MetaLanguage("en-GB", "English UK")),
			"en",
			"en-GB",
		)

		val originalTimeZone = java.util.TimeZone.getDefault()
		try {
			java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
			assertEquals("24/09/2026", localization.format("date", java.util.Date.from(java.time.Instant.parse("2026-09-24T12:00:00Z"))))
		} finally {
			java.util.TimeZone.setDefault(originalTimeZone)
		}
	}

	@Test
	fun `localized catalog names preserve dots in the base name`() {
		val directory = FileHandle(temporaryDirectory.toFile())
		val base = directory.child("ui.messages")
		directory.child("ui.messages.properties").writeString("greeting=Root\n", false, Charsets.UTF_8.name())
		directory.child("ui.messages_de.properties").writeString("greeting=Deutsch\n", false, Charsets.UTF_8.name())
		val localization = MetaLocalization(base, LANGUAGES, "en", "de")

		assertEquals("Deutsch", localization["greeting"])
	}

	@Test
	fun `root catalog formatting uses the configured fallback locale`() {
		val directory = FileHandle(temporaryDirectory.toFile())
		val base = directory.child("rootLocale")
		directory.child("rootLocale.properties").writeString("number={0,number}\n", false, Charsets.UTF_8.name())
		val localization = MetaLocalization(base, LANGUAGES, "de", "de")

		assertEquals("1.234,5", localization.format("number", 1234.5))
	}

	@Test
	fun `missing keys still use fallback when libgdx missing-key exceptions are disabled`() {
		val originalExceptionMode = I18NBundle.getExceptionOnMissingKey()
		try {
			I18NBundle.setExceptionOnMissingKey(false)
			val localization = MetaLocalization(
				catalogs(english = "rootOnly=Root text\n", german = "fallbackOnly=Deutsch\n"),
				LANGUAGES + MetaLanguage("fr", "Français"),
				"de",
				"fr",
			)

			assertEquals("Deutsch", localization["fallbackOnly"])
			assertEquals("unknown.key", localization["unknown.key"])
		} finally {
			I18NBundle.setExceptionOnMissingKey(originalExceptionMode)
		}
	}

	@Test
	fun `script locale catalogs use standard resource bundle names`() {
		val directory = FileHandle(temporaryDirectory.toFile())
		val base = directory.child("scripted")
		directory.child("scripted.properties").writeString("greeting=Root\n", false, Charsets.UTF_8.name())
		directory.child("scripted_zh_Hans_CN.properties").writeString("greeting=简体中文\n", false, Charsets.UTF_8.name())
		val localization = MetaLocalization(
			base,
			listOf(MetaLanguage("en", "English"), MetaLanguage("zh-Hans-CN", "简体中文")),
			"en",
			"zh-Hans-CN",
		)

		assertEquals("简体中文", localization["greeting"])
	}

	@Test
	fun `preferred regional script resolves through the most specific advertised parent`() {
		val directory = FileHandle(temporaryDirectory.toFile())
		val base = directory.child("scriptPreference")
		directory.child("scriptPreference.properties").writeString("greeting=Root\n", false, Charsets.UTF_8.name())
		directory.child("scriptPreference_zh_Hans.properties").writeString("greeting=简体中文\n", false, Charsets.UTF_8.name())
		directory.child("scriptPreference_zh_Hant.properties").writeString("greeting=繁體中文\n", false, Charsets.UTF_8.name())
		val localization = MetaLocalization(
			base,
			listOf(
				MetaLanguage("en", "English"),
				MetaLanguage("zh-Hans", "简体中文"),
				MetaLanguage("zh-Hant", "繁體中文"),
			),
			"en",
			"zh-Hant-TW",
		)

		assertEquals("zh-Hant", localization.currentLanguage.value.tag)
		assertEquals("繁體中文", localization["greeting"])
	}

	private fun catalogs(english: String, german: String): FileHandle {
		val directory = FileHandle(temporaryDirectory.toFile())
		val base = directory.child("messages")
		directory.child("messages.properties").writeString(english, false, Charsets.UTF_8.name())
		directory.child("messages_de.properties").writeString(german, false, Charsets.UTF_8.name())
		return base
	}

	private companion object {
		val LANGUAGES = listOf(
			MetaLanguage("en", "English"),
			MetaLanguage("de", "Deutsch"),
		)
	}
}
