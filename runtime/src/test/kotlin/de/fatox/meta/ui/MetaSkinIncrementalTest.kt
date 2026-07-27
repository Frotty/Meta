package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import de.fatox.meta.test.GdxTestEnvironment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetaSkinIncrementalTest {
	@AfterEach
	fun cleanUp() = MetaSkin.dispose()

	@Test
	fun `incremental initialization queues generated resources without touching GL`() {
		MetaSkin.beginIncrementalInitialize(Skin())
		assertTrue(MetaSkin.pendingInstallSteps > 50)

		val pending = MetaSkin.pendingInstallSteps
		assertEquals(false, MetaSkin.updateIncrementalInitialize(0))
		assertEquals(pending, MetaSkin.pendingInstallSteps)
	}

	companion object {
		@JvmStatic
		@BeforeAll
		fun initializeGdx() = GdxTestEnvironment.ensure()
	}
}
