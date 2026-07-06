package de.fatox.meta.input

import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.Controller
import java.util.EnumMap

enum class MetaUiAction {
	NAVIGATE_UP,
	NAVIGATE_DOWN,
	NAVIGATE_LEFT,
	NAVIGATE_RIGHT,
	CONFIRM,
	BACK,
}

enum class MetaControllerButton {
	A,
	B,
	X,
	Y,
	START,
	BACK,
	DPAD_UP,
	DPAD_DOWN,
	DPAD_LEFT,
	DPAD_RIGHT;

	fun code(controller: Controller): Int {
		val mapping = controller.mapping
		return when (this) {
			A -> mapping.buttonA
			B -> mapping.buttonB
			X -> mapping.buttonX
			Y -> mapping.buttonY
			START -> mapping.buttonStart
			BACK -> mapping.buttonBack
			DPAD_UP -> mapping.buttonDpadUp
			DPAD_DOWN -> mapping.buttonDpadDown
			DPAD_LEFT -> mapping.buttonDpadLeft
			DPAD_RIGHT -> mapping.buttonDpadRight
		}
	}
}

class MetaControllerButtonBinding private constructor(
	val semanticButton: MetaControllerButton?,
	val rawButtonCode: Int?,
) {
	fun matches(controller: Controller, buttonCode: Int): Boolean =
		rawButtonCode == buttonCode || semanticButton?.code(controller) == buttonCode

	companion object {
		fun semantic(button: MetaControllerButton): MetaControllerButtonBinding =
			MetaControllerButtonBinding(button, null)

		fun raw(buttonCode: Int): MetaControllerButtonBinding =
			MetaControllerButtonBinding(null, buttonCode)
	}
}

/**
 * Configurable keyboard/controller bindings for Meta's built-in UI navigation.
 *
 * Games can keep the defaults, replace them from a settings screen, or mirror their own input profile into this
 * object. Controller mappings are semantic by default (`A`, `B`, d-pad, etc.) so they follow libGDX's per-device
 * mapping, with raw button-code bindings available for special cases.
 */
class MetaUiInputBindings {
	var horizontalAxis = 0
	var verticalAxis = 1
	var axisNavigationEnabled = true

	private val keyboardBindings = EnumMap<MetaUiAction, IntArray>(MetaUiAction::class.java)
	private val controllerBindings = EnumMap<MetaUiAction, List<MetaControllerButtonBinding>>(MetaUiAction::class.java)

	init {
		resetDefaults()
	}

	fun resetDefaults() {
		setKeyboardKeys(MetaUiAction.NAVIGATE_UP, Input.Keys.UP)
		setKeyboardKeys(MetaUiAction.NAVIGATE_DOWN, Input.Keys.DOWN)
		setKeyboardKeys(MetaUiAction.NAVIGATE_LEFT, Input.Keys.LEFT)
		setKeyboardKeys(MetaUiAction.NAVIGATE_RIGHT, Input.Keys.RIGHT)
		setKeyboardKeys(MetaUiAction.CONFIRM, Input.Keys.ENTER)
		setKeyboardKeys(MetaUiAction.BACK, Input.Keys.ESCAPE)

		setControllerButtons(MetaUiAction.NAVIGATE_UP, MetaControllerButton.DPAD_UP)
		setControllerButtons(MetaUiAction.NAVIGATE_DOWN, MetaControllerButton.DPAD_DOWN)
		setControllerButtons(MetaUiAction.NAVIGATE_LEFT, MetaControllerButton.DPAD_LEFT)
		setControllerButtons(MetaUiAction.NAVIGATE_RIGHT, MetaControllerButton.DPAD_RIGHT)
		setControllerButtons(MetaUiAction.CONFIRM, MetaControllerButton.A, MetaControllerButton.START)
		setControllerButtons(MetaUiAction.BACK, MetaControllerButton.B, MetaControllerButton.BACK)
	}

	fun setKeyboardKeys(action: MetaUiAction, vararg keycodes: Int) {
		keyboardBindings[action] = keycodes.copyOf()
	}

	fun setControllerButtons(action: MetaUiAction, vararg buttons: MetaControllerButton) {
		controllerBindings[action] = buttons.map { MetaControllerButtonBinding.semantic(it) }
	}

