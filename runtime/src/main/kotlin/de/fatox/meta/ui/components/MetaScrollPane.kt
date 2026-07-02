package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import de.fatox.meta.ui.MetaSkin

/**
 * A [ScrollPane] with a faster, more usable mouse-wheel step than scene2d's default and the VisUI `"list"` style
 * applied by default. Use this instead of a raw `ScrollPane`/`VisScrollPane` for the consistent Meta scroll feel.
 */
class MetaScrollPane : ScrollPane {
	constructor(widget: Actor?, style: ScrollPaneStyle) : super(widget, style)
	constructor(widget: Actor?, styleName: String?) : super(widget, MetaSkin.skin(), styleName)
	constructor(widget: Actor?) : super(widget, MetaSkin.skin(), defaultStyleName())

	override fun getMouseWheelY(): Float = MOUSE_WHEEL_STEP

	private companion object {
		const val MOUSE_WHEEL_STEP = 100f

		fun defaultStyleName(): String =
			if (MetaSkin.skin().has(MetaSkin.SCROLL_PANE, ScrollPaneStyle::class.java)) MetaSkin.SCROLL_PANE else "list"
	}
}
