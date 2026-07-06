package de.fatox.meta.input

/**
 * Persistable representation of Meta UI menu/navigation bindings.
 *
 * Keep this class simple and mutable: libGDX Json can round-trip it without custom serializers, and games can store it
 * inside their own settings data if they do not want to use MetaData directly.
 */
class MetaUiInputProfile {
	var version = VERSION
	var horizontalAxis = 0
	var verticalAxis = 1
	var axisNavigationEnabled = true
	var keyboardBindings: Array<MetaUiKeyboardBindingProfile> = emptyArray()
	var controllerBindings: Array<MetaUiControllerBindingProfile> = emptyArray()

	companion object {
		const val VERSION = 1

		fun defaults(): MetaUiInputProfile = MetaUiInputBindings().toProfile()
	}
}

class MetaUiKeyboardBindingProfile {
	var action: String = ""
	var keycodes: IntArray = IntArray(0)

	constructor()

	constructor(action: MetaUiAction, keycodes: IntArray) {
		this.action = action.name
		this.keycodes = keycodes.copyOf()
	}
}

class MetaUiControllerBindingProfile {
	var action: String = ""
	var semanticButtons: Array<String> = emptyArray()
	var rawButtonCodes: IntArray = IntArray(0)

	constructor()

	constructor(action: MetaUiAction, bindings: List<MetaControllerButtonBinding>) {
		this.action = action.name
		val semanticButtons = ArrayList<String>(bindings.size)
		val rawButtonCodes = ArrayList<Int>(bindings.size)
		for (i in bindings.indices) {
			val binding = bindings[i]
			binding.semanticButton?.let { semanticButtons.add(it.name) }
			binding.rawButtonCode?.let { rawButtonCodes.add(it) }
		}
		this.semanticButtons = semanticButtons.toTypedArray()
		this.rawButtonCodes = rawButtonCodes.toIntArray()
	}
}
