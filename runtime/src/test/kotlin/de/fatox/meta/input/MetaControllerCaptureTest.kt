package de.fatox.meta.input

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerListener
import com.badlogic.gdx.controllers.ControllerMapping
import com.badlogic.gdx.controllers.ControllerPowerLevel
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.injection.MetaInject.Companion.global
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Raw controller-button capture, which is what a rebinding screen needs and what the UI layer otherwise goes out of
 * its way to prevent.
 *
 * The two halves matter equally. Capture has to *deliver* the raw code, because that is the thing being bound. And it
 * has to *suppress* the usual translation, because otherwise pressing the pad's A button while a rebind dialog waits
 * for a keyboard key emits ENTER into that dialog's exclusive processor and ENTER gets bound - a button press
 * recorded as a keystroke. A test that only checked delivery would pass on a build with that bug in it.
 */
internal class MetaControllerCaptureTest {
	private lateinit var bindings: MetaUiInputBindings
	private lateinit var input: RecordingInput
	private lateinit var controller: FakeController

	@BeforeTest
	fun setUp() {
		bindings = MetaUiInputBindings()
		global(clear = true) { singleton(bindings) }
		input = RecordingInput()
		MetaControllerListener.metaInput = input
		controller = FakeController()
	}

	@AfterTest
	fun tearDown() {
		// The listener is a singleton object and outlives any one test, so anything it is still holding leaks into
		// the next one. Each test releases the buttons it pressed, the way a real pad does, so this is a backstop and
		// not the mechanism - isolation that depended on `disconnected()` would depend on the very code under test,
		// and reverting that code showed up as a failure in an unrelated test rather than its own.
		MetaControllerListener.clearCaptures()
		MetaControllerListener.disconnected(controller)
		MetaControllerListener.metaInput = null
		global(clear = true) {}
	}

	/** Drops every capture. Releasing is by owner on purpose, so a test that used an inline lambda has no handle. */
	private fun clearCapture() {
		MetaControllerListener.clearCaptures()
		assertFalse(MetaControllerListener.capturing, "a capture outlived the test that started it")
	}

	// ── Delivery ──────────────────────────────────────────────────────────────

	@Test
	fun `a captured button reports its raw code and device`() {
		var seenController: Controller? = null
		var seenButton = -1
		val target: (Controller, Int) -> Unit = { c, code ->
			seenController = c
			seenButton = code
		}
		MetaControllerListener.captureButtons(target)

		val consumed = MetaControllerListener.buttonDown(controller, 7)

		assertTrue(consumed, "a captured press must be consumed, or it also reaches whatever is underneath")
		assertSame(controller, seenController, "the device did not travel with the capture")
		assertEquals(7, seenButton, "the raw button code was not reported")
		MetaControllerListener.buttonUp(controller, 7)
		MetaControllerListener.releaseCapture(target)
	}

	@Test
	fun `a button bound to a UI action is still captured raw`() {
		// The dangerous case: A is CONFIRM by default, so without capture this button becomes ENTER.
		bindings.setControllerButtonCodes(MetaUiAction.CONFIRM, 3)
		var seenButton = -1
		val target: (Controller, Int) -> Unit = { _, code -> seenButton = code }
		MetaControllerListener.captureButtons(target)

		MetaControllerListener.buttonDown(controller, 3)

		assertEquals(3, seenButton, "a bound button was translated instead of captured")
		MetaControllerListener.buttonUp(controller, 3)
		MetaControllerListener.releaseCapture(target)
	}

	// ── Suppression ───────────────────────────────────────────────────────────

	@Test
	fun `capture stops a bound button becoming a key`() {
		bindings.setControllerButtonCodes(MetaUiAction.CONFIRM, 3)
		MetaControllerListener.captureButtons { _, _ -> }

		MetaControllerListener.buttonDown(controller, 3)

		assertTrue(
			input.keysDown.isEmpty(),
			"the press was translated as well as captured, so a rebind dialog would bind ENTER: ${input.keysDown}",
		)
		MetaControllerListener.buttonUp(controller, 3)
		clearCapture()
	}

	@Test
	fun `capture stops the release becoming a key up`() {
		bindings.setControllerButtonCodes(MetaUiAction.CONFIRM, 3)
		MetaControllerListener.captureButtons { _, _ -> }

		MetaControllerListener.buttonUp(controller, 3)

		assertTrue(
			input.keysUp.isEmpty(),
			"a release emitted a key-up for a key that was never pressed: ${input.keysUp}",
		)
		clearCapture()
	}

