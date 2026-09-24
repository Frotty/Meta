package de.fatox.meta.ui.components

import com.badlogic.gdx.Input

/** Kenney's input-prompt faces bundled with Meta; see `ui/prompts/LICENSE-kenney-input-prompts.txt`. */
enum class MetaGlyphSet(internal val fileName: String) {
	KEYBOARD("kenney_keyboard"),
	XBOX("kenney_xbox"),
	PLAYSTATION("kenney_playstation"),
	STEAM_DECK("kenney_steamdeck"),
}

/**
 * One glyph from Kenney's input prompts: a face and the glyph's name in it, e.g. `xbox_button_a` or `keyboard_enter`.
 *
 * <p>The names are Kenney's own, listed in `ui/prompts/<face>.tsv`. A glyph with an empty [name] and a [text] is for
 * a key Kenney does not draw: the key's name is set in its place, bright like a glyph, rather than inventing a keycap
 * that is not Kenney's.
 */
class MetaInputGlyph @JvmOverloads constructor(
	val set: MetaGlyphSet,
	val name: String,
	val text: String? = null,
)

/** The pad families whose buttons are drawn or labelled differently. */
enum class MetaPadFamily { XBOX, PLAYSTATION, NINTENDO, STEAM, GENERIC }

/** A face button by where it sits, which is what SDL reports and what a player's thumb finds. */
enum class MetaFaceButton { SOUTH, EAST, WEST, NORTH }

/**
 * Kenney glyphs for the buttons a game asks about.
 *
 * <p>Face buttons go by position. Kenney's packs here have no Nintendo face, so a Nintendo pad (and an unrecognised
 * one) is drawn with Xbox glyphs by position: the bottom button is drawn as Xbox A, which is the button a thumb finds
 * there, whatever the pad prints on it.
 */
object MetaInputGlyphs {
	private fun setOf(family: MetaPadFamily): MetaGlyphSet = when (family) {
		MetaPadFamily.PLAYSTATION -> MetaGlyphSet.PLAYSTATION
		MetaPadFamily.STEAM -> MetaGlyphSet.STEAM_DECK
		else -> MetaGlyphSet.XBOX
	}

	fun face(family: MetaPadFamily, button: MetaFaceButton): MetaInputGlyph {
		val set = setOf(family)
		val name = when (set) {
			MetaGlyphSet.PLAYSTATION -> "playstation_button_" + when (button) {
				MetaFaceButton.SOUTH -> "cross"
				MetaFaceButton.EAST -> "circle"
				MetaFaceButton.WEST -> "square"
				MetaFaceButton.NORTH -> "triangle"
			}
			MetaGlyphSet.STEAM_DECK -> "steamdeck_button_" + letter(button)
			else -> "xbox_button_" + letter(button)
		}
		return MetaInputGlyph(set, name)
	}

	private fun letter(button: MetaFaceButton): String = when (button) {
		MetaFaceButton.SOUTH -> "a"
		MetaFaceButton.EAST -> "b"
		MetaFaceButton.WEST -> "x"
		MetaFaceButton.NORTH -> "y"
	}

	/** The d-pad with its up and down arms marked, for vertical navigation. */
	fun dpadVertical(family: MetaPadFamily): MetaInputGlyph = when (setOf(family)) {
		MetaGlyphSet.PLAYSTATION -> MetaInputGlyph(MetaGlyphSet.PLAYSTATION, "playstation_dpad_vertical")
		MetaGlyphSet.STEAM_DECK -> MetaInputGlyph(MetaGlyphSet.STEAM_DECK, "steamdeck_dpad_vertical")
		else -> MetaInputGlyph(MetaGlyphSet.XBOX, "xbox_dpad_vertical")
	}

	/** A trigger ([shoulder] false) or bumper ([shoulder] true) on the given side. */
	fun trigger(family: MetaPadFamily, right: Boolean, shoulder: Boolean = false): MetaInputGlyph = when (setOf(family)) {
		MetaGlyphSet.PLAYSTATION -> MetaInputGlyph(
			MetaGlyphSet.PLAYSTATION,
			"playstation_trigger_" + (if (right) "r" else "l") + (if (shoulder) "1" else "2"),
		)
		MetaGlyphSet.STEAM_DECK -> MetaInputGlyph(
			MetaGlyphSet.STEAM_DECK,
			"steamdeck_button_" + (if (right) "r" else "l") + (if (shoulder) "1" else "2"),
		)
		else -> MetaInputGlyph(MetaGlyphSet.XBOX, "xbox_" + (if (right) "r" else "l") + (if (shoulder) "b" else "t"))
	}

