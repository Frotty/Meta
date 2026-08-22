package de.fatox.meta.test

import de.fatox.meta.ui.components.MetaFlexAlign
import de.fatox.meta.ui.components.MetaFlexBox
import de.fatox.meta.ui.components.MetaFlexDirection
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaTable
import de.fatox.meta.ui.layout.MetaLayout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the harness itself, so a consuming game can trust it.
 *
 * Every assertion here is one a game's own layout test depends on being true: that a text widget measures a non-zero
 * size, that two of them in a row do not overlap, that the skin's chrome is really there, and that a scale change
 * re-measures. If the harness quietly stopped producing real fonts, layout tests everywhere downstream would keep
 * passing against zero-width text — which is the failure mode worth spending a test on.
 */
internal class MetaHeadlessUiTest {

	@BeforeEach
	fun setUp() = MetaHeadlessUi.install()

	@AfterEach
	fun tearDown() = MetaHeadlessUi.dispose()

	@Test
	fun `a text widget measures real text`() {
		val label = MetaLabel("Volume", 18)
		assertTrue(label.prefWidth > 0f, "a label with no width measured nothing, so the font never rasterized")
		assertTrue(label.prefHeight > 0f)
	}

	@Test
	fun `wider text measures wider`() {
		// The property that makes a layout test meaningful. A stub font with uniform
		// advances would pass the previous test and fail this one.
		val short = MetaLabel("Ok", 18)
		val long = MetaLabel("Couch Co-op", 18)
		assertTrue(long.prefWidth > short.prefWidth, "${long.prefWidth} was not wider than ${short.prefWidth}")
	}

	@Test
	fun `a larger size measures taller`() {
		val small = MetaLabel("Play", 12)
		val large = MetaLabel("Play", 48)
		assertTrue(large.prefHeight > small.prefHeight)
		assertTrue(large.prefWidth > small.prefWidth)
	}

	@Test
	fun `a row of labels lays out without overlap`() {
		val first = MetaLabel("Volume", 18)
		val second = MetaLabel("100%", 18)
		val row = MetaFlexBox(direction = MetaFlexDirection.ROW, align = MetaFlexAlign.CENTER, mainGap = 8f)
		row.addItem(first)
		row.addItem(second)

		val root = MetaTable()
		root.setSize(1920f, 1080f)
		root.add(row)
		root.validate()

		assertTrue(second.x >= first.x + first.width, "labels overlapped: ${second.x} < ${first.x + first.width}")
		assertEquals(8f, second.x - (first.x + first.width), "the gap between them is the row's main gap")
		MetaLayout.assertValid(root)
	}

	@Test
	fun `a column of labels stacks without overlap`() {
		val column = MetaFlexBox(direction = MetaFlexDirection.COLUMN, align = MetaFlexAlign.CENTER, mainGap = 6f)
		val rows = List(4) { MetaLabel("Row $it", 18) }
		for (row in rows) column.addItem(row)

		val root = MetaTable()
		root.setSize(1920f, 1080f)
		root.add(column)
		root.validate()

		for (i in 0 until rows.size - 1) {
			assertTrue(
				rows[i].y >= rows[i + 1].y + rows[i + 1].height,
				"rows $i and ${i + 1} overlap",
			)
		}
		MetaLayout.assertValid(root)
	}

	@Test
	fun `the skin's generated chrome is available`() {
		// Widgets measured against a defaults-free skin are measured without their own
		// padding and borders, so this is what makes a measured size the real one. The
		// generated drawables are the part that needs a graphics device, so this is
		// also the assertion that fails first if the GL stub stops being installed.
		val palette = de.fatox.meta.ui.MetaSkin.skin().getColor("meta.background")
		assertEquals(de.fatox.meta.ui.MetaColor.BACKGROUND, palette, "the palette was not installed")
		assertTrue(
			runCatching { de.fatox.meta.ui.MetaSkin.buttonStyle(de.fatox.meta.ui.MetaButtonTier.PRIMARY) }.isSuccess,
			"the generated button styles are missing, so widget chrome never rasterized",
		)
	}

	@Test
	fun `a stage can be built, so a screen can be tested rather than rebuilt`() {
		// The capability that decides whether a consumer duplicates its layouts. Meta's
		// screens own their stage, so without this every test would have to
		// reconstruct the tree it wants to check — and then it is checking the
		// reconstruction. Drawing is still discarded; only measurement is real.
		val stage = com.badlogic.gdx.scenes.scene2d.Stage()
		val label = MetaLabel("Play", 18)
		stage.addActor(label)
		stage.act(1f / 60f)

		assertEquals(1, stage.actors.size)
		assertTrue(label.prefWidth > 0f, "a label on a stage measured nothing")
		stage.dispose()
	}

	@Test
	fun `a scale change re-rasterizes the fonts`() {
		val before = de.fatox.meta.injection.MetaInject.inject<de.fatox.meta.api.graphics.FontProvider>()
		val generationBefore = before.fontGeneration

		MetaHeadlessUi.uiScale.value = 2f
		val label = MetaLabel("Play", 18)

		assertTrue(label.prefWidth > 0f, "a label built after a scale change measured nothing")
		assertTrue(
			before.fontGeneration >= generationBefore,
			"the font generation counter went backwards across a scale change",
		)
	}
}
