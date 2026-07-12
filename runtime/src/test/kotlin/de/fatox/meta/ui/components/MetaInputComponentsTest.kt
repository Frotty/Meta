package de.fatox.meta.ui.components

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.viewport.Viewport
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisTextField
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.extensions.onChange
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.api.lang.AvailableLanguages
import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.error.MetaError
import de.fatox.meta.error.MetaErrorHandler
import de.fatox.meta.injection.MetaInject.Companion.global
import de.fatox.meta.test.GdxTestEnvironment
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.refreshFontsRecursively
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.lang.reflect.Proxy
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

	@Test
	fun `MetaInputLayout collapses an empty feedback row`() {
		val layout = MetaInputLayout.field(labelText = "Name", fontProvider = fontProvider)
		layout.pack()
		val withoutFeedback = layout.prefHeight

		layout.setHelperText("Helpful context")
		layout.pack()

		assertTrue(layout.feedback.isVisible)
		assertTrue(layout.prefHeight > withoutFeedback)

		layout.setHelperText("")
		layout.pack()
		assertFalse(layout.feedback.isVisible)
		assertEquals(withoutFeedback, layout.prefHeight)
	}

	@Test
	fun `MetaIconTextButton is horizontal by default and supports explicit tiles`() {
		val skin = VisUI.getSkin()
		skin.add(MetaSkin.BUTTON, Button.ButtonStyle())
		skin.add("meta.button.focus", BaseDrawable(), Drawable::class.java)
		skin.add("meta.button.focusOver", BaseDrawable(), Drawable::class.java)

		val action = MetaIconTextButton("Open", BaseDrawable())
		val tile = MetaIconTextButton("Open", BaseDrawable(), vertical = true)

		assertEquals(0, action.rows)
		assertEquals(2, action.cells.size)
		assertEquals(1, tile.rows)
	}

	@Test
	fun `refreshFontsRecursively re-fetches fonts into nested Meta widgets after a rebuild`() {
		val secondFont = testFont()
		var currentFont = font
		val switchingProvider = object : FontProvider {
			override fun getFont(size: Int, type: FontType): BitmapFont = currentFont
			override fun write(x: Float, y: Float, text: String, size: Int, type: FontType) = Unit
		}
		val field = MetaInputField("text", fontProvider = switchingProvider)
		val root = Group().apply { addActor(Table().apply { add(field) }) }
		assertSame(font, field.style.font)

		// Simulate a UI-scale change: the provider now hands out a re-rasterized font instance.
		currentFont = secondFont
		root.refreshFontsRecursively()

		assertSame(secondFont, field.style.font)
		assertSame(secondFont, (field.style as VisTextField.VisTextFieldStyle).messageFont)
		// The invalid-state style clone must be refreshed too, or invalid fields would draw a disposed font.
		field.setInputValid(false)
		assertSame(secondFont, field.style.font)
	}

	@Test
	fun `re-attached widget self-heals fonts rebuilt while it was detached`() {
		val secondFont = testFont()
		var currentFont = font
		var generation = 0
		val rebuildingProvider = object : FontProvider {
			override fun getFont(size: Int, type: FontType): BitmapFont = currentFont
			override fun write(x: Float, y: Float, text: String, size: Int, type: FontType) = Unit
			override val fontGeneration: Int get() = generation
		}
		// The stale-check tracker injects the global provider, so the generation-bumping stub must be registered there.
		global(clear = true) { singleton<FontProvider> { rebuildingProvider } }

		val field = MetaInputField("text", fontProvider = rebuildingProvider)
		val tabContent = Table().apply { add(field) }
		val stage = Stage(TestViewport(), noopBatch())
		stage.addActor(tabContent)
		assertSame(font, field.style.font)

		// Simulate a UI-scale change while the "tab content" is detached: the provider rebuilds its fonts and the
		// renderer's refresh walk never reaches this subtree before the old generation is disposed.
		tabContent.remove()
		currentFont = secondFont
		generation++

		stage.addActor(tabContent)
		assertSame(secondFont, field.style.font)
	}

	@Test
	fun `MetaCheckBox external signal write updates isChecked and check glyph without consumer events`() {
		installCheckboxSkin()
		val checkBox = MetaCheckBox(stubAssetProvider(), initialChecked = false)
		var consumerChangeEvents = 0
		checkBox.onChange { consumerChangeEvents++ }

		assertFalse(checkBox.isChecked)
		assertEquals(0, checkBox.children.size)

		checkBox.checkedValue.value = true
		assertTrue(checkBox.isChecked)
		assertEquals(1, checkBox.children.size)

		checkBox.checkedValue.value = false
		assertFalse(checkBox.isChecked)
		assertEquals(0, checkBox.children.size)

		// Signal writes are programmatic state sync; they must not fire consumer ChangeEvents.
		assertEquals(0, consumerChangeEvents)
	}

	@Test
	fun `MetaCheckBox click round-trips into the signal and glyph`() {
		installCheckboxSkin()
		val checkBox = MetaCheckBox(stubAssetProvider(), initialChecked = false)

		// Emulate the user-click path: Button's ClickListener sets the checked state and fires a ChangeEvent.
		checkBox.setProgrammaticChangeEvents(true)
		checkBox.isChecked = true

		assertTrue(checkBox.checkedValue.value)
		assertEquals(1, checkBox.children.size)

		checkBox.isChecked = false
		assertFalse(checkBox.checkedValue.value)
		assertEquals(0, checkBox.children.size)
	}

	@Test
	fun `MetaCheckBox check glyph receives its optical vertical centering offset`() {
		installCheckboxSkin()
		val checkBox = MetaCheckBox(stubAssetProvider(), initialChecked = true)
		checkBox.setSize(30f, 30f)
		checkBox.validate()

		val checkIcon = checkBox.children.first()
		assertEquals((30f - checkIcon.height) * 0.5f - 3f, checkIcon.y, 0.001f)
	}

	private fun installCheckboxSkin() {
		val skin = VisUI.getSkin()
		skin.add("meta.checkbox.focus", BaseDrawable(), Drawable::class.java)
		skin.add("meta.checkbox.onFocus", BaseDrawable(), Drawable::class.java)
		skin.add(MetaSkin.CHECKBOX, Button.ButtonStyle())
	}

	private fun noopBatch(): Batch {
		val projection = Matrix4()
		val transform = Matrix4()
		return Proxy.newProxyInstance(
			Batch::class.java.classLoader,
			arrayOf(Batch::class.java),
		) { _, method, _ ->
			when (method.name) {
				"getProjectionMatrix" -> projection
				"getTransformMatrix" -> transform
				"isBlendingEnabled" -> true
				"isDrawing" -> false
				"getBlendSrcFunc", "getBlendDstFunc", "getBlendSrcFuncAlpha", "getBlendDstFuncAlpha" -> 0
				else -> null
			}
		} as Batch
	}

	private class TestViewport : Viewport() {
		init {
			camera = OrthographicCamera()
			setWorldSize(800f, 600f)
		}

		override fun update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean) {
			setScreenBounds(0, 0, screenWidth, screenHeight)
			if (centerCamera) camera.position.set(worldWidth * 0.5f, worldHeight * 0.5f, 0f)
			camera.update()
		}
	}

	private fun stubAssetProvider(): AssetProvider = object : AssetProvider {
		override fun loadPackedAssetsFromFolder(folder: FileHandle): Boolean = false
		override fun loadRawAssetsFromFolder(folder: FileHandle): Boolean = false
		override fun <T : Any> load(name: String, type: Class<T>) = Unit
		override fun <T : Any> getResource(fileName: String, type: Class<T>, index: Int): T =
			error("Not used in tests")

		override fun getDrawable(name: String): Drawable = BaseDrawable()
		override fun finish() = Unit
		override fun loadAnimationFrames(baseName: String, frames: Int): Array<out TextureRegion> = Array()
	}
}
