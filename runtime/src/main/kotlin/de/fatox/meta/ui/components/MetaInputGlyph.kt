package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color

/**
 * One thing a button prompt shows: a key, a face button, a shoulder, the d-pad or a stick.
 *
 * <p>Described rather than drawn, so a game says *what* a prompt is and [MetaInputPrompt] decides how it looks - one
 * look, sized and aligned the same way in every game on Meta. Text on a glyph (a key's name, a face button's letter)
 * is drawn in Meta's own font, which is why a key name passed here should already be in the player's language.
 */
sealed class MetaInputGlyph {
	/** A keyboard key: its name on a keycap. [arrows] draws up and down arrows instead of text. */
	class Key(val text: String, val arrows: Boolean = false) : MetaInputGlyph()

	/** A face button: a disc with a letter or a symbol, in the colour the pad prints it in. */
	class Face(val text: String?, val symbol: Symbol?, val color: Color) : MetaInputGlyph()

	/** A shoulder, trigger or menu button: its printed name on a keycap-shaped cap. */
	class Shoulder(val text: String) : MetaInputGlyph()

	object DPad : MetaInputGlyph()

	object Stick : MetaInputGlyph()

	enum class Symbol { CROSS, CIRCLE, SQUARE, TRIANGLE }
}

/** The pad families whose face buttons are labelled differently. */
enum class MetaPadFamily { XBOX, PLAYSTATION, NINTENDO, STEAM, GENERIC }

/** A face button by where it sits, which is what SDL reports and what a player's thumb finds. */
enum class MetaFaceButton { SOUTH, EAST, WEST, NORTH }

/**
 * The face buttons as each family prints them.
 *
 * <p>By position, not by letter: the bottom button is "A" on an Xbox pad, a cross on a PlayStation one and "B" on a
 * Nintendo one, and a prompt that says "A" to a Switch player points at the wrong button.
 */
object MetaInputGlyphs {
	private val XBOX_GREEN = Color.valueOf("6CC24AFF")
	private val XBOX_RED = Color.valueOf("E5483FFF")
	private val XBOX_BLUE = Color.valueOf("3D8CE8FF")
	private val XBOX_YELLOW = Color.valueOf("F2C230FF")
	private val PS_BLUE = Color.valueOf("7FB3EAFF")
	private val PS_RED = Color.valueOf("EB6B66FF")
	private val PS_PINK = Color.valueOf("D98FCBFF")
	private val PS_GREEN = Color.valueOf("4FCBA3FF")
	private val NEUTRAL = Color.valueOf("EEF0F5FF")

	fun face(family: MetaPadFamily, button: MetaFaceButton): MetaInputGlyph.Face = when (family) {
		MetaPadFamily.XBOX -> when (button) {
			MetaFaceButton.SOUTH -> MetaInputGlyph.Face("A", null, XBOX_GREEN)
			MetaFaceButton.EAST -> MetaInputGlyph.Face("B", null, XBOX_RED)
			MetaFaceButton.WEST -> MetaInputGlyph.Face("X", null, XBOX_BLUE)
			MetaFaceButton.NORTH -> MetaInputGlyph.Face("Y", null, XBOX_YELLOW)
		}
		MetaPadFamily.PLAYSTATION -> when (button) {
			MetaFaceButton.SOUTH -> MetaInputGlyph.Face(null, MetaInputGlyph.Symbol.CROSS, PS_BLUE)
			MetaFaceButton.EAST -> MetaInputGlyph.Face(null, MetaInputGlyph.Symbol.CIRCLE, PS_RED)
			MetaFaceButton.WEST -> MetaInputGlyph.Face(null, MetaInputGlyph.Symbol.SQUARE, PS_PINK)
			MetaFaceButton.NORTH -> MetaInputGlyph.Face(null, MetaInputGlyph.Symbol.TRIANGLE, PS_GREEN)
		}
		MetaPadFamily.NINTENDO -> when (button) {
			MetaFaceButton.SOUTH -> MetaInputGlyph.Face("B", null, NEUTRAL)
			MetaFaceButton.EAST -> MetaInputGlyph.Face("A", null, NEUTRAL)
			MetaFaceButton.WEST -> MetaInputGlyph.Face("Y", null, NEUTRAL)
			MetaFaceButton.NORTH -> MetaInputGlyph.Face("X", null, NEUTRAL)
		}
		MetaPadFamily.STEAM, MetaPadFamily.GENERIC -> when (button) {
			MetaFaceButton.SOUTH -> MetaInputGlyph.Face("A", null, NEUTRAL)
			MetaFaceButton.EAST -> MetaInputGlyph.Face("B", null, NEUTRAL)
			MetaFaceButton.WEST -> MetaInputGlyph.Face("X", null, NEUTRAL)
			MetaFaceButton.NORTH -> MetaInputGlyph.Face("Y", null, NEUTRAL)
		}
	}
}
