package de.fatox.meta.input

import com.badlogic.gdx.Input
import de.fatox.meta.test.GdxTestEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MetaInputTest {
	private lateinit var input: MetaInput

	@BeforeTest
	fun setUp() {
		GdxTestEnvironment.ensure()
		input = MetaInput()
	}

	@Test
	fun `key listener removal during release does not invalidate dispatch`() {
		val events = ArrayList<String>()
		lateinit var first: KeyListener
		first = listener("first", events) {
			input.removeScreenKeyListener(Input.Keys.ESCAPE, first)
		}
		input.addScreenKeyListener(Input.Keys.ESCAPE, 0L, first)
		input.addScreenKeyListener(Input.Keys.ESCAPE, 0L, listener("second", events))
		input.addScreenKeyListener(Input.Keys.ESCAPE, 0L, listener("third", events))

		input.keyDown(Input.Keys.ESCAPE)
		input.keyUp(Input.Keys.ESCAPE)

		assertEquals(listOf("first", "second", "third"), events)
	}

	private fun listener(name: String, events: MutableList<String>, beforeRecord: () -> Unit = {}) =
		object : KeyListener() {
			override fun onEvent() {
				beforeRecord()
				events.add(name)
			}
		}
}
