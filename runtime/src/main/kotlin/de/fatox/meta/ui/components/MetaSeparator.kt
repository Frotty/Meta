package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.ui.Image
import de.fatox.meta.ui.MetaSkin

/** Thin Meta separator actor; size it through the table cell (`growX`, `growY`, `height`, `width`). */
class MetaSeparator @JvmOverloads constructor(
	drawableName: String = MetaSkin.SEPARATOR,
) : Image(MetaSkin.skin().getDrawable(drawableName))
