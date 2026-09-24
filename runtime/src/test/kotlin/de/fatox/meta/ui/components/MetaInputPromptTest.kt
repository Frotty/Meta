package de.fatox.meta.ui.components

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.api.graphics.physicalPixelsPerStageUnit
import de.fatox.meta.test.MetaHeadlessUi
import de.fatox.meta.ui.MetaInputGlyphFaces
import de.fatox.meta.ui.MetaSkin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Button prompts: Kenney's glyphs, found by name and sized from the label.
 *
 * <p>Pixels are not observable headless, so what is held here is what a prompt is built from - that every name the
 * presets ask for exists in Kenney's faces (a misspelt one draws nothing, silently), that keys map to Kenney's key
 * art, and the geometry a prompt bar is laid out from.
 */
class MetaInputPromptTest {
	@BeforeEach fun setUp() = MetaHeadlessUi.install()
	@AfterEach fun tearDown() = MetaHeadlessUi.dispose()

	private fun assertDrawn(glyph: MetaInputGlyph) {
		assertTrue(MetaInputGlyphFaces.has(glyph.set, glyph.name)) { "${glyph.set} has no glyph '${glyph.name}'" }
		assertNotNull(MetaInputGlyphFaces.region(glyph.set, glyph.name, 48)) { "'${glyph.name}' rasterized to nothing" }
	}

	@Test
	fun `every preset names a glyph Kenney actually drew`() {
		for (family in MetaPadFamily.entries) {
			for (button in MetaFaceButton.entries) assertDrawn(MetaInputGlyphs.face(family, button))
			assertDrawn(MetaInputGlyphs.dpadVertical(family))
			assertDrawn(MetaInputGlyphs.menu(family))
			for (right in listOf(false, true)) for (shoulder in listOf(false, true)) {
				assertDrawn(MetaInputGlyphs.trigger(family, right, shoulder))
			}
		}
		assertDrawn(MetaInputGlyphs.arrowsVertical())
	}

	@Test
	fun `every key with Kenney art maps to it, and the rest get a blank keycap with a name`() {
		for (code in 0..Input.Keys.MAX_KEYCODE) {
			val name = MetaInputGlyphs.keyName(code) ?: continue
			assertTrue(MetaInputGlyphFaces.has(MetaGlyphSet.KEYBOARD, name)) { "key $code maps to missing '$name'" }
		}
		assertEquals("keyboard_escape", MetaInputGlyphs.key(Input.Keys.ESCAPE, "Esc").name)
		val unmapped = MetaInputGlyphs.key(Input.Keys.NUM_LOCK, "Num")
		assertEquals("Num", unmapped.text, "an unmapped key lost its name")
		assertEquals("", unmapped.name, "an unmapped key was given a glyph Kenney did not draw for it")
		val named = MetaInputPrompt(listOf(unmapped), "", 24)
		val longer = MetaInputPrompt(listOf(MetaInputGlyphs.key(Input.Keys.NUM_LOCK, "Numeric lock")), "", 24)
		assertTrue(longer.prefWidth > named.prefWidth) { "the stand-in text does not take its own width" }
		assertNull(MetaInputGlyphFaces.region(MetaGlyphSet.XBOX, "no_such_glyph", 48))
	}

	@Test
	fun `glyphs with cut-outs keep them`() {
		// Kenney's font builds wind some holes the same way as the shape around them, which FreeType fills in: the Xbox
		// LT came out as a blank shape and the PlayStation circle as a solid disc. The bundled faces are re-wound; these
		// fractions are measured, with the broken builds at about 0.90 and 0.76.
		assertTrue(MetaInputGlyphFaces.inkCoverage(MetaGlyphSet.XBOX, "xbox_lt", 96) < 0.85f) { "xbox_lt lost its letters" }
		assertTrue(MetaInputGlyphFaces.inkCoverage(MetaGlyphSet.PLAYSTATION, "playstation_button_circle", 96) < 0.72f) {
			"the PlayStation circle is a solid disc"
		}
	}

	@Test
	fun `the glyph is sized from the label, so a bigger prompt is the same prompt`() {
		val small = MetaInputPrompt(listOf(MetaInputGlyphs.face(MetaPadFamily.XBOX, MetaFaceButton.SOUTH)), "", 20)
		val large = MetaInputPrompt(listOf(MetaInputGlyphs.face(MetaPadFamily.XBOX, MetaFaceButton.SOUTH)), "", 40)
		assertEquals(2f, large.prefWidth / small.prefWidth, 0.1f, "the glyph does not scale with the font size")
		assertTrue(small.prefHeight >= small.prefWidth - 0.5f) { "a glyph cell is not square" }
	}