	@Test
	fun `capture stops a stick push becoming arrow keys`() {
		controller.axes[bindings.verticalAxis] = -1f
		MetaControllerListener.captureButtons { _, _ -> }

		MetaControllerListener.axisMoved(controller, bindings.verticalAxis, -1f)

		assertTrue(input.keysDown.isEmpty(), "a nudged stick navigated inside a capture: ${input.keysDown}")
		clearCapture()
	}

	@Test
	fun `translation resumes once the capture is released`() {
		bindings.setControllerButtonCodes(MetaUiAction.CONFIRM, 3)
		val target: (Controller, Int) -> Unit = { _, _ -> }
		MetaControllerListener.captureButtons(target)
		MetaControllerListener.releaseCapture(target)

		MetaControllerListener.buttonDown(controller, 3)

		assertEquals(
			listOf(Input.Keys.ENTER),
			input.keysDown,
			"the pad stayed deaf after the dialog closed",
		)
	}

	@Test
	fun `a captured press keeps its release consumed after the capture ends`() {
		// The shape a rebind dialog actually has: it closes on the press, and closing releases the capture, so the
		// physical release lands after teardown with translation switched back on. Translating it emits a CONFIRM
		// key-up for a key that was never pressed - and UiControlHelper reads that as an activation, so letting go of
		// the button you just bound presses whatever is focused behind the dialog that closed.
		bindings.setControllerButtonCodes(MetaUiAction.CONFIRM, 3)
		val target: (Controller, Int) -> Unit = { _, _ -> }
		MetaControllerListener.captureButtons(target)

		MetaControllerListener.buttonDown(controller, 3)
		MetaControllerListener.releaseCapture(target)
		val consumed = MetaControllerListener.buttonUp(controller, 3)

		assertTrue(consumed, "the release escaped the capture that consumed its press")
		assertTrue(
			input.keysUp.isEmpty(),
			"releasing the bound button activated whatever was focused behind the dialog: ${input.keysUp}",
		)
	}

	@Test
	fun `only the captured press has its release consumed`() {
		// The other half: a button that was never captured must go back to translating normally, or the pad goes
		// half-deaf after any rebind.
		bindings.setControllerButtonCodes(MetaUiAction.CONFIRM, 3)
		val target: (Controller, Int) -> Unit = { _, _ -> }
		MetaControllerListener.captureButtons(target)
		MetaControllerListener.buttonDown(controller, 9)
		MetaControllerListener.releaseCapture(target)

		MetaControllerListener.buttonUp(controller, 3)

		assertEquals(
			listOf(Input.Keys.ENTER),
			input.keysUp,
			"an uncaptured button stopped translating its release",
		)
	}

	@Test
	fun `one pad disconnecting does not release another pad's captured press`() {
		// The guard exists for exactly this shape, so clearing every device's entries on any disconnect would defeat
		// it: pad A captures and closes the dialog, pad B is unplugged before A's release, and A's release is then
		// translated after all - activating whatever sits behind the dialog that closed.
		bindings.setControllerButtonCodes(MetaUiAction.CONFIRM, 3)
		val other = FakeController()
		val target: (Controller, Int) -> Unit = { _, _ -> }
		MetaControllerListener.captureButtons(target)
		MetaControllerListener.buttonDown(controller, 3)
		MetaControllerListener.releaseCapture(target)

		MetaControllerListener.disconnected(other)
		input.keysUp.clear()
		val consumed = MetaControllerListener.buttonUp(controller, 3)

		assertTrue(consumed, "another pad's disconnect released this pad's captured press")
		assertTrue(input.keysUp.isEmpty(), "the release leaked after an unrelated disconnect: ${input.keysUp}")
	}

	@Test
	fun `a disconnect forgets captured presses whose releases will never arrive`() {
		bindings.setControllerButtonCodes(MetaUiAction.CONFIRM, 3)
		val target: (Controller, Int) -> Unit = { _, _ -> }
		MetaControllerListener.captureButtons(target)
		MetaControllerListener.buttonDown(controller, 3)
		MetaControllerListener.releaseCapture(target)

		MetaControllerListener.disconnected(controller)
		input.keysUp.clear()
		MetaControllerListener.buttonUp(controller, 3)

		assertEquals(
			listOf(Input.Keys.ENTER),
			input.keysUp,
			"a stale captured press swallowed the next pad's release",
		)
	}

	// ── Handover ──────────────────────────────────────────────────────────────

	@Test
	fun `starting a capture releases keys the pad was holding`() {
		bindings.setControllerButtonCodes(MetaUiAction.CONFIRM, 3)
		MetaControllerListener.buttonDown(controller, 3)
		assertEquals(listOf(Input.Keys.ENTER), input.keysDown, "precondition: the press should have emitted a key")

		MetaControllerListener.captureButtons { _, _ -> }

		assertEquals(
			listOf(Input.Keys.ENTER),
			input.keysUp,
			"the held key was never released, so it stays down forever - the matching up is captured, not translated",
		)
		clearCapture()
	}

