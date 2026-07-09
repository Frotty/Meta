package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import de.fatox.meta.ui.MetaColor

/** Three-dot corner marker for resizable Meta windows. */
class MetaResizeGrip : WidgetGroup() {
	private val bottomLeft = dot()
	private val bottomRight = dot()
	private val topRight = dot()

	init {
		touchable = Touchable.disabled
		addActor(bottomLeft)
		addActor(bottomRight)
		addActor(topRight)
	}

	override fun layout() {
		bottomLeft.setBounds(DOT_MARGIN, DOT_MARGIN, DOT_SIZE, DOT_SIZE)
		bottomRight.setBounds(DOT_MARGIN + DOT_STEP, DOT_MARGIN, DOT_SIZE, DOT_SIZE)
		topRight.setBounds(DOT_MARGIN + DOT_STEP, DOT_MARGIN + DOT_STEP, DOT_SIZE, DOT_SIZE)
	}

	override fun getPrefWidth(): Float = GRIP_SIZE

	override fun getPrefHeight(): Float = GRIP_SIZE

	private companion object {
		const val GRIP_SIZE = 12f
		const val DOT_SIZE = 2f
		const val DOT_MARGIN = 2f
		const val DOT_STEP = 4f
		val dotTint: Color = MetaColor.TEXT.cpy().apply { a = 0.82f }

		val dotTexture: Texture by lazy {
			val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
			pixmap.setBlending(Pixmap.Blending.None)
			pixmap.setColor(Color.WHITE)
			pixmap.fill()
			Texture(pixmap).also {
				it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
				pixmap.dispose()
			}
		}
		val dotDrawable: TextureRegionDrawable by lazy {
			TextureRegionDrawable(TextureRegion(dotTexture))
		}

		fun dot(): Image = Image(dotDrawable).apply {
			touchable = Touchable.disabled
			color.set(dotTint)
		}
	}
}
