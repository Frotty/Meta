package de.fatox.meta.test

import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import de.fatox.meta.Meta.Companion.inject
import org.junit.jupiter.api.BeforeEach

abstract class MetaTest {
	@BeforeEach
	fun prepare() {
		val config = HeadlessApplicationConfiguration()
		HeadlessApplication(TestApp(), config)
	}
}