	@Test
	fun `releasing the top capture restores the one beneath it`() {
		// The lifetime this API advertises, and the one MetaInputProcessor's exclusive stack has: an inner modal
		// letting go must hand input back to the outer one, not to nobody. A single slot lost the outer capture the
		// moment the inner one released, and the pad silently resumed navigating the UI behind both.
		bindings.setControllerButtonCodes(MetaUiAction.CONFIRM, 3)
		var outerSaw = -1
		val outer: (Controller, Int) -> Unit = { _, code -> outerSaw = code }
		val inner: (Controller, Int) -> Unit = { _, _ -> }
		MetaControllerListener.captureButtons(outer)
		MetaControllerListener.captureButtons(inner)

		MetaControllerListener.releaseCapture(inner)
		MetaControllerListener.buttonDown(controller, 3)

		assertEquals(3, outerSaw, "the outer capture was not restored when the inner one released")
		assertTrue(input.keysDown.isEmpty(), "translation resumed while a capture was still registered")
		MetaControllerListener.buttonUp(controller, 3)
		MetaControllerListener.releaseCapture(outer)
	}

	@Test
	fun `a button held when a capture starts has its release consumed`() {
		// The press was translated before the capture existed and the release will not be, so without care the
		// release emits an unmatched canonical key-up once the capture ends - activating whatever is focused behind
		// the dialog the player was about to cancel.
		bindings.setControllerButtonCodes(MetaUiAction.CONFIRM, 3)
		MetaControllerListener.buttonDown(controller, 3)
		val target: (Controller, Int) -> Unit = { _, _ -> }
		MetaControllerListener.captureButtons(target)
		MetaControllerListener.releaseCapture(target)
		input.keysUp.clear()

		val consumed = MetaControllerListener.buttonUp(controller, 3)

		assertTrue(consumed, "a release owed from before the capture escaped it")
		assertTrue(input.keysUp.isEmpty(), "the held button emitted a second, unmatched key-up: ${input.keysUp}")
	}

	@Test
	fun `releasing a capture someone else owns does nothing`() {
		// Pop by owner, the way the exclusive processor stack does: a dialog closing late must not switch off a
		// capture that has since been taken over by another.
		val first: (Controller, Int) -> Unit = { _, _ -> }
		val second: (Controller, Int) -> Unit = { _, _ -> }
		MetaControllerListener.captureButtons(first)
		MetaControllerListener.captureButtons(second)

		MetaControllerListener.releaseCapture(first)

		assertTrue(MetaControllerListener.capturing, "the later capture was released by an earlier owner")
		MetaControllerListener.releaseCapture(second)
		assertFalse(MetaControllerListener.capturing, "the owner could not release its own capture")
	}

	// ── One canonical key, many devices ───────────────────────────────────────

	@Test
	fun `a second pad pressing the same button does not press the key twice`() {
		// The translation collapses every pad onto one keyboard-shaped stream, so the key is down while *anyone*
		// holds it. Emitting per device would send a second CONFIRM down that nothing had released.
		bindings.setControllerButtonCodes(MetaUiAction.CONFIRM, 3)
		val other = FakeController()

		MetaControllerListener.buttonDown(controller, 3)
		MetaControllerListener.buttonDown(other, 3)

		assertEquals(listOf(Input.Keys.ENTER), input.keysDown, "the second pad pressed the key again")
		MetaControllerListener.buttonUp(controller, 3)
		MetaControllerListener.buttonUp(other, 3)
	}

	@Test
	fun `the key stays down until the last pad holding it lets go`() {
		bindings.setControllerButtonCodes(MetaUiAction.CONFIRM, 3)
		val other = FakeController()
		MetaControllerListener.buttonDown(controller, 3)
		MetaControllerListener.buttonDown(other, 3)

		MetaControllerListener.buttonUp(controller, 3)
		assertTrue(input.keysUp.isEmpty(), "one pad releasing let go of a key another was still holding")

		MetaControllerListener.buttonUp(other, 3)
		assertEquals(listOf(Input.Keys.ENTER), input.keysUp, "the last release did not let the key up")
	}

	@Test
	fun `disconnecting one pad does not release a key another is holding`() {
		// The deferred half of the per-device work: `disconnected` released every pad's keys, so unplugging a second
		// controller let go of CONFIRM while the player was still holding it on the first.
		bindings.setControllerButtonCodes(MetaUiAction.CONFIRM, 3)
		val other = FakeController()
        MetaControllerListener.buttonDown(controller, 3)
		MetaControllerListener.buttonDown(other, 3)

		MetaControllerListener.disconnected(other)

		assertTrue(input.keysUp.isEmpty(), "a disconnect released a key a surviving pad was holding: ${input.keysUp}")
		MetaControllerListener.buttonUp(controller, 3)
		assertEquals(listOf(Input.Keys.ENTER), input.keysUp, "the surviving pad could no longer release the key")
	}

