package de.fatox.meta.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.addGlobalKeyListener
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

class UiControlHelper {
	private val metaInput: MetaInputProcessor by lazyInject()
	private val metaUIRenderer: UIRenderer by lazyInject()

	var activated: Boolean = true
	var canMove: Boolean = true

	private var targets = Array<Actor>()

	var selectedActor: Actor = Actor()
		set(value) {
			field = value
			metaUIRenderer.setFocusedActor(value)

			var parent = value.parent
			while (parent != null) {
				if (parent is ScrollPane) {
					helper.set(0f,0f)
					value.localToStageCoordinates(helper)
					parent.stageToLocalCoordinates(helper)
					parent.scrollTo(helper.x, helper.y, value.width, value.height)
					break
				}
				parent = parent.parent
			}
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

	private val selectedActorPos = Vector2()
	private val helper = Vector2()

	init {
		metaInput.addGlobalKeyListener(Input.Keys.RIGHT) {
			if (activated && canMove)
				selectedActor = getNextX(left = false)
		}
		metaInput.addGlobalKeyListener(Input.Keys.LEFT) {
			if (activated && canMove)
				selectedActor = getNextX(left = true)
		}
		metaInput.addGlobalKeyListener(Input.Keys.DOWN) {
			if (activated && canMove)
				selectedActor = getNextY(up = false)
		}
		metaInput.addGlobalKeyListener(Input.Keys.UP) {
			if (activated && canMove)
				selectedActor = getNextY(up = true)
		}
		metaInput.addGlobalKeyListener(Input.Keys.ENTER) {
			if (activated && !Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) && !Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
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
			if (actor != selectedActor && actor is Button && !targets.contains(actor, true)) {
				targets.add(actor)
			} else if (actor is Group) {
				targetsInGroup(actor)
			}
		}
	}

	private fun getNextX(left: Boolean): Actor {
		val possibleTargets = possibleTargets

		selectedActorPos.set(0f, 0f)
		selectedActor.localToStageCoordinates(selectedActorPos)
		var dist = Float.MAX_VALUE
		var selected: Actor? = null
		val iterator = possibleTargets.iterator()
		while (iterator.hasNext()) {

			val next = iterator.next()

			if ((next is Disableable && next.isDisabled)) {
				continue
			}
			helper.set(0f,0f)
			next.localToStageCoordinates(helper);
			var angleDeg = Math.atan2((helper.y - selectedActorPos.y).toDouble(), (helper.x - selectedActorPos.x).toDouble())
				.toFloat() * MathUtils.radiansToDegrees
			if (angleDeg < 0)
				angleDeg += 360
			if ((left && angleDeg > 135 && angleDeg < 225) || (!left && angleDeg >= 315 && angleDeg <= 360) || (!left && angleDeg >= 0 && angleDeg <= 45)) {
				helper.set(0f,0f)
				val dst2 = next.localToStageCoordinates(helper).dst2(selectedActorPos)
				if (dst2 < dist) {
					dist = dst2
					selected = next
				}
			}


		}
		selected?.let {
			return selected
		}
		return selectedActor
	}

	private fun getNextY(up: Boolean): Actor {
		val possibleTargets = possibleTargets

		selectedActorPos.set(0f, 0f)
		selectedActor.localToStageCoordinates(selectedActorPos)
		var dist = Float.MAX_VALUE
		var selected: Actor? = null
		val iterator = possibleTargets.iterator()
		while (iterator.hasNext()) {

			val next = iterator.next()

			if ((next is Disableable && next.isDisabled)) {
				continue
			}
			helper.set(0f,0f)
			next.localToStageCoordinates(helper)
			var angleDeg = Math.atan2((helper.y - selectedActorPos.y).toDouble(), (helper.x - selectedActorPos.x).toDouble())
				.toFloat() * MathUtils.radiansToDegrees
			if (angleDeg < 0)
				angleDeg += 360
			if ((up && angleDeg >= 45 && angleDeg <= 135) || (!up && angleDeg > 225 && angleDeg < 315)) {
				helper.set(0f,0f)
				val dst2 = next.localToStageCoordinates(helper).dst2(selectedActorPos)
				if (dst2 < dist) {
					dist = dst2
					selected = next
				}
			}


		}
		selected?.let {
			return selected
		}
		return selectedActor
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
