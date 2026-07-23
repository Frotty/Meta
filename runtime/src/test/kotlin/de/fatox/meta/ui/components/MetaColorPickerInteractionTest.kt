package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.viewport.Viewport
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject.Companion.global
import de.fatox.meta.test.GdxTestEnvironment
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.layout.MetaLayout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import java.lang.reflect.Proxy

internal class MetaColorPickerInteractionTest {
	private lateinit var font: BitmapFont

	@BeforeTest
	fun setUp() {
		GdxTestEnvironment.ensure()
		MetaSkin.dispose()
		font = testFont()
		MetaSkin.initialize(Skin().apply {
			add(MetaSkin.COLOR_FILL, BaseDrawable(), Drawable::class.java)
			add(MetaSkin.SEPARATOR, BaseDrawable(), Drawable::class.java)
			val sliderStyle = Slider.SliderStyle(BaseDrawable(), BaseDrawable())
			add(MetaSkin.SLIDER_HORIZONTAL, sliderStyle, Slider.SliderStyle::class.java)
			add("default-horizontal", Slider.SliderStyle(sliderStyle), Slider.SliderStyle::class.java)
			val windowStyle = Window.WindowStyle(font, Color.WHITE, BaseDrawable())
			add(MetaSkin.WINDOW, windowStyle, Window.WindowStyle::class.java)
			add(MetaSkin.WINDOW_RESIZABLE, Window.WindowStyle(windowStyle), Window.WindowStyle::class.java)
			add(MetaSkin.SCROLL_PANE, ScrollPane.ScrollPaneStyle(), ScrollPane.ScrollPaneStyle::class.java)
			add(MetaSkin.SCROLL_PANE_FLAT, ScrollPane.ScrollPaneStyle(), ScrollPane.ScrollPaneStyle::class.java)
			add(MetaSkin.BUTTON_PRIMARY, Button.ButtonStyle())
			add(MetaSkin.BUTTON_SECONDARY, Button.ButtonStyle())
			add(MetaSkin.BUTTON_TERTIARY, Button.ButtonStyle())
			add(MetaSkin.ICON_BUTTON, Button.ButtonStyle())
			add("meta.button.focus", BaseDrawable(), Drawable::class.java)
			add("meta.button.focusOver", BaseDrawable(), Drawable::class.java)
			add("meta.field.focus", BaseDrawable(), Drawable::class.java)
			add("meta.tooltip", BaseDrawable(), Drawable::class.java)
			val textFieldStyle = TextField.TextFieldStyle().apply {
				this.font = this@MetaColorPickerInteractionTest.font
				fontColor = Color.WHITE
				messageFontColor = Color.GRAY
			}
			add(MetaSkin.TEXT_FIELD, textFieldStyle)
			add(MetaSkin.TEXT_FIELD_ERROR, TextField.TextFieldStyle(textFieldStyle))
		}, installDefaults = false)
		global(clear = true) {
			singleton<FontProvider> {
				object : FontProvider {
					override fun getFont(size: Int, type: FontType): BitmapFont = font
					override fun write(x: Float, y: Float, text: String, size: Int, type: FontType) = Unit
				}
			}
		}
	}

	@AfterTest
	fun tearDown() {
		MetaSkin.dispose()
		font.dispose()
		global(clear = true) {}
	}

	@Test
	fun `hue control owns its complete visible hit zone`() {
		val field = MetaHueField(MetaColorState()) {}.apply {
			setBounds(40f, 30f, 28f, 210f)
		}
		assertSame(field, field.hit(0f, 0f, true))
		assertSame(field, field.hit(field.width - 0.01f, field.height - 0.01f, true))
		assertNull(field.hit(field.width + 1f, field.height * 0.5f, true))
		field.dispose()
	}

	@Test
	fun `hue marker uses transformed bounds at non-default UI scale`() {
		assertEquals(350f, MetaHueField.markerY(bottom = 100f, top = 600f, hue = 180f), 0.001f)
		assertEquals(225f, MetaHueField.markerY(bottom = 100f, top = 600f, hue = 90f), 0.001f)
	}

	@Test
	fun `stage routes hue presses and the complete drag gesture to the field`() {
		val hues = mutableListOf<Float>()
		val state = MetaColorState()
		val field = MetaHueField(state) { hues += state.hue.peek() }.apply {
			setBounds(40f, 30f, 28f, 210f)
		}
		val stage = Stage(TestViewport(), noopBatch()).apply {
			viewport.update(800, 600, true)
			addActor(field)
		}

		assertTrue(stage.press(field, 14f, 20f))
		assertTrue(stage.drag(field, 100f, 105f))
		assertTrue(stage.drag(field, -50f, 200f))
		stage.release(field, -50f, 200f)

		assertEquals(3, hues.size)
		assertEquals(20f / 210f * 360f, hues[0], 0.01f)
		assertEquals(105f / 210f * 360f, hues[1], 0.01f)
		assertEquals(200f / 210f * 360f, hues[2], 0.01f)
		stage.dispose()
	}

