package de.fatox.meta.test

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.input.KeyListener
import de.fatox.meta.ui.MetaToastManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The fixture's own fidelity, which is the only thing that makes it worth having.
 *
 * A harness that dispatches *approximately* like `MetaInput` is worse than one that dispatches nothing: a test can
 * then pass against a build where a modal did not own input, or where a consumed event was delivered twice, and the
 * failure looks like production being fine. Each asymmetry `MetaInput` has is pinned here, because they are asymmetric
 * on purpose and easy to "tidy" into agreement.
 */
class DispatchingInputTest {

	private lateinit var input: MetaInputProcessor

	@BeforeTest
	fun setUp() {
		MetaHeadlessUi.install(input = { DispatchingInput() })
		input = MetaInject.inject()
	}

	@AfterTest
	fun tearDown() = MetaHeadlessUi.dispose()

	private class Recorder(private val consume: Boolean = false) : InputAdapter() {
		val events = ArrayList<String>()

		override fun keyDown(keycode: Int): Boolean = record("keyDown")
		override fun keyUp(keycode: Int): Boolean = record("keyUp")
		override fun touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean = record("touchDown")
		override fun touchUp(x: Int, y: Int, pointer: Int, button: Int): Boolean = record("touchUp")
		override fun touchDragged(x: Int, y: Int, pointer: Int): Boolean = record("touchDragged")
		override fun mouseMoved(x: Int, y: Int): Boolean = record("mouseMoved")
		override fun scrolled(amountX: Float, amountY: Float): Boolean = record("scrolled")

		private fun record(name: String): Boolean {
			events.add(name)
			return consume
		}
	}

	// ── The grab takes everything ─────────────────────────────────────────────

	@Test
	fun `an exclusive grab receives pointer motion and scroll, and the background does not`() {
		// The finding that mattered most: letting these through would make a modal test pass against a build where the
		// modal did not own input.
		val background = Recorder()
		val grab = Recorder()
		input.addGlobalInputProcessor(background)
		input.pushExclusiveProcessor(grab)

		input.touchDragged(1, 1, 0)
		input.mouseMoved(1, 1)
		input.scrolled(0f, 1f)
		input.touchDown(1, 1, 0, 0)
		input.keyDown(Input.Keys.A)

		assertEquals(
			listOf("touchDragged", "mouseMoved", "scrolled", "touchDown", "keyDown"),
			grab.events,
			"the grab did not receive every event type",
		)
		assertTrue(background.events.isEmpty(), "the background saw input under a grab: ${background.events}")
	}

	@Test
	fun `a cancelled touch is delivered as a release`() {
		// Otherwise an interrupted gesture never lets go of drag state.
		val recorder = Recorder()
		input.addGlobalInputProcessor(recorder)

		input.touchCancelled(1, 1, 0, 0)

		assertEquals(listOf("touchUp"), recorder.events, "a cancelled touch did not arrive as a release")
	}

	// ── Short-circuiting, where and only where MetaInput does it ──────────────

	@Test
	fun `a consumed pointer event stops at the processor that consumed it`() {
		val consumer = Recorder(consume = true)
		val later = Recorder()
		input.addGlobalInputProcessor(consumer)
		input.addGlobalInputProcessor(later)

		input.touchDown(1, 1, 0, 0)
		input.mouseMoved(1, 1)

		assertEquals(listOf("touchDown", "mouseMoved"), consumer.events)
		assertTrue(later.events.isEmpty(), "a later processor saw a consumed pointer event: ${later.events}")
	}

	@Test
	fun `a consumed key event still reaches every processor`() {
		// The asymmetry: MetaInput does not short-circuit keys, and making it symmetric would hide a second delivery
		// that production really does perform.
		val consumer = Recorder(consume = true)
		val later = Recorder()
		input.addGlobalInputProcessor(consumer)
		input.addGlobalInputProcessor(later)

		input.keyDown(Input.Keys.A)

		assertEquals(listOf("keyDown"), later.events, "a key event stopped at the first processor")
	}

	// ── Registrations that must not be inert ──────────────────────────────────

	@Test
	fun `scroll listeners are notified`() {
		var scrolls = 0
		input.addGlobalScrollListener { scrolls++ }

		input.scrolled(0f, 1f)

		assertEquals(1, scrolls, "a registered ScrollListener was never called")
	}

