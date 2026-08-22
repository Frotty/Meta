package de.fatox.meta.test

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration

/**
 * Boots a single, JVM-wide headless libGDX application so unit tests can exercise scene2d layout
 * (`Table`/`Cell` need `Gdx.files` to be non-null, otherwise `Cell.defaults()` recurses into a StackOverflow).
 *
 * Call [ensure] from `@BeforeEach`/`@BeforeAll` in any UI-layout test. It is idempotent and starts no render loop
 * (`updatesPerSecond = -1`). There is NO GL context, so this enables layout math only - never actual drawing or
 * texture/font rasterization.
 */
object GdxTestEnvironment {
	@Volatile
	private var started = false

	fun ensure() {
		if (started) return
		synchronized(this) {
			if (started) return
			val config = HeadlessApplicationConfiguration().apply { updatesPerSecond = -1 }
			HeadlessApplication(object : ApplicationAdapter() {}, config)
			started = true
		}
	}
}
