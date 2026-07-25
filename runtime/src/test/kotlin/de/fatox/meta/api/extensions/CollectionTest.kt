package de.fatox.meta.api.extensions

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CollectionTest {
	@Test
	fun `array helper is reentrant over the same collection`() {
		val values = Array.with("a", "b", "c")
		var visits = 0

		values.forEachValue {
			values.forEachIndexedValue { index, value ->
				assertEquals(values[index], value)
				visits++
			}
		}

		assertEquals(9, visits)
	}

	@Test
	fun `map helper is reentrant over the same collection`() {
		val values = ObjectMap<String, Int>()
		values.put("one", 1)
		values.put("two", 2)
		var visits = 0

		values.forEachEntryReentrant { _, _ ->
			values.forEachEntryReentrant { _, _ -> visits++ }
		}

		assertEquals(4, visits)
	}
}