	@Test
	fun `a key listener fires on the release, not the press`() {
		// KeyListener with no hold duration raises onEvent from onUp. A harness firing on the press would disagree
		// with production about which edge an action lands on, and that distinction has already been a real bug.
		val order = ArrayList<String>()
		input.addGlobalKeyListener(
			Input.Keys.ESCAPE,
			0L,
			object : KeyListener() {
				override fun onEvent() {
					order.add("event")
				}
			},
		)

		input.keyDown(Input.Keys.ESCAPE)
		assertTrue(order.isEmpty(), "the listener fired on the press")

		input.keyUp(Input.Keys.ESCAPE)
		assertEquals(listOf("event"), order, "the listener did not fire on the release")
	}

	@Test
	fun `an unregistered processor stops receiving input`() {
		val recorder = Recorder()
		val handle: InputProcessor = input.addGlobalInputProcessor(recorder)
		input.removeGlobalInputProcessor(handle)

		input.keyDown(Input.Keys.A)

		assertTrue(recorder.events.isEmpty(), "a removed processor still received input")
	}

	// ── Held keys reach Gdx.input ─────────────────────────────────────────────

	@Test
	fun `a held key is visible to Gdx isKeyPressed`() {
		// UiControlHelper.activateSelectedActor asks Gdx.input, not this interface, so tracking modifiers privately
		// left ctrl+confirm activating a control that production leaves alone.
		assertFalse(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT), "nothing is held yet")

		input.keyDown(Input.Keys.CONTROL_LEFT)
		assertTrue(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT), "a held key was invisible to Gdx.input")
		assertTrue(input.isLeftCtrlDown, "the interface disagreed with Gdx.input")

		input.keyUp(Input.Keys.CONTROL_LEFT)
		assertFalse(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT), "a released key stayed held")
	}

	@Test
	fun `alt is tracked too`() {
		// Not a modifier this interface exposes, and one activateSelectedActor checks.
		input.keyDown(Input.Keys.ALT_LEFT)

		assertTrue(Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT), "alt was not tracked")
	}

	@Test
	fun `removing a registration removes the one handed over, not one that merely compares equal`() {
		// ArrayList.remove drops the first *equal* element. MetaInput uses removeValue(x, true) and the key-listener
		// API promises identity, so equality-based removal would report a registration gone while leaving it live.
		val first = EqualRecorder()
		val second = EqualRecorder()
		input.addGlobalInputProcessor(first)
		input.addGlobalInputProcessor(second)

		input.removeGlobalInputProcessor(second)
		input.keyDown(Input.Keys.A)

		assertEquals(listOf("keyDown"), first.events, "the wrong registration was removed")
		assertTrue(second.events.isEmpty(), "the removed registration still received input: ${second.events}")
	}

	/** Two distinct owners that compare equal, which is the case identity removal exists for. */
	private class EqualRecorder : InputAdapter() {
		val events = ArrayList<String>()

		override fun keyDown(keycode: Int): Boolean {
			events.add("keyDown")
			return false
		}

		override fun equals(other: Any?): Boolean = other is EqualRecorder
		override fun hashCode(): Int = 1
	}

	// ── The toast seam ────────────────────────────────────────────────────────

	@Test
	fun `a stage created for a toast manager is disposed with the harness`() {
		// The documented one-liner gives the caller nowhere to keep the reference, and a Stage owns a SpriteBatch, so
		// an install/dispose cycle per test would retain one each time.
		MetaHeadlessUi.dispose()
		// Built inside the factory, not before install: a Stage needs the GL stub that install() puts in place.
		var stage: TrackedStage? = null
		MetaHeadlessUi.install(
			toastManager = {
				val created = TrackedStage()
				MetaHeadlessUi.own(created)
				stage = created
				MetaToastManager(created)
			},
		)
		val renderer: UIRenderer = MetaInject.inject()
		renderer.getToastManager()
		val created = requireNotNull(stage) { "the factory never ran, so nothing was handed over" }

		MetaHeadlessUi.dispose()

		assertTrue(created.disposed, "the harness did not dispose a stage handed to it")
		// Reinstall so tearDown has something to tear down.
		MetaHeadlessUi.install()
	}

	private class TrackedStage : com.badlogic.gdx.scenes.scene2d.Stage() {
		var disposed = false

		override fun dispose() {
			disposed = true
			super.dispose()
		}
	}

	@Test
	fun `the supplied toast manager is the same one every time`() {
		MetaHeadlessUi.dispose()
		MetaHeadlessUi.install(toastManager = { MetaToastManager(toastStage()) })
		val renderer: UIRenderer = MetaInject.inject()

		assertSame(
			renderer.getToastManager(),
			renderer.getToastManager(),
			"each call built a new manager, so two toasts would land in unrelated collections",
		)
	}
}
