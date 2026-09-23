package de.fatox.meta.lang

import com.badlogic.gdx.files.FileHandle
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
