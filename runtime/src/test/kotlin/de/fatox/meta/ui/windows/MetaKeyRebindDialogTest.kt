package de.fatox.meta.ui.windows

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.input.MetaControllerListener
import de.fatox.meta.input.MetaRebindCapture
import de.fatox.meta.test.DispatchingInput
import de.fatox.meta.test.MetaHeadlessUi
import de.fatox.meta.test.RecordingUiManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What the rebind dialog captures, and what it lets go of.
 *
 * This is the test the dialog shipped without. It needed two things the harness did not have: an input processor that
 * dispatches, so a key can reach the exclusive grab the dialog pushes, and a `UIManager`, because `MetaDialog.close`
 * and detaching from the stage both call one and there was nothing to answer with.
 */
class MetaKeyRebindDialogTest {

	private lateinit var stage: Stage
	private lateinit var input: MetaInputProcessor
	private lateinit var dialog: MetaKeyRebindDialog

	@BeforeTest
	fun setUp() {
		MetaHeadlessUi.install(input = { DispatchingInput() }, uiManager = { RecordingUiManager() })
		stage = Stage(ScreenViewport())
		stage.viewport.update(1920, 1080, true)
		input = MetaInject.inject()
		dialog = MetaKeyRebindDialog()
		stage.addActor(dialog)
		dialog.show()
	}

	@AfterTest
	fun tearDown() {
		// The listener is a singleton object and outlives the test; a capture left set silences the pad for the next one.
		MetaControllerListener.clearCaptures()
		stage.dispose()
		MetaHeadlessUi.dispose()
	}

	@Test
	fun `nothing is captured until a key arrives`() {
		assertNull(dialog.captured.value, "the dialog reported a capture before anything was pressed")
	}

	@Test
	fun `a key press is captured and reported`() {
		input.keyDown(Input.Keys.F)

        val capture = dialog.captured.value
		assertIs<MetaRebindCapture.Key>(capture, "a key press was not reported as a key capture")
		assertEquals(Input.Keys.F, capture.keycode, "the wrong keycode was reported")
	}

	@Test
	fun `capturing closes the dialog and releases the grab`() {
		input.keyDown(Input.Keys.F)

		assertNull(input.exclusiveProcessor, "the exclusive grab outlived the capture that ended it")
		assertFalse(MetaControllerListener.capturing, "the controller capture outlived the dialog")
	}

	@Test
	fun `the cancel key does not become a binding`() {
		// It cannot: the pointer is dead under an exclusive grab, so this is the only way out of the dialog.
		input.keyDown(Input.Keys.ESCAPE)

		assertNull(dialog.captured.value, "the cancel key was recorded as a binding")
		assertNull(input.exclusiveProcessor, "cancelling left the grab in place")
	}

	@Test
	fun `the grab is taken while the dialog is up`() {
		assertTrue(input.exclusiveProcessor != null, "the dialog did not take the exclusive grab")
		assertTrue(MetaControllerListener.capturing, "the dialog did not suppress controller translation")
	}

	@Test
	fun `showing again forgets the previous capture`() {
		input.keyDown(Input.Keys.F)
		assertIs<MetaRebindCapture.Key>(dialog.captured.value, "precondition: the first capture should be recorded")

		stage.addActor(dialog)
		dialog.show()

		assertNull(dialog.captured.value, "a reused dialog still reported the binding from last time")
	}

	@Test
	fun `a keyboard-only dialog does not record a controller button`() {
		dialog.close()
		val keyboardOnly = MetaKeyRebindDialog(captureControllerButtons = false)
		stage.addActor(keyboardOnly)
		keyboardOnly.show()

		// Swallowed rather than ignored: leaving the pad uncaptured would let MetaControllerListener translate a bound
		// button into its canonical key, which the grab would then record as a *keyboard* binding.
		assertTrue(MetaControllerListener.capturing, "keyboard-only mode left the pad translating into the grab")
		assertNull(keyboardOnly.captured.value, "nothing was pressed yet")
		keyboardOnly.close()
	}
}
