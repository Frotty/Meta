package de.fatox.meta.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.registerGlobalKeyListener
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import kotlin.math.abs

object UiControlHelper {
	private val metaInput: MetaInputProcessor by lazyInject()

	var activated = true
	var canMove = true

	private var targets = Array<Actor>()

	private val selectedColor: Color = Color.WHITE.cpy()
	var selectedActor: Actor = Actor()
		set(value) {
			field.color = selectedColor // TODO The color of the old actor should be set here?
			field = value
			selectedColor.set(value.color)
			val color = field.color
			if (color.r + color.g + color.b > 1.0f)
				color.add(-0.25f, -0.25f, 0.3f, 0.2f)
			else
				color.add(0.05f, 0.05f, 0.3f, 0.2f)
		}

	private val possibleTargets: Array<Actor>
		get() {
			targets.clear()
			if (!selectedActor.isVisible) {
				selectedActor = selectedActor.stage.actors[0]
			}
			var parent: Group? = if (selectedActor is Group) selectedActor as Group else selectedActor.parent
			while (parent != null) {
				targetsInGroup(parent)
				parent = parent.parent
			}
			return targets
		}

	private val helper = Vector2()

	init {
		metaInput.registerGlobalKeyListener(Input.Keys.RIGHT) {
			if (activated && canMove)
				selectedActor = getNextX(left = false)
		}
		metaInput.registerGlobalKeyListener(Input.Keys.LEFT) {
			if (activated && canMove)
				selectedActor = getNextX(left = true)
		}
		metaInput.registerGlobalKeyListener(Input.Keys.DOWN) {
			if (activated && canMove)
				selectedActor = getNextY(up = false)
		}
		metaInput.registerGlobalKeyListener(Input.Keys.UP) {
			if (activated && canMove)
				selectedActor = getNextY(up = true)
		}
		metaInput.registerGlobalKeyListener(Input.Keys.ENTER) {
			if (activated) {
				val inputEvent = InputEvent().apply {
					button = Input.Buttons.LEFT
					stageX = selectedActor.x
					stageY = selectedActor.y
					type = InputEvent.Type.touchDown
					listenerActor = selectedActor
					stage = selectedActor.stage
				}
				selectedActor.listeners.forEach { eventListener -> eventListener.handle(inputEvent) }
				inputEvent.type = InputEvent.Type.touchUp
				selectedActor.listeners.forEach { eventListener -> eventListener.handle(inputEvent) }
			}
		}
	}

	private fun targetsInGroup(t: Group) {
		for (actor in t.children) {
			if (actor is Button && !targets.contains(actor, true)) {
				targets.add(actor)
			} else if (actor is Group) {
				targetsInGroup(actor)
			}
		}
	}

	private fun getNextX(left: Boolean): Actor {
		val possibleTargets = possibleTargets

		possibleTargets.sort { a1, a2 ->
			val a1x = a1.localToStageCoordinates(helper).x
			val a2x = a2.localToStageCoordinates(helper).x
			(a2x - a1x).toInt()
		}
		val iterator = possibleTargets.iterator()
		while (iterator.hasNext()) {
			val next = iterator.next()
			if (abs(next.y - selectedActor.y) > selectedActor.height * 2.75f
				|| (next is Disableable && next.isDisabled)) {
				iterator.remove()
			}
		}
		return getNext(left, possibleTargets, possibleTargets.indexOf(selectedActor, true))
	}

	private fun getNextY(up: Boolean): Actor {
		val possibleTargets = possibleTargets

		possibleTargets.sort { a1, a2 ->
			val a1y = a1.localToStageCoordinates(helper).y
			val a2y = a2.localToStageCoordinates(helper).y
			(a2y - a1y).toInt()
		}
		val iterator = possibleTargets.iterator()
		while (iterator.hasNext()) {
			val next = iterator.next()
			if (abs(next.x - selectedActor.x) > selectedActor.width * 1.25f
				|| (next is Disableable && next.isDisabled)) {
				iterator.remove()
			}
		}
		return getNext(up, possibleTargets, possibleTargets.indexOf(selectedActor, true))
	}

	private fun getNext(left: Boolean, possibleTargets: Array<Actor>, index: Int): Actor {
		return when {
			possibleTargets.isEmpty || index < 0 -> selectedActor
			left -> {
				if (index == 0)
					possibleTargets.get(possibleTargets.size - 1)
				else
					possibleTargets.get(index - 1)
			}
			index == possibleTargets.size - 1 -> possibleTargets.get(0)
			else -> possibleTargets.get(index + 1)
		}
	}
}
