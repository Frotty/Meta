package de.fatox.meta.graphics.animation

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array

object EmptyAnimation {
	var animation: Animation<TextureRegion>? = null
	fun get(): Animation<TextureRegion> {
		if (animation == null) {
			val array = Array<TextureRegion?>()
			array.add(TextureRegion(Texture(0, 0, Pixmap.Format.RGBA8888)))
			animation = Animation<TextureRegion>(1f, array)
		}
		return animation!!
	}
}