	@Test
	fun `real picker layout routes hue strip input to the color field and live previews`() {
		val picker = MetaColorPicker(isAllowAlphaEdit = true).apply {
			setBounds(100f, 20f, 500f, 570f)
		}
		val stage = Stage(TestViewport(), noopBatch()).apply {
			viewport.update(800, 600, true)
			addActor(picker)
		}
		picker.validate()
		picker.hueField.validate()
		val oldHue = picker.state.hue.peek()
		val localPress = Vector2(picker.hueField.width * 0.5f, picker.hueField.height * 0.6f)
		val stagePress = picker.hueField.localToStageCoordinates(localPress.cpy())
		val target = stage.hit(stagePress.x, stagePress.y, true)
		assertSame(picker.hueField, target)
		val event = InputEvent().apply {
			type = InputEvent.Type.touchDown
			stageX = stagePress.x
			stageY = stagePress.y
			pointer = 0
			button = 0
			this.stage = stage
		}
		target.fire(event)

		assertTrue(event.isHandled)
		assertTrue(picker.state.hue.peek() != oldHue)
		assertEquals(216f, picker.state.hue.peek(), 0.01f)
		assertEquals(picker.colorValue.value, picker.preview.selectedColor)
		assertTrue(picker.height < 520f)
		assertTrue(picker.height >= picker.prefHeight)
		stage.dispose()
		picker.dispose()
	}

	@Test
	fun `stage routes saturation value presses and keeps that region for the drag`() {
		data class Selection(val hue: Float, val saturation: Float, val value: Float)
		val selections = mutableListOf<Selection>()
		val state = MetaColorState()
		val field = MetaColorField(state) {
			selections += Selection(state.hue.peek(), state.saturation.peek(), state.value.peek())
		}.apply {
			setBounds(40f, 30f, 360f, 210f)
		}
		val stage = Stage(TestViewport(), noopBatch()).apply {
			viewport.update(800, 600, true)
			addActor(field)
		}

		assertTrue(stage.press(field, 100f, 50f))
		assertTrue(stage.drag(field, 400f, 200f))
		stage.release(field, 400f, 200f)

		assertEquals(2, selections.size)
		assertEquals(100f / 360f, selections[0].saturation, 0.01f)
		assertEquals(50f / 210f, selections[0].value, 0.01f)
		assertEquals(1f, selections[1].saturation, 0.01f)
		assertEquals(200f / 210f, selections[1].value, 0.01f)
		assertEquals(0f, selections[1].hue, 0.01f)
		stage.dispose()
	}

	@Test
	fun `color swatch always exposes its selected color behind a border`() {
		val selected = Color.valueOf("5AC8FAFF")
		val swatch = MetaColorSwatch(selected)

		assertEquals(selected, swatch.selectedColor)
		assertNotNull(swatch.background)
	}

	@Test
	fun `reactive color state drives brightness and alpha without losing hue or saturation`() {
		val source = Color.valueOf("4A90E2CC")
		val state = MetaColorState()
		val result = Color()
		state.setColor(source, allowAlpha = true)

		state.value.value = 0.35f
		state.alpha.value = 0.4f
		state.toColor(result)

		val converted = FloatArray(3)
		MetaHsv.fromColor(result, converted)
		assertEquals(0.35f, converted[2], 0.001f)
		assertEquals(0.4f, result.a, 0.001f)
	}

	@Test
	fun `picker preserves hue through black and updates both published and visual previews`() {
		val picker = MetaColorPicker(isAllowAlphaEdit = true)
		picker.selectedColor = Color.valueOf("38B54AFF")
		val originalHue = picker.state.hue.peek()

		picker.brightnessSlider.valueValue.value = 0f
		assertEquals(originalHue, picker.state.hue.peek(), 0.001f)
		assertEquals(Color.BLACK.r, picker.colorValue.value.r, 0.001f)
		assertEquals(Color.BLACK.g, picker.colorValue.value.g, 0.001f)
		assertEquals(Color.BLACK.b, picker.colorValue.value.b, 0.001f)
		assertEquals(picker.colorValue.value, picker.preview.selectedColor)

		picker.brightnessSlider.valueValue.value = 1f
		assertEquals(originalHue, picker.state.hue.peek(), 0.001f)
		assertTrue(picker.colorValue.value.g > picker.colorValue.value.r)
		assertEquals(picker.colorValue.value, picker.preview.selectedColor)
		picker.dispose()
	}

