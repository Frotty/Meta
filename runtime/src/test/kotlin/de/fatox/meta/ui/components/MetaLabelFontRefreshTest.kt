package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.test.MetaHeadlessUi
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.windows.MetaWindow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * A label on a stage the renderer does not own still draws from a live font after a scale change.
 *
 * The renderer's refresh walk covers its own stage only, then the old faces and the old atlas page are released. A
 * game that draws a menu on a `Stage` of its own had every label on it keep the released font and sample a deleted
 * texture, which draws each glyph as a solid black box. Whether it happened depended on whether a resize landed
 * after the menu was built, so it showed up on some launches and not others.
 */
class MetaLabelFontRefreshTest {
	@BeforeEach fun setUp() = MetaHeadlessUi.install()
	@AfterEach fun tearDown() = MetaHeadlessUi.dispose()

	@Test
	fun `a label on a foreign stage re-fetches its font on the first draw after a rebuild`() {
		val fonts = MetaInject.inject<FontProvider>("default")
		val stage = Stage(ScreenViewport())
		val label = MetaLabel("Play", 24, type = FontType.BOLD)
		stage.addActor(label)
		val before = fontOf(label)

		// What MetaUIRenderer.regenerateForPixelScale does, minus the walk, which never reaches this stage.
		MetaSkin.rebuildAtlas()
		fonts.disposeOrphanedFonts()
		// Guards the premise: attach-time healing cannot have run, so only draw-time healing can pass below.
		assertSame(before, fontOf(label), "The label refreshed before drawing; the test no longer reaches its case")

		stage.draw()

		val current = fonts.getFont(24, FontType.BOLD)
		assertNotSame(before, fontOf(label), "The label still draws from the font released by the rebuild")
		assertSame(current, fontOf(label), "The label did not pick up the provider's current font")
		stage.dispose()
	}

	@Test
	fun `a window on a foreign stage re-fetches its title font on the first draw after a rebuild`() {
		val fonts = MetaInject.inject<FontProvider>("default")
		val stage = Stage(ScreenViewport())
		val window = object : MetaWindow("Title") {}
		stage.addActor(window)
		val before = window.titleLabel.style.font

		MetaSkin.rebuildAtlas()
		fonts.disposeOrphanedFonts()
		assertSame(before, window.titleLabel.style.font, "The title refreshed before drawing; the test no longer reaches its case")

		stage.draw()

		assertNotSame(before, window.titleLabel.style.font, "The window title still draws from the font released by the rebuild")
		stage.dispose()
	}

	@Test
	fun `detached select boxes and window titles refresh before measuring`() {
		val fonts = MetaInject.inject<FontProvider>("default")
		val selectBox = MetaSelectBox<String>().apply { setItems("Play", "Settings") }
		val window = object : MetaWindow("Settings") {}
		val boxFont = selectBox.style.font
		val titleFont = window.titleLabel.style.font
		assertTrue(selectBox.prefWidth > 0f)
		assertTrue(window.prefWidth > 0f)

		MetaSkin.rebuildAtlas()
		fonts.disposeOrphanedFonts()
		assertSame(boxFont, selectBox.style.font)
		assertSame(titleFont, window.titleLabel.style.font)

		assertTrue(selectBox.prefWidth > 0f)
		assertTrue(window.prefWidth > 0f)
		assertNotSame(boxFont, selectBox.style.font)
		assertSame(selectBox.style.font, selectBox.list.style.font)
		assertNotSame(titleFont, window.titleLabel.style.font)
	}

	@Test
	fun `detached prompts remeasure before drawing after an atlas rebuild`() {
		val fonts = MetaInject.inject<FontProvider>("default")
		val prompt = MetaInputPrompt(emptyList(), "Play", 24)
		val width = prompt.prefWidth

		MetaSkin.rebuildAtlas()
		fonts.disposeOrphanedFonts()
		val current = fonts.getFont(24, FontType.REGULAR)
		current.data.setScale(current.scaleX * 2f, current.scaleY * 2f)
		assertTrue(prompt.prefWidth > width)
		prompt.setLabel("Следующая игра")
		assertTrue(prompt.prefWidth > width)
	}

	@Test
	fun `a detached label can be measured in Russian before its first draw after an atlas rebuild`() {
		val fonts = MetaInject.inject<FontProvider>("default")
		val label = MetaLabel("Play", 24, type = FontType.BOLD)
		val before = fontOf(label)

		MetaSkin.rebuildAtlas()
		fonts.disposeOrphanedFonts()
		assertSame(before, fontOf(label))

		label.setText("Новая игра")
		assertTrue(label.prefWidth > 0f)
		assertSame(fonts.getFont(24, FontType.BOLD), fontOf(label))
	}

	@Test
	fun `layout refreshes the font before building a glyph cache`() {
		val fonts = MetaInject.inject<FontProvider>("default")
		val label = MetaLabel("Continue", 24)
		label.setSize(240f, 36f)
		val before = fontOf(label)

		MetaSkin.rebuildAtlas()
		fonts.disposeOrphanedFonts()
		assertSame(before, fontOf(label))

		label.setText("Продолжить")
		label.layout()
		assertTrue(label.glyphLayout.width > 0f)
		assertSame(fonts.getFont(24, FontType.REGULAR), fontOf(label))
	}

	private fun fontOf(label: MetaLabel): BitmapFont {
		val field = MetaLabel::class.java.getDeclaredField("font")
		field.isAccessible = true
		return field.get(label) as BitmapFont
	}
}
