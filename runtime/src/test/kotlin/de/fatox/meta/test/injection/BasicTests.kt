package de.fatox.meta.test.injection

import de.fatox.meta.injection.MetaInject.Companion.global
import de.fatox.meta.injection.MetaInject.Companion.inject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertSame

private const val SINGLETON_VALUE = "singleton_value"
private const val SINGLETON_NAME = "singleton_name"

internal class BasicTests {

	@BeforeEach
	fun prepare() {
		global {
			singleton(SINGLETON_VALUE, SINGLETON_NAME)
		}
	}

	@AfterEach
	fun tearDown() {
		global(true) {}
	}

	@Test
	fun `value === injected named singleton value`() {
		assertSame(SINGLETON_VALUE, inject(SINGLETON_NAME))
	}

	@Test
	fun `injected named singleton value === injected named singleton value`() {
		assertSame(inject<String>(SINGLETON_NAME), inject(SINGLETON_NAME))
	}

	@Test
	fun `adding the same type with same name to the same context throws exception`() {
		assertThrows<IllegalStateException> {
			global {
				singleton(SINGLETON_VALUE, SINGLETON_NAME)
			}
		}
	}

	@Test
	fun `adding the same type with no name to the same context is successful`() {
		global {
			singleton(SINGLETON_VALUE)
		}
	}
}