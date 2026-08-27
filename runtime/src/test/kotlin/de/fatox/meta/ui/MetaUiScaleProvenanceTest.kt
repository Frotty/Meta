package de.fatox.meta.ui

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import de.fatox.meta.api.ui.FocusRenderer
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.test.MetaHeadlessUi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Whether a scale write came from the renderer or from someone else.
 *
 * The distinction only matters in the case that leaves no other trace: a player committing the scale that is
 * already in effect. A signal suppresses a write equal to its current value, so nothing is notified, nothing
 * changes, and the only evidence the choice was made is that it was recorded at the write. Getting this wrong is
 * silent until a window is moved to another monitor and the choice is overwritten.
 */
class MetaUiScaleProvenanceTest {
	private lateinit var renderer: MetaUIRenderer

	@BeforeEach
	fun setUp() {
		MetaHeadlessUi.install()
		// MetaUIRenderer builds a real Stage, so it needs the two services the layout-only harness does not
		// register. The GL stub reports every upload as succeeding, which is enough for a batch and a stage.
		MetaInject.global {
			singleton("default") { SpriteBatch() }
			singleton<FocusRenderer> { DefaultFocusRenderer() }
		}
		renderer = MetaUIRenderer()
	}

	@AfterEach
	fun tearDown() = MetaHeadlessUi.dispose()

	@Test
	fun `a fresh renderer has not had its scale chosen for it`() {
		assertFalse(renderer.scaleChosenByUser)
	}

	@Test
	fun `committing the value already in effect counts as a choice`() {
		val current = renderer.uiScale.peek()

		renderer.uiScale.value = current

		// Nothing observable changed, which is exactly why recording at the write rather than on change matters.
		assertEquals(current, renderer.uiScale.peek())
		assertTrue(renderer.scaleChosenByUser) {
			"An equal-value write was not recorded, so a monitor change would overwrite a deliberate choice"
		}
	}

	@Test
	fun `a different value also counts as a choice`() {
		renderer.uiScale.value = renderer.uiScale.peek() + 0.5f
		assertTrue(renderer.scaleChosenByUser)
	}
}