	fun setControllerButtonCodes(action: MetaUiAction, vararg buttonCodes: Int) {
		controllerBindings[action] = buttonCodes.map { MetaControllerButtonBinding.raw(it) }
	}

	fun setControllerBindings(action: MetaUiAction, vararg bindings: MetaControllerButtonBinding) {
		controllerBindings[action] = bindings.toList()
	}

	fun setControllerBindings(action: MetaUiAction, bindings: List<MetaControllerButtonBinding>) {
		controllerBindings[action] = bindings.toList()
	}

	fun keyboardKeysFor(action: MetaUiAction): IntArray =
		keyboardBindings[action]?.copyOf() ?: IntArray(0)

	fun controllerBindingsFor(action: MetaUiAction): List<MetaControllerButtonBinding> =
		controllerBindings[action] ?: emptyList()

	fun toProfile(): MetaUiInputProfile {
		val profile = MetaUiInputProfile()
		profile.horizontalAxis = horizontalAxis
		profile.verticalAxis = verticalAxis
		profile.axisNavigationEnabled = axisNavigationEnabled
		profile.keyboardBindings = Array(MetaUiAction.entries.size) { i ->
			val action = MetaUiAction.entries[i]
			MetaUiKeyboardBindingProfile(action, keyboardKeysFor(action))
		}
		profile.controllerBindings = Array(MetaUiAction.entries.size) { i ->
			val action = MetaUiAction.entries[i]
			MetaUiControllerBindingProfile(action, controllerBindingsFor(action))
		}
		return profile
	}

	fun applyProfile(profile: MetaUiInputProfile): MetaUiInputBindings {
		resetDefaults()
		horizontalAxis = profile.horizontalAxis
		verticalAxis = profile.verticalAxis
		axisNavigationEnabled = profile.axisNavigationEnabled
		for (i in profile.keyboardBindings.indices) {
			val binding = profile.keyboardBindings[i]
			val action = binding.action.toUiActionOrNull() ?: continue
			setKeyboardKeys(action, *binding.keycodes)
		}
		for (i in profile.controllerBindings.indices) {
			val binding = profile.controllerBindings[i]
			val action = binding.action.toUiActionOrNull() ?: continue
			val bindings = ArrayList<MetaControllerButtonBinding>(
				binding.semanticButtons.size + binding.rawButtonCodes.size
			)
			for (buttonIndex in binding.semanticButtons.indices) {
				binding.semanticButtons[buttonIndex].toControllerButtonOrNull()
					?.let { bindings.add(MetaControllerButtonBinding.semantic(it)) }
			}
			for (buttonIndex in binding.rawButtonCodes.indices) {
				bindings.add(MetaControllerButtonBinding.raw(binding.rawButtonCodes[buttonIndex]))
			}
			setControllerBindings(action, bindings)
		}
		return this
	}

	fun actionForKey(keycode: Int): MetaUiAction? {
		for (action in MetaUiAction.entries) {
			val keys = keyboardBindings[action] ?: continue
			for (i in keys.indices) if (keys[i] == keycode) return action
		}
		return null
	}

	fun actionForButton(controller: Controller, buttonCode: Int): MetaUiAction? {
		for (action in MetaUiAction.entries) {
			val bindings = controllerBindings[action] ?: continue
			for (i in bindings.indices) if (bindings[i].matches(controller, buttonCode)) return action
		}
		return null
	}

	/**
	 * Canonical keycode emitted by controller input for this UI action. These are stable internal UI events, while
	 * user-facing keyboard bindings are read through [actionForKey].
	 */
	fun canonicalKeyFor(action: MetaUiAction): Int =
		when (action) {
			MetaUiAction.NAVIGATE_UP -> Input.Keys.UP
			MetaUiAction.NAVIGATE_DOWN -> Input.Keys.DOWN
			MetaUiAction.NAVIGATE_LEFT -> Input.Keys.LEFT
			MetaUiAction.NAVIGATE_RIGHT -> Input.Keys.RIGHT
			MetaUiAction.CONFIRM -> Input.Keys.ENTER
			MetaUiAction.BACK -> Input.Keys.ESCAPE
		}

	private fun String.toUiActionOrNull(): MetaUiAction? {
		for (action in MetaUiAction.entries) if (action.name == this) return action
		return null
	}

	private fun String.toControllerButtonOrNull(): MetaControllerButton? {
		for (button in MetaControllerButton.entries) if (button.name == this) return button
		return null
	}
}
