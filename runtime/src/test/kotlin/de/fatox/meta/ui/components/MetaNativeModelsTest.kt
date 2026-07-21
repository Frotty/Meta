package de.fatox.meta.ui.components

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class MetaNativeModelsTest {
	@Test
	fun `integer spinner clamps values and steps without toolkit state`() {
		val model = MetaIntSpinnerModel(initial = 4, min = 0, max = 5, step = 2)

		model.increment()
		assertEquals(5, model.value)
		model.decrement()
		assertEquals(3, model.value)
		model.value = -20
		assertEquals(0, model.value)
	}

	@Test
	fun `float spinner parsing and formatting respect precision`() {
		val model = MetaFloatSpinnerModel(initial = 1f, min = 0f, max = 2f, step = 0.25f, precision = 2)

		assertEquals("1.00", model.format(model.value))
		model.value = model.parse("1.236")!!
		assertEquals("1.24", model.format(model.value))
		model.increment()
		assertEquals("1.49", model.format(model.value))
	}

	@Test
	fun `spinner width hints cover bounds and fractional digits`() {
		assertEquals("2048", MetaIntSpinnerModel(128, 128, 2048).widthHint())
		assertEquals("-10000", MetaIntSpinnerModel(0, -10000, 10000).widthHint())
		assertEquals("30.00", MetaFloatSpinnerModel(5f, 0.5f, 30f, 0.25f).widthHint())
	}

	@Test
	fun `file type filter accepts configured extensions case insensitively`() {
		val filter = MetaFileTypeFilter(false).apply { addRule("Levels", "xlv", ".clv") }

		assertTrue(filter.accepts(FileHandle("example.XLV")))
		assertTrue(filter.accepts(FileHandle("example.clv")))
		assertFalse(filter.accepts(FileHandle("example.txt")))
	}

	@Test
	fun `HSV picker conversion produces full intensity primary colors`() {
		val color = MetaHsv.toColor(0f, 1f, 1f, 1f, Color())

		assertEquals(1f, color.r)
		assertEquals(0f, color.g)
		assertEquals(0f, color.b)
		assertEquals(1f, color.a)
	}

	@Test
	fun `HSV picker conversion round trips an arbitrary color`() {
		val original = Color(0.24f, 0.71f, 0.43f, 0.8f)
		val hsv = MetaHsv.fromColor(original, FloatArray(3))
		val converted = MetaHsv.toColor(hsv[0], hsv[1], hsv[2], original.a, Color())

		assertEquals(original.r, converted.r, 0.0001f)
		assertEquals(original.g, converted.g, 0.0001f)
		assertEquals(original.b, converted.b, 0.0001f)
		assertEquals(original.a, converted.a, 0.0001f)
	}

	@Test
	fun `combined color input accepts hex and RGBA component lists`() {
		val color = Color()

		assertTrue(MetaColorCodec.parse("#FF800040", allowAlpha = true, color))
		assertEquals(1f, color.r)
		assertEquals(128f / 255f, color.g)
		assertEquals(0f, color.b)
		assertEquals(64f / 255f, color.a)

		assertTrue(MetaColorCodec.parse("12, 34, 56, 78", allowAlpha = true, color))
		assertEquals("#0C22384E", MetaColorCodec.format(color, includeAlpha = true))
	}

	@Test
	fun `combined color input accepts common CSS RGB formats`() {
		val color = Color()

		assertTrue(MetaColorCodec.parse("rgb(79, 157, 222)", allowAlpha = false, color))
		assertEquals("rgb(79, 157, 222)", MetaColorCodec.formatRgb(color, includeAlpha = false))

		assertTrue(MetaColorCodec.parse("rgba(79 157 222 / 50%)", allowAlpha = true, color))
		assertEquals(0.5f, color.a)
		assertEquals("rgba(79, 157, 222, 0.5)", MetaColorCodec.formatRgb(color, includeAlpha = true))
	}

	@Test
	fun `combined color input rejects out of range components`() {
		assertFalse(MetaColorCodec.parse("256, 0, 0", allowAlpha = false, Color()))
	}
}
