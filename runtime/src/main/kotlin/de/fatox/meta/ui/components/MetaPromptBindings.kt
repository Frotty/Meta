package de.fatox.meta.ui.components

import com.badlogic.gdx.Input
import de.fatox.meta.input.MetaInputDevice
import de.fatox.meta.input.MetaUiAction
import de.fatox.meta.input.MetaUiInputBindings

/** Resolves the first configured binding for each action. Reads the profile reactively inside [MetaPromptBar.bindEntries]. */
fun MetaUiInputBindings.promptGlyphs(device: MetaInputDevice, vararg actions: MetaUiAction): List<MetaInputGlyph> =
	actions.mapNotNull { action ->
		when (device) {
			MetaInputDevice.Keyboard -> keyboardKeysFor(action).firstOrNull { it in 1..Input.Keys.MAX_KEYCODE }?.let {
				MetaInputGlyphs.key(it, Input.Keys.toString(it) ?: "Key $it")
			}
			is MetaInputDevice.Gamepad -> controllerBindingsFor(action).firstNotNullOfOrNull { binding ->
				val semantic = binding.semanticButton
				val raw = binding.rawButtonCode
				when {
					semantic != null && semantic.code(device.controller) >= 0 -> MetaInputGlyphs.button(device.family, semantic)
					raw != null && raw >= 0 -> MetaInputGlyphs.button(device, raw)
					else -> null
				}
			}
		}
	}.distinct()
