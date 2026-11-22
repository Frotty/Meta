package de.fatox.meta.ui.components

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import de.fatox.meta.reactive.effect
import de.fatox.meta.reactive.signal
import de.fatox.meta.test.MetaHeadlessUi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MetaMenuShortcutTest {
	@BeforeEach fun setUp() = MetaHeadlessUi.install()
	@AfterEach fun tearDown() = MetaHeadlessUi.dispose()

	@Test
	fun `rebound shortcuts repack the open popup and clear without leaving a gap`() {
		val stage = Stage(ScreenViewport())
		stage.viewport.update(640, 480, true)
		val item = MetaMenuItem("Save")
		val menu = MetaMenu("File").apply { addItem(item) }
		val binding = signal(emptyList<MetaInputGlyph>())
		val displayBinding = effect { item.shortcutGlyphs = binding.value }
		try {
			menu.showMenu(stage, stage.width - 1f, stage.height)
			val originalWidth = menu.width
			val originalX = menu.x
			val originalChildren = item.children.size
			val chord = listOf(
				MetaInputGlyphs.key(Input.Keys.CONTROL_LEFT, "Ctrl"),
				MetaInputGlyphs.key(Input.Keys.S, "S"),
			)

			binding.value = chord
			val prompt = item.children.peek() as MetaInputPrompt
			assertEquals(chord, item.shortcutGlyphs)
			assertTrue(menu.width > originalWidth)
			assertTrue(menu.x < originalX)
			assertEquals(stage.width, menu.x + menu.width, 0.01f)
			assertTrue(prompt.x + prompt.width <= item.width)
			assertEquals(Touchable.disabled, prompt.touchable)
			assertSame(item, item.hit(prompt.x + prompt.width / 2f, prompt.y + prompt.height / 2f, true))

			item.isDisabled = true
			binding.value = listOf(MetaInputGlyphs.face(MetaPadFamily.XBOX, MetaFaceButton.SOUTH))
			assertSame(prompt, item.children.peek())
			assertTrue(prompt.color.a < 1f)
			item.isDisabled = false
			assertEquals(1f, prompt.color.a)

			binding.value = emptyList()
			assertEquals(originalChildren, item.children.size)
			assertEquals(originalWidth, menu.width, 0.01f)
			assertEquals(originalX, menu.x, 0.01f)
			assertTrue(menu.openValue.peek())
		} finally {
			displayBinding.dispose()
			menu.remove()
			stage.dispose()
		}
	}
}
