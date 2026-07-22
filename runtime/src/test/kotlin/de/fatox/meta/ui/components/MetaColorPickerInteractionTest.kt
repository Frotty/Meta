package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import de.fatox.meta.test.GdxTestEnvironment
import de.fatox.meta.ui.MetaSkin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class MetaColorPickerInteractionTest {
	@BeforeTest
	fun setUp() {
		GdxTestEnvironment.ensure()
		MetaSkin.dispose()
		MetaSkin.initialize(Skin().apply {
			add(MetaSkin.COLOR_FILL, BaseDrawable(), Drawable::class.java)
		}, installDefaults = false)
	}

	@AfterTest
	fun tearDown() {
		MetaSkin.dispose()
	}

	@Test
	fun `hue strip and its visual gap are one continuous hit zone`() {
		val width = 360f
		val squareEdge = width - MetaColorField.HUE_GAP - MetaColorField.HUE_WIDTH

		assertEquals(ColorFieldRegion.SATURATION_VALUE, MetaColorField.regionAt(squareEdge, width))
		assertEquals(ColorFieldRegion.HUE, MetaColorField.regionAt(squareEdge + 1f, width))
		assertEquals(ColorFieldRegion.HUE, MetaColorField.regionAt(width, width))
		assertEquals(ColorFieldRegion.OUTSIDE, MetaColorField.regionAt(width + 1f, width))
	}

	@Test
	fun `hue press is consumed and publishes a color change`() {
		var changes = 0
		val field = MetaColorField { _, _, _ -> changes++ }.apply { setSize(360f, 210f) }
		val event = InputEvent()
		val listener = field.listeners[field.listeners.size - 1] as InputListener

		val handled = listener.touchDown(event, 359f, 105f, 0, 0)

		assertEquals(1, changes)
		assertTrue(handled)
		assertTrue(event.isStopped)
		field.dispose()
	}

	@Test
	fun `color swatch always exposes its selected color behind a border`() {
		val selected = Color.valueOf("5AC8FAFF")
		val swatch = MetaColorSwatch(selected)

		assertEquals(selected, swatch.selectedColor)
		assertNotNull(swatch.background)
	}
}
