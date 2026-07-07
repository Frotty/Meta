package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisTextField
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.api.lang.AvailableLanguages
import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.error.MetaError
import de.fatox.meta.error.MetaErrorHandler
import de.fatox.meta.injection.MetaInject.Companion.global
import de.fatox.meta.test.GdxTestEnvironment
import de.fatox.meta.ui.MetaColor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class MetaInputComponentsTest {
	private lateinit var fontProvider: FontProvider
	private lateinit var font: BitmapFont

	@BeforeEach
	fun setUp() {
		GdxTestEnvironment.ensure()
		if (VisUI.isLoaded()) VisUI.dispose()
		font = testFont()
		VisUI.load(Skin().apply {
			add("default", VisTextField.VisTextFieldStyle().apply {
				this.font = this@MetaInputComponentsTest.font
				fontColor = Color.WHITE
				messageFontColor = Color.GRAY
			})
		})
		fontProvider = object : FontProvider {
			override fun getFont(size: Int, type: FontType): BitmapFont = this@MetaInputComponentsTest.font
			override fun write(x: Float, y: Float, text: String, size: Int, type: FontType) = Unit
		}
		global(clear = true) {
			singleton<FontProvider> { fontProvider }
			singleton<LanguageBundle> {
				object : LanguageBundle {
					override val currentLanguage: AvailableLanguages = AvailableLanguages.EN
					override fun loadBundle(lang: AvailableLanguages) = Unit
					override fun get(key: String): String = key
					override fun format(key: String, vararg args: Any): String = "$key ${args.joinToString()}"
				}
			}
		}
	}

	private fun testFont(): BitmapFont {
		val data = BitmapFont.BitmapFontData().apply {
			lineHeight = 12f
			capHeight = 9f
			ascent = 9f
			descent = -4f
			spaceXadvance = 4f
		}
		return BitmapFont(data, TextureRegion(), false).apply {
			for (code in 32..126) {
				this.data.setGlyph(code, BitmapFont.Glyph().apply {
					id = code
					width = 1
					height = 1
					xadvance = 8
				})
			}
		}
	}

	@AfterEach
	fun tearDown() {
		if (VisUI.isLoaded()) VisUI.dispose()
		global(clear = true) {}
	}

	@Test
	fun `MetaInputField exposes reactive text and invalid style state`() {
		val field = MetaInputField("start", fontProvider = fontProvider, placeholder = "Name")
		val validStyle = field.style

		assertEquals("start", field.textValue.value)

		field.setText("changed")
		assertEquals("changed", field.textValue.value)

		field.setInputValid(false)
		assertFalse(field.inputValidValue.value)
		assertNotSame(validStyle, field.style)

		val invalidStyle = field.style
		field.setInputValid(true)
		assertTrue(field.inputValidValue.value)
		assertNotSame(invalidStyle, field.style)
		assertSame(validStyle, field.style)
	}

	@Test
	fun `MetaTextArea validates text and tracks reactive state`() {
		val area = MetaTextArea("ok", fontProvider = fontProvider)
		area.addValidator(object : MetaInputValidator() {
			override fun validateInput(input: String, errors: MetaErrorHandler) {
				if (input.length < 3) errors.add(MetaError("Too short", ""))
			}
		})

		assertFalse(area.inputValidValue.value)

		area.setText("long enough")

		assertEquals("long enough", area.textValue.value)
		assertTrue(area.inputValidValue.value)
	}

	@Test
	fun `MetaInputLayout binds label helper and validation feedback for each presentation`() {
		val layout = MetaInputLayout.field(
			labelText = "Name",
			text = "Al",
			helperText = "Shown below",
			fontProvider = fontProvider,
		)
		layout.addValidator(object : MetaInputValidator() {
			override fun validateInput(input: String, errors: MetaErrorHandler) {
				if (input.length < 2) errors.add(MetaError("Need two characters", ""))
			}
		})

		assertEquals("Name", layout.label.text.toString())
		assertEquals("Shown below", layout.feedback.text.toString())
		assertTrue(layout.feedback.isVisible)

		layout.input.setText("")
		assertFalse(layout.inputValidValue.value)
		assertEquals("Need two characters", layout.feedback.text.toString())
		assertEquals(MetaColor.NEGATIVE, layout.feedback.color)

		layout.labelTextValue.value = "Renamed"
		assertEquals("Renamed", layout.label.text.toString())
	}
}
