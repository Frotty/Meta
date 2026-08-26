package de.fatox.meta.ui.windows

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.controllers.Controller
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.input.MetaControllerListener
import de.fatox.meta.input.MetaRebindCapture
import de.fatox.meta.reactive.ReactiveValue
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaType
import de.fatox.meta.ui.components.MetaLabel

/**
 * Captures one control - a key or a controller button - and reports what it was.
 *
 * ```
 * val dialog = uiManager.showDialog<MetaKeyRebindDialog>()
 * scope.effect("rebind") {
 *     when (val capture = dialog.captured()) {
 *         is MetaRebindCapture.Key -> profile.bindKey(action, capture.keycode)
 *         is MetaRebindCapture.Button -> profile.bindButton(action, capture.buttonCode)
 *         null -> Unit // still waiting, or cancelled
 *     }
 * }
 * ```
 *
 * ### Why it takes the whole of input
 *
 * A rebinding screen is the one place where a keystroke must not mean what it usually means: the point of pressing
 * ENTER here is to bind ENTER, not to confirm. So the dialog pushes an exclusive processor, which routes every key to
 * it and past the stage.
 *
 * The consequence to know about is that the **pointer is dead while capturing** - an exclusive grab bypasses the
 * scene2d stage, so the title bar's close button cannot be clicked. That is why [cancelKeycode] exists and why the
 * prompt names it. It is also the one control that cannot be bound here, which is the usual trade in a rebinding
 * screen and the honest one: a working escape hatch matters more than rebinding the escape hatch to itself.
 *
 * ### Why the controller needs its own arrangement
 *
 * An exclusive processor is an `InputProcessor`, and a controller is not one. Pad buttons reach the UI through
 * [MetaControllerListener], which turns them into canonical keys - so without further care, pressing A while this
 * dialog is capturing would arrive as ENTER and be recorded as a *keyboard* binding.
 * [MetaControllerListener.captureButtons] both delivers the raw code and suppresses that translation, and is released
 * by owner so a dialog closing late cannot switch off a capture that has since been taken over.
 *
 * ### The result
 *
 * [captured] is reactive rather than a callback, because that is how Meta carries state. It is `null` until something
 * is captured and stays `null` if the dialog is cancelled, so a caller reads it in an effect and acts once. It resets
 * on every [show], so a dialog reused for a second binding never reports the first one again.
 */
class MetaKeyRebindDialog @JvmOverloads constructor(
	/** The key that cancels instead of binding. Named in the prompt, and unbindable here as a result. */
	private val cancelKeycode: Int = Input.Keys.ESCAPE,
	/**
	 * Whether a controller button is *recorded* as the binding. When false the pad is still captured and its presses
	 * swallowed - see [buttonTarget] for why ignoring it is not the same as switching it off.
	 */
	private val captureControllerButtons: Boolean = true,
) : MetaDialog("Rebind Key", true) {

	private val capturedSignal = signal<MetaRebindCapture?>(null)

	/** What was captured, or `null` while waiting and after a cancel. */
	val captured: ReactiveValue<MetaRebindCapture?> get() = capturedSignal

	private val metaInput: MetaInputProcessor by lazyInject()

	private var rebindProcessor: RebindProcessor? = null

	/**
	 * Held in a field, not passed as `::onButton`. A method reference allocates a fresh object per evaluation, so
	 * releasing by identity would never match what was registered and every dialog would leak its capture.
	 */
	private val buttonTarget: (Controller, Int) -> Unit = { controller, buttonCode ->
		if (captureControllerButtons) {
			finish(MetaRebindCapture.Button(controller, buttonCode))
		}
		// Otherwise swallowed, and swallowing is the whole point of installing a capture in keyboard-only mode.
		// Leaving the pad uncaptured does not make it inert: MetaControllerListener would still translate a bound
		// button into its canonical key, so pressing A would reach the exclusive processor as ENTER and be recorded
		// as a *keyboard* binding. A mode that only accepts keys has to suppress the pad, not ignore it.
	}

	init {
		// The prompt has to name what this mode actually accepts. In keyboard-only mode a button press is swallowed
		// and the dialog stays open, so inviting one leaves it looking broken to anyone who follows the instruction.
		val accepts = if (captureControllerButtons) "a key or button" else "a key"
		contentTable.add(
			MetaLabel("Press $accepts to bind. ${Input.Keys.toString(cancelKeycode)} cancels.", MetaType.BODY),
		).center()
	}

	override fun show() {
		// Before the grab, so a dialog reused for a second binding cannot be read as still holding the first.
		capturedSignal.value = null
		super.show()
		// Guard against double show: only ever hold ONE pushed grab, so onHidden can pop exactly what we pushed.
		if (rebindProcessor == null) {
			rebindProcessor = RebindProcessor(this).also { metaInput.pushExclusiveProcessor(it) }
		}
		// Unconditional: see buttonTarget. Both modes need the pad captured; they differ in what they do with it.
		MetaControllerListener.captureButtons(buttonTarget)
	}

	/**
	 * Critical: release both grabs so input returns to the rest of the UI once this dialog is gone, no matter how it
	 * closed. Popped by owner rather than peek-and-pop-top, so a grab nested on top of ours does not bury and leak our
	 * processor.
	 */
	override fun onHidden() {
		rebindProcessor?.let { metaInput.popExclusiveProcessor(it) }
		rebindProcessor = null
		MetaControllerListener.releaseCapture(buttonTarget)
	}

	private fun finish(capture: MetaRebindCapture) {
		capturedSignal.value = capture
		close()
	}

	/**
	 * Captures on key *down*, not up.
	 *
	 * The dialog is usually opened by a confirm, and `UiControlHelper` raises a confirm on key-up - so the opening key
	 * is already released by the time this is installed, and there is no stale release to mistake for a choice.
	 * Waiting for an up would also mean a held key never resolves.
	 */
	class RebindProcessor(private val dialog: MetaKeyRebindDialog) : InputAdapter() {
		override fun keyDown(keycode: Int): Boolean {
			if (keycode == dialog.cancelKeycode) {
				dialog.close()
				return true
			}
			dialog.finish(MetaRebindCapture.Key(keycode))
			return true
		}
	}
}