	@Test
	fun `picker alpha changes update the published and checkerboard preview color`() {
		val picker = MetaColorPicker(isAllowAlphaEdit = true)
		picker.selectedColor = Color.valueOf("4F9DDEFF")

		picker.alphaSlider.valueValue.value = 0.25f

		assertEquals(0.25f, picker.colorValue.value.a, 0.001f)
		assertEquals(picker.colorValue.value, picker.preview.selectedColor)
		assertTrue(picker.preview.children.size > 0)
		picker.dispose()
	}

	@Test
	fun `color ramp slider keeps Meta slider behavior over an unobscured ramp`() {
		val control = MetaColorRampSlider(0f, 1f, 0.01f) { position, out ->
			out.set(position, 0f, 1f - position, 1f)
		}

		control.valueValue.value = 0.72f
		control.setSize(300f, 36f)
		control.validate()

		assertEquals(0.72f, control.slider.value, 0.001f)
		assertNotNull(control.slider.style.background)
		control.slider.setMetaFocused(true)
		assertNotNull(control.slider.style.background)
		assertNull(control.slider.style.knobBefore)
		assertNull(control.slider.style.knobAfter)
		assertEquals(2, control.children.size)
		MetaLayout.assertValid(control)
		control.dispose()
	}

	@Test
	fun `stage can press and drag a color ramp slider without a painted track or crash`() {
		val control = MetaColorRampSlider(0f, 1f, 0.01f) { position, out ->
			out.set(position, 0f, 1f - position, 1f)
		}.apply {
			setBounds(40f, 30f, 300f, 36f)
		}
		val stage = Stage(TestViewport(), noopBatch()).apply {
			viewport.update(800, 600, true)
			addActor(control)
		}
		control.validate()

		val press = control.slider.localToStageCoordinates(Vector2(75f, 18f))
		val target = stage.hit(press.x, press.y, true)
		assertSame(control.slider, target)
		val pressEvent = InputEvent().apply {
			type = InputEvent.Type.touchDown
			stageX = press.x
			stageY = press.y
			pointer = 0
			button = 0
			this.stage = stage
		}
		target.fire(pressEvent)
		assertTrue(pressEvent.isHandled)

		val drag = control.slider.localToStageCoordinates(Vector2(240f, 18f))
		assertTrue(stage.touchDragged(drag.x.toInt(), (stage.viewport.screenHeight - drag.y).toInt(), 0))
		assertTrue(stage.touchUp(drag.x.toInt(), (stage.viewport.screenHeight - drag.y).toInt(), 0, 0))
		assertTrue(control.valueValue.value > 0.7f)

		stage.dispose()
		control.dispose()
	}

	private fun Stage.press(field: Actor, x: Float, y: Float): Boolean {
		val stagePosition = field.localToStageCoordinates(Vector2(x, y))
		val target = hit(stagePosition.x, stagePosition.y, true)
		assertSame(field, target)
		val event = InputEvent().apply {
			type = InputEvent.Type.touchDown
			stageX = stagePosition.x
			stageY = stagePosition.y
			pointer = 0
			button = 0
			stage = this@press
		}
		target.fire(event)
		return event.isHandled
	}

	private fun Stage.drag(field: Actor, x: Float, y: Float): Boolean {
		val screen = field.toScreen(x, y)
		return touchDragged(screen.x.toInt(), screen.y.toInt(), 0)
	}

	private fun Stage.release(field: Actor, x: Float, y: Float): Boolean {
		val screen = field.toScreen(x, y)
		return touchUp(screen.x.toInt(), screen.y.toInt(), 0, 0)
	}

	private fun Actor.toScreen(x: Float, y: Float): Vector2 =
		localToStageCoordinates(Vector2(x, y)).apply { this.y = stage.viewport.screenHeight - this.y }

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

	private fun testFont(): BitmapFont {
		val data = BitmapFont.BitmapFontData().apply {
			lineHeight = 12f
			capHeight = 9f
			ascent = 9f
			descent = -4f
			spaceXadvance = 5f
			xHeight = 7f
		}
		return BitmapFont(data, TextureRegion(), true)
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

		override fun unproject(screenCoords: Vector2): Vector2 {
			screenCoords.x -= screenX
			screenCoords.y = screenHeight - screenCoords.y
			return screenCoords
		}
	}
}
