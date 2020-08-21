package de.fatox.meta.graphics.animation

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FloatTextureData
import com.badlogic.gdx.utils.Array

object MockAnimation : Animation<TextureRegion>(
	Float.MAX_VALUE,
	Array(arrayOf(TextureRegion(Texture(FloatTextureData(
		0,
		0,
		0,
		0,
		0,
		false
	)))))
)