	@Test
	fun `the label adds its width and follows setLabel`() {
		val prompt = MetaInputPrompt(listOf(MetaInputGlyphs.arrowsVertical()), "Go", 24)
		val before = prompt.prefWidth
		prompt.setLabel("Navigate the menu")
		assertTrue(prompt.prefWidth > before) { "a longer label did not widen the prompt" }
	}

	@Test
	fun `every glyph draws, on a bar, on a stage`() {
		val stage = Stage(ScreenViewport())
		val bar = MetaPromptBar(24)
		val all = MetaPadFamily.entries.flatMap { family -> MetaFaceButton.entries.map { MetaInputGlyphs.face(family, it) } } +
			listOf(MetaInputGlyphs.key(Input.Keys.ENTER, "Enter"), MetaInputGlyphs.key(Input.Keys.NUM_LOCK, "Num"))
		bar.setEntries(all.map { MetaPromptBar.Entry(listOf(it), "x") })
		stage.addActor(bar)
		bar.setSize(bar.prefWidth, bar.prefHeight)
		stage.draw()
		assertEquals(all.size, bar.children.size, "the bar did not build a prompt per entry")
		assertNotNull(MetaSkin.skin().optional(MetaSkin.PROMPT_BAR, Drawable::class.java), "the bar's pill is missing")
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
		val prompt = MetaInputPrompt(listOf(MetaInputGlyphs.key(Input.Keys.ESCAPE, "Esc")), "Back", 24)
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
		bar.setEntries(listOf(MetaPromptBar.Entry(listOf(MetaInputGlyphs.key(Input.Keys.ESCAPE, "Esc")), "Back")))
		val singleRow = bar.prefHeight
		val many = List(6) {
			MetaPromptBar.Entry(listOf(MetaInputGlyphs.key(Input.Keys.ENTER, "Enter")), "A rather long translated label")
		}
		bar.setEntries(many)
		bar.setSize(bar.prefWidth, bar.prefHeight)
		bar.validate()
		assertTrue(bar.width <= stage.width) { "the bar is ${bar.width} wide on a ${stage.width} stage" }
		assertTrue(bar.height > singleRow * 1.5f) { "the prompts did not wrap onto more rows" }
		for (i in 0 until bar.children.size) {
			val child = bar.children[i]
			assertTrue(child.x >= 0f && child.x + child.width <= bar.width + 0.5f) {
				"a prompt at ${child.x}..${child.x + child.width} sticks out of a bar ${bar.width} wide"
			}
		}
		stage.dispose()
	}

	@Test
	fun `a prewarmed prompt draws without opening, generating or uploading anything`() {
		val stage = Stage(ScreenViewport())
		stage.viewport.update(1280, 720, true)
		val ppu = physicalPixelsPerStageUnit(stage.width)
		val glyphs = listOf(MetaInputGlyphs.face(MetaPadFamily.PLAYSTATION, MetaFaceButton.EAST),
			MetaInputGlyphs.key(Input.Keys.ESCAPE, "Esc"))
		MetaInputPrompt.prewarm(glyphs, 26, ppu)
		val warmed = MetaInputPrompt(glyphs, "Back", 26)
		stage.addActor(warmed)
		warmed.setSize(warmed.prefWidth, warmed.prefHeight)
		val before = MetaInputGlyphFaces.work
		stage.draw()
		assertEquals(before, MetaInputGlyphFaces.work, "drawing a prewarmed prompt still rasterized or uploaded")

		// Guards the seam: the same glyphs at a size nobody warmed do cost work on draw, so the count is being kept.
		val cold = MetaInputPrompt(glyphs, "Back", 31)
		stage.addActor(cold)
		cold.setSize(cold.prefWidth, cold.prefHeight)
		stage.draw()
		assertTrue(MetaInputGlyphFaces.work > before) { "an unwarmed size drew without any work; the counter is dead" }
		stage.dispose()
	}

	@Test
	fun `required fonts are the ones a prompt asks for`() {
		val fonts = MetaInputPrompt.requiredFonts(30)
		assertEquals(30 to FontType.REGULAR, fonts[0], "the label's own face is not listed first")
		assertEquals(1, fonts.size)
	}
}
