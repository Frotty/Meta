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
	fun `widgets that resolve dependencies in their constructor can be built`() {
		// Most Meta widgets fetch their font lazily; a few resolve eagerly, and a
		// missing eager dependency is not a degraded widget — it is a
		// GdxRuntimeException before the tree can be measured. MetaSelectBox takes a
		// UiControlHelper that way, so a screen containing one used to fail outright.
		val box = de.fatox.meta.ui.components.MetaSelectBox<String>()
		val field = de.fatox.meta.ui.components.MetaTextField("hello")
		val root = MetaTable()
		root.add(box)
		root.add(field)
		root.setSize(1920f, 1080f)
		root.validate()

		assertTrue(box.prefHeight > 0f, "the select box measured nothing")
		assertTrue(field.prefHeight > 0f, "the text field measured nothing")
		MetaLayout.assertValid(root)
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
	fun `teardown puts the GL globals back`() {
		// These are process-wide. A stub left installed silently changes every later
		// test in the JVM — MetaFontProviderTest asserts that font generation fails
		// with an NPE naming Gdx.gl, so it would pass or fail depending on which class
		// the runner reached first. Order-dependent tests are worse than absent ones.
		assertTrue(com.badlogic.gdx.Gdx.gl != null, "the harness should have installed a GL stub")

		MetaHeadlessUi.dispose()
		assertEquals(null, com.badlogic.gdx.Gdx.gl, "Gdx.gl was left pointing at the stub")
		assertEquals(null, com.badlogic.gdx.Gdx.gl20, "Gdx.gl20 was left pointing at the stub")

		// Put it back for the shared @AfterEach, which is a no-op once disposed.
		MetaHeadlessUi.install()
	}

	@Test
	fun `teardown still restores the globals when the graph was pulled out from under it`() {
		// A setup that fails part way leaves the harness in some intermediate state,
		// and the one thing teardown must still manage is handing the process-wide
		// globals back. Emptying the graph is the closest an outside test can get to
		// that shape without a fault-injection seam in the fixture.
		de.fatox.meta.injection.MetaInject.global(clear = true) {}

		MetaHeadlessUi.dispose()
		assertEquals(null, com.badlogic.gdx.Gdx.gl, "a partial teardown left the GL stub installed")

		MetaHeadlessUi.install()
		assertTrue(MetaLabel("Play", 18).prefWidth > 0f, "the harness could not come back up afterwards")
	}

	@Test
	fun `an install and dispose cycle can be repeated`() {
		// The documented usage is per test, so the cycle has to be re-entrant: fonts
		// disposed, graph emptied, GL restored, and all of it able to start again.
		repeat(3) {
			MetaHeadlessUi.dispose()
			MetaHeadlessUi.install()
			assertTrue(MetaLabel("Play", 18).prefWidth > 0f, "the harness did not come back up")
		}
	}

	/** Counts the refresh calls a real font-caching widget would receive. */
	private class CountingRefreshable : com.badlogic.gdx.scenes.scene2d.Actor(), de.fatox.meta.ui.FontRefreshable {
		var refreshes = 0
		override fun refreshFont() {
			refreshes++
		}
	}

	@Test
	fun `refreshing a tree reaches every widget in it`() {
		// The walk is the whole point: a widget that never re-fetches keeps a face that
		// disposeOrphanedFonts is about to release. Asserted through a FontRefreshable
		// spy rather than through the provider's generation counter, because
		// disposeOrphanedFonts bumps that counter on its own — an earlier version of
		// this test passed with the walk deleted, which is exactly the weakness the
		// review flagged in the version before that.
		val nested = CountingRefreshable()
		val inner = MetaTable()
		inner.add(nested)
		val root = MetaTable()
		root.add(MetaLabel("Play", 18))
		root.add(inner)
		root.setSize(1920f, 1080f)
		root.validate()

		MetaHeadlessUi.uiScale.value = 2f
		MetaHeadlessUi.refreshFonts(root)

		assertEquals(1, nested.refreshes, "the refresh walk did not reach a nested widget")
	}

	@Test
	fun `refreshing a tree after a scale change re-rasterizes its fonts`() {
		val provider = de.fatox.meta.injection.MetaInject
			.inject<de.fatox.meta.api.graphics.FontProvider>("default")
		val root = MetaTable()
		root.add(MetaLabel("Play", 18))
		root.setSize(1920f, 1080f)
		root.validate()

		val generationBefore = provider.fontGeneration
		val faceBefore = provider.getFont(18, de.fatox.meta.api.graphics.FontType.REGULAR)

		MetaHeadlessUi.uiScale.value = 2f
		MetaHeadlessUi.refreshFonts(root)

		assertTrue(
			provider.fontGeneration > generationBefore,
			"the provider never regenerated: ${provider.fontGeneration} was not past $generationBefore",
		)
		assertTrue(
			provider.getFont(18, de.fatox.meta.api.graphics.FontType.REGULAR) !== faceBefore,
			"the same face came back after a scale change, so nothing was re-rasterized",
		)
	}

	@Test
	fun `a scale change alone leaves an existing tree on its old faces`() {
		// Pinning the boundary the refresh exists for, rather than leaving a consumer
		// to discover it: the provider re-rasterizes lazily, so writing the scale
		// without refreshing regenerates nothing at all.
		val provider = de.fatox.meta.injection.MetaInject
			.inject<de.fatox.meta.api.graphics.FontProvider>("default")
		val root = MetaTable()
		root.add(MetaLabel("Play", 18))
		root.validate()

		val generationBefore = provider.fontGeneration
		MetaHeadlessUi.uiScale.value = 2f

		assertEquals(
			generationBefore,
			provider.fontGeneration,
			"a bare scale write regenerated fonts, so refreshFonts is no longer the documented step",
		)
	}
}
