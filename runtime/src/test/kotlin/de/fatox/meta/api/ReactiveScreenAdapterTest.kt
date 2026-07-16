package de.fatox.meta.api

import de.fatox.meta.reactive.signal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class ReactiveScreenAdapterTest {
	@Test
	fun `screen bindings are disposed on hide and recreated on show`() {
		val source = signal(0)
		val screen = TestScreen(source)

		screen.show()
		assertEquals(listOf(0), screen.observed)
		source.value = 1
		assertEquals(listOf(0, 1), screen.observed)

		screen.hide()
		source.value = 2
		assertEquals(listOf(0, 1), screen.observed)

		screen.show()
		assertEquals(listOf(0, 1, 2), screen.observed)
		source.value = 3
		assertEquals(listOf(0, 1, 2, 3), screen.observed)
	}

	@Test
	fun `dispose tears down the active presentation exactly once`() {
		val screen = TestScreen(signal(0))
		screen.show()

		screen.dispose()
		screen.dispose()

		assertEquals(1, screen.hiddenCount)
		assertEquals(1, screen.disposedCount)
		assertTrue(screen.scopeDisposed)
		assertFailsWith<IllegalStateException> { screen.show() }
	}

	private class TestScreen(private val source: de.fatox.meta.reactive.Signal<Int>) : ReactiveScreenAdapter() {
		val observed = mutableListOf<Int>()
		var hiddenCount = 0
		var disposedCount = 0
		val scopeDisposed: Boolean get() = reactiveScope.isDisposed

		override fun onShown() {
			reactiveScope.effect { observed.add(source()) }
		}

		override fun onHidden() {
			hiddenCount++
		}

		override fun onDisposed() {
			disposedCount++
		}
	}
}
