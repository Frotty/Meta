package de.fatox.meta.api

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import de.fatox.meta.Meta
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.test.MetaHeadlessUi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The splash screen driving a real startup, with no [Meta] application behind it.
 *
 * Both halves of that sentence are the point. The phase machine was only ever exercised by running the editor,
 * and it could only run there: `show()` dereferenced `Meta.instance`, so the screen threw for any application that
 * adopted Meta's UI layer without its `Game` class — and threw in a test, which is why there was no test. It now
 * reads [Meta.instanceOrNull], and every assertion below is made with that field never assigned.
 *
 * Frames are driven by hand rather than by a clock. A [SplashCallbacks.prepareAssets] worker would make the
 * result depend on how fast a thread starts, so these cover the GL-thread path; the worker phases have no branch
 * of their own beyond the one already covered by the status mapping.
 */
internal class SplashScreenTest {

	/** Enough frames for the fades, the hold and a slow startup, with room to spare. */
	private val frameCap = 600
	private val frameSeconds = 1f / 60f

	@BeforeEach
	fun setUp() {
		MetaHeadlessUi.install()
		// The one thing the panel needs that the layout harness has no use for. Registered
		// after install(), which owns (and clears) the graph.
		MetaInject.global { singleton { SpriteBatch() } }
	}

	@AfterEach
	fun tearDown() = MetaHeadlessUi.dispose()

	/** Which frame the loop is on, so a callback can record when it was called. */
	private var frame = 0

	/** Renders until [until] holds, or gives up. Returns the frames it took. */
	private fun SplashScreen.runUntil(until: () -> Boolean): Int {
		while (frame < frameCap) {
			frame++
			render(frameSeconds)
			if (until()) return frame
		}
		return -1
	}

	@Test
	fun `the application's startup work is advanced in slices before the panel goes away`() {
		val events = ArrayList<String>()
		val sliceFrames = ArrayList<Int>()
		val sliceBudgets = ArrayList<Int>()
		val splash = SplashScreen(
			SplashCallbacks(
				queueAssets = { events.add("queue") },
				startupLoad = { millis ->
					sliceFrames.add(frame)
					sliceBudgets.add(millis)
					events.add("slice")
					sliceFrames.size == 4
				},
				onLoaded = { events.add("loaded") },
			),
		)

		splash.show()
		val frames = splash.runUntil { events.contains("loaded") }

		assertTrue(frames > 0, "the splash never finished loading in $frameCap frames: $events")
		assertEquals(4, sliceFrames.size, "the startup load was not called until it reported completion")
		assertEquals("queue", events.first(), "assets must be queued before the application loads its own")
		assertEquals("loaded", events.last(), "the loaded callback ran before startup work had finished")
		// One call per frame, never four in a row: the whole reason for the phase is that the
		// panel draws between the steps. Draining the queue inside one frame would satisfy every
		// assertion above and none of this one.
		assertEquals(
			sliceFrames.size,
			sliceFrames.distinct().size,
			"two slices ran in the same frame, so the panel never drew between them: $sliceFrames",
		)
		assertTrue(
			sliceBudgets.none { it <= 0 },
			"a slice was handed no budget at all, so that frame did no work: $sliceBudgets",
		)
	}

	@Test
	fun `startup work that never finishes holds the panel instead of handing over`() {
		// The property that makes the phase worth having. If an unfinished load fell through
		// to onLoaded, the caller would be handed a screen whose resources do not exist yet —
		// which is exactly the freeze-then-crash the phase replaces, only quieter.
		var slices = 0
		var loaded = false
		val splash = SplashScreen(
			SplashCallbacks(
				startupLoad = { slices++; false },
				onLoaded = { loaded = true },
			),
		)

		splash.show()
		repeat(frameCap) { splash.render(frameSeconds) }

		assertFalse(loaded, "the splash handed over while the application was still loading")
		assertTrue(slices > 100, "the splash stopped asking for work after $slices slices")
	}

	@Test
	fun `a splash with no application work of its own still reaches its callback`() {
		// The existing constructors pass no startup load, so this is the path every current
		// caller takes: the new phase must be skipped, not entered and waited on.
		var loaded = false
		val splash = SplashScreen(onLoaded = { loaded = true })

		splash.show()
		val frames = splash.runUntil { loaded }

		assertTrue(frames > 0, "a splash with no callbacks never completed in $frameCap frames")
	}

	@Test
	fun `a step that pumps the window does not advance the machine underneath itself`() {
		// Applying a display mode re-enters the render loop: GLFW pumps the platform window, LWJGL3 runs a frame,
		// and this screen's render() lands on the stack inside the step that asked for the change. Unguarded, the
		// next slice began while the current one was still running — observed in a real game as step two finishing
		// before step one, and step one never reporting at all.
		val order = ArrayList<String>()
		val pending = ArrayDeque(listOf("first", "second", "third"))
		var pumped = false
		val holder = arrayOfNulls<SplashScreen>(1)
		val splash = SplashScreen(
			SplashCallbacks(
				startupLoad = {
					val step = pending.removeFirstOrNull()
					if (step != null) {
						order.add("enter $step")
						if (!pumped) {
							pumped = true
							// What the platform does to us, done deliberately.
							holder[0]?.render(frameSeconds)
						}
						order.add("leave $step")
					}
					pending.isEmpty()
				},
				onLoaded = { order.add("loaded") },
			),
		)
		holder[0] = splash

		splash.show()
		val frames = splash.runUntil { order.contains("loaded") }

		assertTrue(frames > 0, "the splash never finished: $order")
		assertTrue(pumped, "the re-entrant render never happened, so this test proved nothing")
		assertEquals(
			listOf(
				"enter first", "leave first",
				"enter second", "leave second",
				"enter third", "leave third",
				"loaded",
			),
			order,
			"a step began while another was still on the stack",
		)
	}

	@Test
	fun `screen changes are unthrottled when nothing owns the screens`() {
		// The throttle exists to stop Meta swapping screens twice in one frame. With no
		// application there is nothing to throttle, and the splash waits on this to leave
		// its first phase — so a false here is a startup that never begins.
		assertTrue(Meta.canChangeScreen())
		assertEquals(null, Meta.instanceOrNull)
	}
}
