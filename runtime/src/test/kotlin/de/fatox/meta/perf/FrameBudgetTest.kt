package de.fatox.meta.perf

import com.badlogic.gdx.scenes.scene2d.Stage
import de.fatox.meta.test.AllocationProbe
import de.fatox.meta.test.GlCallRecorder
import de.fatox.meta.test.MetaHeadlessUi
import de.fatox.meta.test.NoOpGL20
import de.fatox.meta.test.toastStage
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType
import de.fatox.meta.ui.components.MetaCheckBox
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaTextButton
import de.fatox.meta.ui.components.MetaTextField
import de.fatox.meta.ui.metaFlexColumn
import de.fatox.meta.ui.metaFlexRow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Frame-cost gates for Meta's UI.
 *
 * These assert on **counts, not durations**, and that is the whole design. A shared CI runner's clock speed, its
 * noisy neighbours and its thermal state change how long a frame takes; none of them change how many textures it
 * bound or how many bytes it allocated. So these can gate a pull request without ever flaking, which is precisely
 * what a wall-clock benchmark cannot do on hosted CI.
 *
 * Budgets are set from measurement, never from guesswork: each one records what the tree actually does today and
 * leaves modest headroom. When an optimisation lands, tighten the budget in the same commit — a gate left slack
 * after the win it was written for stops guarding anything.
 *
 * Headroom also absorbs one genuine cross-platform variable: glyph atlas packing depends on the FreeType native
 * build, so the exact page count can differ between a Windows workstation and a Linux runner. Texture *creation*
 * counts have no such dependency and are pinned tighter.
 */
class FrameBudgetTest {
	@BeforeEach fun setUp() = MetaHeadlessUi.install()
	@AfterEach fun tearDown() = MetaHeadlessUi.dispose()

	/**
	 * A representative Meta screen: a heading over six rows of label, field, checkbox and button.
	 *
	 * Deliberately ordinary. The point is to measure what a consuming game's settings or options screen costs, not
	 * to build a worst case that nobody ships.
	 */
	private fun referenceScreen(stage: Stage) {
		val root = metaFlexColumn(gap = MetaSpacing.MD) {
			setSize(REFERENCE_WIDTH, REFERENCE_HEIGHT)
			addItem(MetaLabel("Settings", MetaType.HEADING))
			for (row in 0 until REFERENCE_ROWS) {
				addItem(
					metaFlexRow(gap = MetaSpacing.SM) {
						addItem(MetaLabel("Option $row", MetaType.LABEL))
						addItem(MetaTextField("value $row"))
						addItem(MetaCheckBox())
						addItem(MetaTextButton("Apply"))
					},
				)
			}
		}
		stage.addActor(root)
		root.setSize(REFERENCE_WIDTH, REFERENCE_HEIGHT)
		stage.act(FRAME)
		root.validate()
		// Draw once before measuring: the first frame rasterizes glyphs into the font atlas, which is startup cost
		// rather than steady state, and counting it would measure the wrong thing.
		repeat(WARM_FRAMES) { stage.draw() }
	}

	@Test
	fun `a reference screen stays within its draw call budget`() {
		val stage = toastStage()
		referenceScreen(stage)

		val counts = GlCallRecorder.record { stage.draw() }

		assertTrue(counts.textureBinds <= MAX_TEXTURE_BINDS) {
			"A $REFERENCE_ROWS-row screen bound ${counts.textureBinds} textures (budget $MAX_TEXTURE_BINDS). " +
				"SpriteBatch flushes on every texture change, so this is the frame's draw-call count. " +
				"If this rose, something stopped sharing a texture. $counts"
		}
		assertTrue(counts.drawCalls <= MAX_DRAW_CALLS) {
			"Frame issued ${counts.drawCalls} draw calls (budget $MAX_DRAW_CALLS). $counts"
		}
		assertTrue(counts.shaderSwitches <= MAX_SHADER_SWITCHES) {
			"Frame switched shaders ${counts.shaderSwitches} times (budget $MAX_SHADER_SWITCHES). $counts"
		}
	}

	@Test
	fun `stage act allocates nothing`() {
		assumeTrue(AllocationProbe.isSupported, "This JVM cannot report per-thread allocation")
		val stage = toastStage()
		referenceScreen(stage)

		val bytes = AllocationProbe.measure(warmup = ALLOC_WARMUP, iterations = ALLOC_ITERATIONS) { stage.act(FRAME) }

		assertTrue(bytes <= 0) {
			"stage.act allocated $bytes bytes per frame. AGENTS.md requires act/draw/layout to be allocation-free; " +
				"at 60fps this is ${bytes * 60 / 1024} KB/s of avoidable GC pressure."
		}
	}

