package de.fatox.meta.ui.components

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.utils.Align
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType

object MetaTooltip {
	private val tmp = Vector2()

	fun attach(target: Actor?, text: String, align: Int = Align.center) {
		if (target == null) return
		val tooltip = MetaTable().apply {
			background = MetaSkin.skin().getDrawable("meta.tooltip")
			add(MetaLabel(text, MetaType.CAPTION, MetaColor.TEXT)).pad(MetaSpacing.SM)
			pack()
			isVisible = false
		}
		target.addListener(object : InputListener() {
			override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
				if (pointer != -1) return
				val stage = target.stage ?: return
				if (tooltip.stage == null) stage.addActor(tooltip)
				target.localToStageCoordinates(tmp.set(x, y))
				when (align) {
					Align.left -> tooltip.setPosition(tmp.x - tooltip.width - MetaSpacing.SM, tmp.y)
					Align.right -> tooltip.setPosition(tmp.x + MetaSpacing.SM, tmp.y)
					else -> tooltip.setPosition(tmp.x - tooltip.width * 0.5f, tmp.y + MetaSpacing.SM)
				}
				tooltip.isVisible = true
				tooltip.toFront()
			}

			override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
				if (pointer != -1) return
				tooltip.isVisible = false
				tooltip.remove()
			}
		})
	}
}
