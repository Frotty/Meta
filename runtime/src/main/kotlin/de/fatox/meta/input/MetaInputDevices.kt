package de.fatox.meta.input

import com.badlogic.gdx.controllers.Controller
import de.fatox.meta.reactive.ReactiveValue
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.components.MetaPadFamily

sealed interface MetaInputDevice {
	data object Keyboard : MetaInputDevice
	class Gamepad(val controller: Controller, val family: MetaPadFamily) : MetaInputDevice
}

/** Last device used. Pointer presses and scrolling select keyboard prompts. Report custom input on the GL thread. */
class MetaInputDevices {
	private val active = signal<MetaInputDevice>(MetaInputDevice.Keyboard)
	val activeDevice: ReactiveValue<MetaInputDevice> get() = active

	fun keyboardUsed() {
		active.value = MetaInputDevice.Keyboard
	}

	fun controllerUsed(controller: Controller) {
		if ((active.peek() as? MetaInputDevice.Gamepad)?.controller === controller) return
		val name = controller.name.orEmpty().lowercase()
		val family = when {
			"steam deck" in name || "steamdeck" in name -> MetaPadFamily.STEAM
			"playstation" in name || "dualsense" in name || "dualshock" in name || "sony" in name -> MetaPadFamily.PLAYSTATION
			"nintendo" in name || "joy-con" in name || "switch" in name -> MetaPadFamily.NINTENDO
			"xbox" in name || "xinput" in name -> MetaPadFamily.XBOX
			else -> MetaPadFamily.GENERIC
		}
		active.value = MetaInputDevice.Gamepad(controller, family)
	}

	fun controllerDisconnected(controller: Controller) {
		if ((active.peek() as? MetaInputDevice.Gamepad)?.controller === controller) keyboardUsed()
	}
}
