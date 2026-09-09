package de.fatox.meta.assets

import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.api.model.MetaDisplayMode
import de.fatox.meta.api.model.MetaWindowData
import de.fatox.meta.api.ui.MetaDockLayoutData
import de.fatox.meta.input.MetaUiControllerBindingProfile
import de.fatox.meta.input.MetaUiInputProfile
import de.fatox.meta.input.MetaUiKeyboardBindingProfile
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import kotlin.test.assertTrue

/**
 * Pins the field names of everything Meta persists.
 *
 * libGDX `Json` writes a field under its own name, so renaming one is a silent data migration: the old value stops
 * being found, the object gets the default, and the next save writes that over the player's setting. Nothing about
 * that is visible at the rename - it compiles, the tests that use the class pass, and the loss only shows up on
 * someone's machine that has an older save.
 *
 * `AGENTS.md` has asked for persisted keys to be preserved for a long time; this is that rule with teeth. Adding a
 * field is fine and needs no change here - a new name simply is not pinned yet. Renaming or removing one fails, and
 * the fix is a deliberate decision about existing saves, which is exactly the conversation the failure should start.
 *
 * The same mechanism catches an obfuscator renaming fields, which is the trap waiting for anyone who turns on R8:
 * shrinking a reflection-serialised class resets every save in the field.
 */
class PersistedFieldNamesTest {
	@Test
	fun `persisted field names are pinned`() {
		val pinned: List<Pair<Class<*>, List<String>>> = listOf(
			MetaAudioVideoData::class.java to listOf(
				"profile", "hd", "resizeable", "borderless", "fullscreen", "x", "y", "width", "height",
				"displayMode", "vsyncEnabled", "maxFps", "videoDebug", "masterVolume", "musicVolume", "soundVolume",
				"metaDisplayMode", "runWithUI", "windowedBoundsInitialized", "maximized",
			),
			MetaDisplayMode::class.java to listOf(
				"width", "height", "refreshRate", "bitsPerPixel", "monitorIndex",
			),
			MetaWindowData::class.java to listOf(
				"name", "x", "y", "width", "height", "displayed", "dialog", "viewportWidth", "viewportHeight",
				"horizontalAnchor", "verticalAnchor", "horizontalDistance", "verticalDistance",
				"dockSide", "dockOrder", "dockHeight", "dockFill",
			),
			MetaDockLayoutData::class.java to listOf("leftWidth", "rightWidth"),
			MetaUiInputProfile::class.java to listOf(
				"version", "horizontalAxis", "verticalAxis", "axisNavigationEnabled",
				"keyboardBindings", "controllerBindings",
			),
			MetaUiKeyboardBindingProfile::class.java to listOf("action", "keycodes"),
			MetaUiControllerBindingProfile::class.java to listOf("action", "semanticButtons", "rawButtonCodes"),
		)

		val missing = ArrayList<String>()
		for (index in pinned.indices) {
			val (type, names) = pinned[index]
			val declared = type.declaredFields
				.filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
				.map { it.name }
				.toSet()
			for (nameIndex in names.indices) {
				val name = names[nameIndex]
				if (name !in declared) missing.add("${type.simpleName}.$name")
			}
		}

		assertTrue(
			missing.isEmpty(),
			"persisted field(s) renamed or removed: $missing. Every save already written uses the old name, so " +
				"this is a data migration, not a rename. Restore the name, or decide explicitly what happens to " +
				"existing saves and re-pin here.",
		)
	}
}
