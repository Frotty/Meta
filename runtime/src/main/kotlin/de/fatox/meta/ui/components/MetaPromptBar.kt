package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaType

/**
 * A row of button prompts on a floating pill - the "[A] Select  [B] Back" strip along the bottom of a game screen.
 *
 * <p>The pill is rounded all round because it floats above the screen edge; the edge-docked [MetaBottomBar] has a
 * flat bottom that reads as clipped the moment it is lifted off the edge, which is what games did with it. Spacing
 * between prompts and the pill's padding are both taken from the font size, so a prompt bar scaled up for a TV is the
 * same bar, not a bigger font in the same box.
 */
class MetaPromptBar @JvmOverloads constructor(
	private var fontSize: Int = MetaType.BODY,
	private val labelColor: Color = MetaColor.TEXT_MUTED,
) : Table(MetaSkin.skin()) {
	/** One prompt of the bar: the glyphs and what they do. */
	class Entry(val glyphs: List<MetaInputGlyph>, val label: String)

	private var entries: List<Entry> = emptyList()

	init {
		background = MetaSkin.skin().getDrawable(MetaSkin.PROMPT_BAR)
		touchable = Touchable.disabled
		align(Align.center)
		applyPadding()
	}

	/** Replaces the prompts. Cheap enough to call on every change of controller family or language. */
	fun setEntries(next: List<Entry>) {
		entries = next
		rebuild()
	}

	fun setFontSize(size: Int) {
		if (size == fontSize) return
		fontSize = size
		applyPadding()
		rebuild()
	}

	private fun applyPadding() {
		val v = fontSize * 0.45f
		val h = fontSize * 0.9f
		pad(v, h, v, h)
	}

	private fun rebuild() {
		clearChildren()
		for (index in entries.indices) {
			val entry = entries[index]
			val cell = add(MetaInputPrompt(entry.glyphs, entry.label, fontSize, labelColor))
			if (index > 0) cell.padLeft(fontSize * 1.3f)
		}
		invalidateHierarchy()
	}
}