	@Test
	fun `disconnecting a pad releases the keys only it was holding`() {
		// The other half: no stuck keys either.
		bindings.setControllerButtonCodes(MetaUiAction.CONFIRM, 3)
		MetaControllerListener.buttonDown(controller, 3)

		MetaControllerListener.disconnected(controller)

		assertEquals(listOf(Input.Keys.ENTER), input.keysUp, "a disconnect left the pad's key stuck down")
	}

	// ── Doubles ───────────────────────────────────────────────────────────────

	private class RecordingInput : MetaInputProcessor {
		val keysDown = ArrayList<Int>()
		val keysUp = ArrayList<Int>()

		override fun keyDown(keycode: Int): Boolean {
			keysDown.add(keycode)
			return false
		}

		override fun keyUp(keycode: Int): Boolean {
			keysUp.add(keycode)
			return false
		}

		override var exclusiveProcessor: InputProcessor? = null
		override val isLeftCtrlDown: Boolean = false
		override val isRightCtrlDown: Boolean = false
		override val isLeftShiftDown: Boolean = false
		override val isRightShiftDown: Boolean = false
		override fun pushExclusiveProcessor(processor: InputProcessor) = Unit
		override fun popExclusiveProcessor(processor: InputProcessor): Boolean = false
		override fun clearExclusiveProcessors() = Unit
		override fun changeScreen() = Unit
		override fun addGlobalInputProcessor(inputProcessor: InputProcessor): InputProcessor = inputProcessor
		override fun removeGlobalInputProcessor(inputProcessor: InputProcessor): Boolean = false
		override fun addScreenInputProcessor(inputProcessor: InputProcessor): InputProcessor = inputProcessor
		override fun removeScreenInputProcessor(inputProcessor: InputProcessor): Boolean = false
		override fun addGlobalKeyListener(keycode: Int, millisRequired: Long, keyListener: KeyListener): KeyListener =
			keyListener

		override fun removeGlobalKeyListener(keycode: Int, keyListener: KeyListener): Boolean = false
		override fun addScreenKeyListener(keycode: Int, millisRequired: Long, keyListener: KeyListener): KeyListener =
			keyListener

		override fun removeScreenKeyListener(keycode: Int, keyListener: KeyListener): Boolean = false
		override fun addGlobalScrollListener(scrollListener: ScrollListener): ScrollListener = scrollListener
		override fun removeGlobalScrollListener(scrollListener: ScrollListener): Boolean = false
		override fun addScreenScrollListener(scrollListener: ScrollListener): ScrollListener = scrollListener
		override fun removeScreenScrollListener(scrollListener: ScrollListener): Boolean = false
		override fun keyTyped(character: Char): Boolean = false
		override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false
		override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false
		override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false
		override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = false
		override fun mouseMoved(screenX: Int, screenY: Int): Boolean = false
		override fun scrolled(amountX: Float, amountY: Float): Boolean = false
	}

	private class FakeController : Controller {
		val axes = FloatArray(8)
		private val mapping = FakeMapping()

		override fun getButton(buttonCode: Int): Boolean = false
		override fun getAxis(axisCode: Int): Float = axes.getOrElse(axisCode) { 0f }
		override fun getName(): String = "fake"
		override fun getUniqueId(): String = "fake"
		override fun getMinButtonIndex(): Int = 0
		override fun getMaxButtonIndex(): Int = 25
		override fun getAxisCount(): Int = axes.size
		override fun isConnected(): Boolean = true
		override fun canVibrate(): Boolean = false
		override fun isVibrating(): Boolean = false
		override fun startVibration(duration: Int, strength: Float) = Unit
		override fun cancelVibration() = Unit
		override fun supportsPlayerIndex(): Boolean = false
		override fun getPlayerIndex(): Int = Controller.PLAYER_IDX_UNSET
		override fun setPlayerIndex(index: Int) = Unit
		override fun getMapping(): ControllerMapping = mapping
		override fun getPowerLevel(): ControllerPowerLevel = ControllerPowerLevel.POWER_UNKNOWN
		override fun addListener(listener: ControllerListener) = Unit
		override fun removeListener(listener: ControllerListener) = Unit
	}

	private class FakeMapping : ControllerMapping(
		0, 1, 2, 3,
		10, 11, 12, 13,
		14, 15, 16, 17,
		18, 19, 20, 21,
		22, 23, 24, 25,
	)
}
