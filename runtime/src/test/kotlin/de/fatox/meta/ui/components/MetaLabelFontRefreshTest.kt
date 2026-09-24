package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.test.MetaHeadlessUi
import de.fatox.meta.ui.MetaSkin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
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

	private fun fontOf(label: MetaLabel): BitmapFont {
		val field = MetaLabel::class.java.getDeclaredField("font")
		field.isAccessible = true
		return field.get(label) as BitmapFont
	}
}
