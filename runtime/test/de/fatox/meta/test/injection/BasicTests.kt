package de.fatox.meta.test.injection

import de.fatox.meta.Meta.Companion.addModule
import de.fatox.meta.Meta.Companion.inject
import de.fatox.meta.injection.*
import de.fatox.meta.injection.MetaInject.Companion.global
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BasicTests {
	class NamedTestModule {
		init {
		    global {
				singleton("yeah", "someName")
			}
		}
	}

	@BeforeEach
	fun prepare() {
		addModule(NamedTestModule())
	}

	class NamedTestSample {
		val s: String = MetaInject.inject("someName")
	}

	@Test
	fun testNamed() {
		val namedTestSample = NamedTestSample()
		Assertions.assertEquals("yeah", namedTestSample.s)
	}
}