	/** The menu / start / options button. */
	fun menu(family: MetaPadFamily): MetaInputGlyph = when (setOf(family)) {
		// The PS4 cut, which carries the word OPTIONS: the PS5 one is a bare pill that reads as nothing at prompt size.
		MetaGlyphSet.PLAYSTATION -> MetaInputGlyph(MetaGlyphSet.PLAYSTATION, "playstation4_button_options")
		MetaGlyphSet.STEAM_DECK -> MetaInputGlyph(MetaGlyphSet.STEAM_DECK, "steamdeck_button_options")
		else -> MetaInputGlyph(MetaGlyphSet.XBOX, "xbox_button_menu")
	}

	/** The up and down arrow keys together, for vertical navigation. */
	fun arrowsVertical(): MetaInputGlyph = MetaInputGlyph(MetaGlyphSet.KEYBOARD, "keyboard_arrows_vertical")

	/**
	 * The keycap for a libGDX key code. A key Kenney does not draw is shown as [fallbackText] - the caller's name for
	 * the key, which is also how a game gets that name into the player's language.
	 */
	fun key(keyCode: Int, fallbackText: String): MetaInputGlyph {
		val name = keyName(keyCode)
		return if (name != null) MetaInputGlyph(MetaGlyphSet.KEYBOARD, name)
		else MetaInputGlyph(MetaGlyphSet.KEYBOARD, "", fallbackText)
	}

	/** Kenney's name for a key, or null when the pack has no glyph for it. */
	internal fun keyName(keyCode: Int): String? {
		if (keyCode in Input.Keys.A..Input.Keys.Z) return "keyboard_" + ('a' + (keyCode - Input.Keys.A))
		if (keyCode in Input.Keys.NUM_0..Input.Keys.NUM_9) return "keyboard_" + (keyCode - Input.Keys.NUM_0)
		if (keyCode in Input.Keys.F1..Input.Keys.F12) return "keyboard_f" + (keyCode - Input.Keys.F1 + 1)
		return when (keyCode) {
			Input.Keys.UP -> "keyboard_arrow_up"
			Input.Keys.DOWN -> "keyboard_arrow_down"
			Input.Keys.LEFT -> "keyboard_arrow_left"
			Input.Keys.RIGHT -> "keyboard_arrow_right"
			Input.Keys.ENTER -> "keyboard_enter"
			Input.Keys.NUMPAD_ENTER -> "keyboard_numpad_enter"
			Input.Keys.ESCAPE -> "keyboard_escape"
			Input.Keys.SPACE -> "keyboard_space"
			Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT -> "keyboard_shift"
			Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT -> "keyboard_ctrl"
			Input.Keys.ALT_LEFT, Input.Keys.ALT_RIGHT -> "keyboard_alt"
			Input.Keys.TAB -> "keyboard_tab"
			Input.Keys.BACKSPACE -> "keyboard_backspace"
			Input.Keys.FORWARD_DEL -> "keyboard_delete"
			Input.Keys.INSERT -> "keyboard_insert"
			Input.Keys.HOME -> "keyboard_home"
			Input.Keys.END -> "keyboard_end"
			Input.Keys.PAGE_UP -> "keyboard_page_up"
			Input.Keys.PAGE_DOWN -> "keyboard_page_down"
			Input.Keys.CAPS_LOCK -> "keyboard_capslock"
			Input.Keys.COMMA -> "keyboard_comma"
			Input.Keys.PERIOD -> "keyboard_period"
			Input.Keys.SLASH -> "keyboard_slash_forward"
			Input.Keys.BACKSLASH -> "keyboard_slash_back"
			Input.Keys.SEMICOLON -> "keyboard_semicolon"
			Input.Keys.APOSTROPHE -> "keyboard_apostrophe"
			Input.Keys.LEFT_BRACKET -> "keyboard_bracket_open"
			Input.Keys.RIGHT_BRACKET -> "keyboard_bracket_close"
			Input.Keys.MINUS -> "keyboard_minus"
			Input.Keys.EQUALS -> "keyboard_equals"
			Input.Keys.GRAVE -> "keyboard_tilde"
			else -> null
		}
	}
}
