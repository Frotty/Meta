package de.fatox.meta.graphics.animation

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Gdx2DPixmap
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FloatTextureData
import com.badlogic.gdx.utils.Array
import java.nio.ByteBuffer

object MockAnimation : Animation<TextureRegion>(
	Float.MAX_VALUE,
	Array(arrayOf(TextureRegion(Texture(
		Pixmap(Gdx2DPixmap(ByteBuffer.allocateDirect(0), longArrayOf(0,0,0,1)))
	))
)))