	@Test
	fun `stage draw allocates nothing`() {
		assumeTrue(AllocationProbe.isSupported, "This JVM cannot report per-thread allocation")
		val stage = toastStage()
		referenceScreen(stage)

		// The ordinary HeadlessGL20 is a reflective Proxy, which allocates an Object[] per GL call - about 39 bytes,
		// or ~27 KB across one drawn frame. Measuring the draw path through it would report the instrument.
		NoOpGL20.install()
		try {
			repeat(WARM_FRAMES) { stage.draw() }
			val bytes = AllocationProbe.measure(warmup = ALLOC_WARMUP, iterations = ALLOC_ITERATIONS) { stage.draw() }
			assertTrue(bytes <= 0) {
				"stage.draw allocated $bytes bytes per frame. AGENTS.md requires draw to be allocation-free; " +
					"at 60fps this is ${bytes * 60 / 1024} KB/s of avoidable GC pressure."
			}
		} finally {
			NoOpGL20.uninstall()
		}
	}

	@Test
	fun `the generated skin stays within its texture budget`() {
		val counts = GlCallRecorder.record {
			MetaSkin.dispose()
			MetaSkin.initialize()
		}

		assertTrue(counts.callsTo("glGenTexture") <= MAX_SKIN_TEXTURES) {
			"Generating the skin created ${counts.callsTo("glGenTexture")} textures (budget $MAX_SKIN_TEXTURES). " +
				"Each one is a texture the UI must bind separately, so this is the ceiling on how well any Meta " +
				"screen can batch. $counts"
		}
		assertTrue(MetaSkin.atlasPageCount <= MAX_ATLAS_PAGES) {
			"The generated chrome spilled onto ${MetaSkin.atlasPageCount} atlas pages (budget $MAX_ATLAS_PAGES). " +
				"Every page is a separate bind, so this is the ceiling on batching for the chrome."
		}
	}

	@Test
	fun `a row of text buttons stays within its alternation budget`() {
		val stage = toastStage()
		val root = metaFlexColumn(gap = MetaSpacing.SM) {
			setSize(REFERENCE_WIDTH, REFERENCE_HEIGHT)
			for (index in 0 until BUTTON_ROW_COUNT) addItem(MetaTextButton("Apply $index"))
		}
		stage.addActor(root)
		root.setSize(REFERENCE_WIDTH, REFERENCE_HEIGHT)
		stage.act(FRAME)
		root.validate()
		repeat(WARM_FRAMES) { stage.draw() }

		val counts = GlCallRecorder.record { stage.draw() }

		// Isolates alternation from texture count: these buttons use two textures between them, and every switch
		// from skin patch to glyphs and back flushes the batch.
		assertTrue(counts.textureBinds <= MAX_BUTTON_ROW_BINDS) {
			"$BUTTON_ROW_COUNT buttons bound ${counts.textureBinds} textures (budget $MAX_BUTTON_ROW_BINDS). $counts"
		}
	}

	private companion object {
		const val REFERENCE_WIDTH = 1280f
		const val REFERENCE_HEIGHT = 720f
		const val REFERENCE_ROWS = 6
		const val FRAME = 1f / 60f
		const val WARM_FRAMES = 5
		const val ALLOC_WARMUP = 200
		const val ALLOC_ITERATIONS = 50

		/**
		 * Measured at 7, from 37 before any atlas work and 31 with the chrome alone on a page.
		 *
		 * The last step was the one that mattered: chrome and glyphs share a page, so a widget's background and its
		 * text come from the same texture and there is nothing for the batch to switch to. What remains is real
		 * work - the text fields clip, and clipping flushes.
		 */
		const val MAX_TEXTURE_BINDS = 12
		const val MAX_DRAW_CALLS = 12
		const val MAX_SHADER_SWITCHES = 2

		/**
		 * Twenty text buttons in one draw call, measured at 1. It was 40.
		 *
		 * This case exists because it isolates alternation from everything else: twenty buttons used two textures
		 * between them and still cost forty draw calls, because scene2d draws each background then its text and the
		 * batch flushed on every switch. Reducing texture *count* never moved it; sharing the page did.
		 *
		 * The tightest gate in the suite on purpose. Any regression that separates chrome from glyphs again -
		 * a font that misses the shared packer, a page overflow - shows up here first and nowhere else.
		 */
		const val MAX_BUTTON_ROW_BINDS = 4
		const val BUTTON_ROW_COUNT = 20

		/**
		 * Two: one atlas page holding all 84 generated drawables, plus the default `BitmapFont` that `addStyles`
		 * creates when a skin supplies none. Was 85 - one private 32x32 texture per drawable.
		 *
		 * Deterministic, no font rasterization involved, so pinned exactly rather than given headroom.
		 */
		const val MAX_SKIN_TEXTURES = 2
		/**
		 * One page for chrome *and* every rasterized glyph. A second page means the atlas overflowed, and the
		 * batch starts flushing between whatever landed on each - which is the whole cost this design removes.
		 */
		const val MAX_ATLAS_PAGES = 1
	}
}
