package de.fatox.meta.test.injection

import de.fatox.meta.Meta.Companion.addModule
import de.fatox.meta.Meta.Companion.inject
import de.fatox.meta.injection.Inject
import de.fatox.meta.injection.Named
import de.fatox.meta.injection.Provides
import de.fatox.meta.injection.Singleton
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BasicTests {
	class NamedTestModule {
		@Provides
		@Named("someName")
		@Singleton
		fun someString(): String {
			return "yeah"
		}
	}

	@BeforeEach
	fun prepare() {
		addModule(NamedTestModule())
	}

	class NamedTestSample {
		@Inject
		@Named("someName")
		var s: String? = null

		init {
			inject(this)
		}
	}

	@Test
	fun testNamed() {
		val namedTestSample = NamedTestSample()
		Assertions.assertEquals("yeah", namedTestSample.s)
	}
}