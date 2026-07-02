package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing

/** A texture-backed icon on Meta's generated button background. */
class MetaIconButton(drawable: Drawable?) : Button(MetaSkin.skin().get(MetaSkin.ICON_BUTTON, ButtonStyle::class.java)) {
	private val image = Image(drawable)

	init {
		add(image).size(24f).pad(MetaSpacing.XS)
	}
}
