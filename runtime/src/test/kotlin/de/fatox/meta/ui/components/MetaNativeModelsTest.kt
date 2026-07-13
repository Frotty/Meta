package de.fatox.meta.ui.components

import com.badlogic.gdx.files.FileHandle
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

		model.value = model.parse("1.236")!!
		assertEquals("1.24", model.format(model.value))
		model.increment()
		assertEquals("1.49", model.format(model.value))
	}

	@Test
	fun `file type filter accepts configured extensions case insensitively`() {
		val filter = MetaFileTypeFilter(false).apply { addRule("Levels", "xlv", ".clv") }

		assertTrue(filter.accepts(FileHandle("example.XLV")))
		assertTrue(filter.accepts(FileHandle("example.clv")))
		assertFalse(filter.accepts(FileHandle("example.txt")))
	}
}
