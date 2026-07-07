package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import de.fatox.meta.ui.MetaSkin

/**
 * A compact, clickable image control intended for icon-only surfaces that should stay visually lighter than a full action
 * button. Use this when an image itself is the interaction target (e.g. toolbar toggles, list-row actions).
 */
class MetaImageButton(drawable: Drawable?) : MetaIconButton(drawable, MetaSkin.IMAGE_BUTTON)
