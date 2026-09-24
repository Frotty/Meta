package de.fatox.meta.ui.components

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.api.graphics.physicalPixelsPerStageUnit
import de.fatox.meta.test.MetaHeadlessUi
import de.fatox.meta.ui.MetaInputGlyphSkin
import de.fatox.meta.ui.MetaSkin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Button prompts measure the way they draw.
 *
 * <p>Pixels are not observable headless, so what is held here is the geometry a prompt bar is laid out from - which
 * is where the old prompts went wrong: a glyph sized from its font's em box rather than from the text beside it, and
 * a key that did not grow with its name.
 */
class MetaInputPromptTest {
	@BeforeEach fun setUp() = MetaHeadlessUi.install()
	@AfterEach fun tearDown() = MetaHeadlessUi.dispose()

	@Test
	fun `every glyph shape is in the skin`() {
		val names = listOf(
			MetaInputGlyphSkin.KEYCAP, MetaInputGlyphSkin.DISC, MetaInputGlyphSkin.CROSS, MetaInputGlyphSkin.RING,
			MetaInputGlyphSkin.SQUARE, MetaInputGlyphSkin.TRIANGLE, MetaInputGlyphSkin.DPAD, MetaInputGlyphSkin.STICK,
			MetaInputGlyphSkin.ARROW_UP, MetaSkin.PROMPT_BAR,
		)
		for (name in names) assertNotNull(MetaSkin.skin().optional(name, Drawable::class.java), "$name is missing")
		assertEquals(1, MetaSkin.atlasPageCount, "the glyphs pushed the chrome onto a second atlas page")
	}

	@Test
	fun `a key grows with its name and never gets narrower than it is tall`() {
		val short = MetaInputPrompt(listOf(MetaInputGlyph.Key("A")), "", 24)
		val long = MetaInputPrompt(listOf(MetaInputGlyph.Key("Left Shift")), "", 24)
		assertTrue(long.prefWidth > short.prefWidth) { "a long key name did not widen its key" }
		assertTrue(short.prefWidth >= short.prefHeight - 0.5f) { "a one-letter key is narrower than it is tall" }
	}

	@Test
	fun `the glyph is sized from the label, so a bigger prompt is the same prompt`() {
		val small = MetaInputPrompt(listOf(MetaInputGlyphs.face(MetaPadFamily.XBOX, MetaFaceButton.SOUTH)), "", 20)
		val large = MetaInputPrompt(listOf(MetaInputGlyphs.face(MetaPadFamily.XBOX, MetaFaceButton.SOUTH)), "", 40)
		assertEquals(2f, large.prefWidth / small.prefWidth, 0.1f, "the glyph does not scale with the font size")
	}

	@Test
	fun `the label adds its width and follows setLabel`() {
		val prompt = MetaInputPrompt(listOf(MetaInputGlyph.DPad), "Go", 24)
		val before = prompt.prefWidth
		prompt.setLabel("Navigate the menu")
		assertTrue(prompt.prefWidth > before) { "a longer label did not widen the prompt" }
	}

	@Test
	fun `every kind of glyph draws, on a bar, on a stage`() {
		val stage = Stage(ScreenViewport())
		val bar = MetaPromptBar(24)
		val all = MetaPadFamily.entries.flatMap { family -> MetaFaceButton.entries.map { MetaInputGlyphs.face(family, it) } } +
			listOf(MetaInputGlyph.Key("Esc"), MetaInputGlyph.Key("", arrows = true), MetaInputGlyph.Shoulder("RB"),
				MetaInputGlyph.DPad, MetaInputGlyph.Stick)
		bar.setEntries(all.map { MetaPromptBar.Entry(listOf(it), "x") })
		stage.addActor(bar)
		bar.pack()
		stage.draw()
		assertEquals(all.size, bar.children.size, "the bar did not build a prompt per entry")
		stage.dispose()
	}

	@Test
	fun `a prompt under a fractionally placed, scaled group still draws on the pixel grid`() {
		val stage = Stage(ScreenViewport())
		stage.viewport.update(1280, 720, true)
		val group = Group()
		group.isTransform = true
		group.setPosition(10.37f, 5.61f)
		group.setScale(1.5f)
		stage.addActor(group)
		val prompt = MetaInputPrompt(listOf(MetaInputGlyph.Key("Esc")), "Back", 24)
		prompt.setPosition(0.23f, 0.41f)
		group.addActor(prompt)
		val ppu = physicalPixelsPerStageUnit(stage.width)
		val drawn = group.localToStageCoordinates(prompt.snappedOrigin(Vector2()))
		// Guards the premise: without the snap the origin is well off the grid, so passing below means something.
		val raw = prompt.localToStageCoordinates(Vector2())
		assertTrue(offGrid(raw.x, ppu) > 0.05f) { "the fixture is already on the grid" }
		assertEquals(0f, offGrid(drawn.x, ppu), 1e-3f, "the prompt's x is off the pixel grid under the group")
		assertEquals(0f, offGrid(drawn.y, ppu), 1e-3f, "the prompt's y is off the pixel grid under the group")
		stage.dispose()
	}

	/** How far [stageValue] is from the nearest physical pixel, in pixels. */
	private fun offGrid(stageValue: Float, ppu: Float): Float {
		val pixels = stageValue * ppu
		return kotlin.math.abs(pixels - kotlin.math.round(pixels))
	}

	@Test
	fun `a bar with more than fits wraps inside the stage instead of running off it`() {
		val stage = Stage(ScreenViewport())
		stage.viewport.update(480, 320, true)
		val bar = MetaPromptBar(24)
		stage.addActor(bar)
		val one = listOf(MetaPromptBar.Entry(listOf(MetaInputGlyph.Key("Esc")), "Back"))
		bar.setEntries(one)
		val singleRow = bar.prefHeight
		val many = List(6) { MetaPromptBar.Entry(listOf(MetaInputGlyph.Key("Enter")), "A rather long translated label") }
		bar.setEntries(many)
		bar.setSize(bar.prefWidth, bar.prefHeight)
		bar.validate()
		assertTrue(bar.width <= stage.width) { "the bar is ${bar.width} wide on a ${stage.width} stage" }
		assertTrue(bar.height > singleRow * 1.5f) { "the prompts did not wrap onto more rows" }
		for (child in bar.children) {
			assertTrue(child.x >= 0f && child.x + child.width <= bar.width + 0.5f) {
				"a prompt at ${child.x}..${child.x + child.width} sticks out of a bar ${bar.width} wide"
			}
		}
		stage.dispose()
	}

	@Test
	fun `required fonts are the ones a prompt asks for`() {
		val fonts = MetaInputPrompt.requiredFonts(30)
		assertEquals(30 to FontType.REGULAR, fonts[0], "the label's own face is not listed first")
		assertEquals(3, fonts.size)
	}
}
