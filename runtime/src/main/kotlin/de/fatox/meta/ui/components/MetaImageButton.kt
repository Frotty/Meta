package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import de.fatox.meta.ui.MetaSkin

/**
 * A compact, clickable image control intended for icon-only surfaces that should stay visually lighter than a full action
 * button. Use this when an image itself is the interaction target (e.g. toolbar toggles, list-row actions).
 */
class MetaImageButton : MetaIconButton {
	constructor(drawable: Drawable?) : super(drawable, MetaSkin.IMAGE_BUTTON)
	constructor(
		icon: String,
		size: Int = DEFAULT_ICON_SIZE.toInt(),
		color: Color? = Color.WHITE,
	) : super(icon, MetaSkin.IMAGE_BUTTON, size, color)
}
