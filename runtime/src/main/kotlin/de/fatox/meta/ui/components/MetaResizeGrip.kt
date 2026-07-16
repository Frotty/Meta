package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSkin

/** Six-dot corner marker whose bounds match MetaWindow's bottom-right resize target. */
class MetaResizeGrip : WidgetGroup() {
	private val bottomLeft = dot()
	private val bottomCenter = dot()
	private val bottomRight = dot()
	private val middleCenter = dot()
	private val middleRight = dot()
	private val topRight = dot()

	init {
		touchable = Touchable.disabled
		addActor(bottomLeft)
		addActor(bottomCenter)
		addActor(bottomRight)
		addActor(middleCenter)
		addActor(middleRight)
		addActor(topRight)
	}

	override fun layout() {
		bottomLeft.setBounds(DOT_MARGIN, DOT_MARGIN, DOT_SIZE, DOT_SIZE)
		bottomCenter.setBounds(DOT_MARGIN + DOT_STEP, DOT_MARGIN, DOT_SIZE, DOT_SIZE)
		bottomRight.setBounds(DOT_MARGIN + DOT_STEP * 2f, DOT_MARGIN, DOT_SIZE, DOT_SIZE)
		middleCenter.setBounds(DOT_MARGIN + DOT_STEP, DOT_MARGIN + DOT_STEP, DOT_SIZE, DOT_SIZE)
		middleRight.setBounds(DOT_MARGIN + DOT_STEP * 2f, DOT_MARGIN + DOT_STEP, DOT_SIZE, DOT_SIZE)
		topRight.setBounds(DOT_MARGIN + DOT_STEP * 2f, DOT_MARGIN + DOT_STEP * 2f, DOT_SIZE, DOT_SIZE)
	}

	override fun getPrefWidth(): Float = GRIP_SIZE

	override fun getPrefHeight(): Float = GRIP_SIZE

	private companion object {
		const val GRIP_SIZE = 16f
		const val DOT_SIZE = 2f
		const val DOT_MARGIN = 2f
		const val DOT_STEP = 4f
		val dotTint: Color = MetaColor.TEXT.cpy().apply { a = 0.82f }

		fun dot(): Image = Image(MetaSkin.skin().getDrawable(MetaSkin.COLOR_FILL)).apply {
			touchable = Touchable.disabled
			color.set(dotTint)
		}
	}
}
