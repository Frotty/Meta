package de.fatox.meta.injection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

internal class MetaInjectDefaultTest {
	@Test
	fun `default singleton does not replace application singleton`() {
		val inject = MetaInject()
		val applicationValue = Any()

		inject.singleton { applicationValue }
		inject.singleton("default") { Any() }

		assertSame(applicationValue, inject.inject())
	}

	@Test
	fun `default eager singleton does not replace application singleton`() {
		val inject = MetaInject()

		inject.singleton { "application" }
		inject.singleton("framework", "default")

		assertEquals("application", inject.inject())
	}

	@Test
	fun `default singleton supplies unnamed binding when application omits it`() {
		val inject = MetaInject()

		inject.singleton("default") { "framework" }

		assertEquals("framework", inject.inject<String>())
		assertEquals("framework", inject.inject<String>("default"))
	}

	@Test
	fun `default provider does not replace application provider`() {
		val inject = MetaInject()

		inject.provider { "application" }
		inject.provider("default") { "framework" }

		assertEquals("application", inject.inject<String>())
	}
}
