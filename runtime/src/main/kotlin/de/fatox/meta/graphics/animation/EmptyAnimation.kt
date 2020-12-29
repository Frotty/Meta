package de.fatox.meta.graphics.animation

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array

object EmptyAnimation : Animation<TextureRegion>(
	1f,
	Array<TextureRegion>(arrayOf(TextureRegion(Texture(0, 0, Pixmap.Format.RGBA8888))))
)