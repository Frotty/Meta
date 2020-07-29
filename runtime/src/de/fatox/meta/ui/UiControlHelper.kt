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
import de.fatox.meta.Meta
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.injection.Inject
import de.fatox.meta.injection.Singleton
import de.fatox.meta.input.KeyListener
import de.fatox.meta.input.MetaInput

@Singleton
class UiControlHelper {
    @Inject
    private lateinit var metaInput: MetaInput
    @Inject
    private lateinit var uiManager: UIManager

    private var selectedActor: Actor? = null
    private val selectedColor = Color.WHITE.cpy()
    var activated = true
	var canMove = true
    private var targets = Array<Actor>()

    private val possibleTargets: Array<Actor>
        get() {
            targets.clear()
            if (!selectedActor!!.isVisible) {
                selectedActor = selectedActor!!.stage.actors.get(0)
            }
            var parent: Group? = if (selectedActor is Group) selectedActor as Group? else selectedActor!!.parent
            while (parent != null) {
                targetsInGroup(parent)
                parent = parent.parent
            }
            return targets
        }

    private val helper = Vector2()

    init {
        Meta.inject(this)
        metaInput.registerGlobalKeyListener(Input.Keys.RIGHT, object : KeyListener() {
            override fun onEvent() {
                if (activated && canMove) {
                    setSelectedActor(getNextX(false))
                }
            }
        })
        metaInput.registerGlobalKeyListener(Input.Keys.LEFT, object : KeyListener() {
            override fun onEvent() {
                if (activated && canMove) {
                    setSelectedActor(getNextX(true))
                }
            }
        })
        metaInput.registerGlobalKeyListener(Input.Keys.DOWN, object : KeyListener() {
            override fun onEvent() {
                if (activated && canMove) {
                    setSelectedActor(getNextY(false))
                }
            }
        })
        metaInput.registerGlobalKeyListener(Input.Keys.UP, object : KeyListener() {
            override fun onEvent() {
                if (activated && canMove) {
                    setSelectedActor(getNextY(true))
                }
            }
        })
        metaInput.registerGlobalKeyListener(Input.Keys.ENTER, object : KeyListener() {
            override fun onEvent() {
                if (activated) {
                    val inputEvent = InputEvent()
                    inputEvent.button = Input.Buttons.LEFT
                    inputEvent.stageX = selectedActor!!.x
                    inputEvent.stageY = selectedActor!!.y
                    inputEvent.type = InputEvent.Type.touchDown
                    inputEvent.listenerActor = selectedActor
                    inputEvent.stage = selectedActor!!.stage
                    selectedActor!!.listeners.forEach { eventListener -> eventListener.handle(inputEvent) }
                    inputEvent.type = InputEvent.Type.touchUp
                    selectedActor!!.listeners.forEach { eventListener -> eventListener.handle(inputEvent) }
                }
            }
        })
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
            if (Math.abs(next.y - selectedActor!!.y) > selectedActor!!.height * 2.75f
				|| (next is Disableable && (next as Disableable).isDisabled)) {
                iterator.remove()
            }
        }
        val index = possibleTargets.indexOf(selectedActor, true)
        return getNext(left, possibleTargets, index)
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
            if (Math.abs(next.x - selectedActor!!.x) > selectedActor!!.width * 1.25f
				|| (next is Disableable && (next as Disableable).isDisabled)) {
                iterator.remove()
            }
        }
        val index = possibleTargets.indexOf(selectedActor, true)
        return getNext(up, possibleTargets, index)
    }

    private fun getNext(left: Boolean, possibleTargets: Array<Actor>, index: Int): Actor {
		if (possibleTargets.isEmpty) {
			return selectedActor!!
		}

		return if (left) {
            if (index == 0) {
                possibleTargets.get(possibleTargets.size - 1)
            } else {
                possibleTargets.get(index - 1)
            }
        } else {
            if (index == possibleTargets.size - 1) {
                possibleTargets.get(0)
            } else {
                possibleTargets.get(index + 1)
            }
        }
    }

    fun getSelectedActor(): Actor? {
        return selectedActor
    }

    fun setSelectedActor(selectedActor: Actor) {
        if (this.selectedActor != null) {
            this.selectedActor!!.color = selectedColor
        }
        this.selectedActor = selectedActor
        this.selectedColor.set(selectedActor.color)
		val color = this.selectedActor!!.color
		if (color.r + color.g + color.b > 1.0f) {
			this.selectedActor!!.color = this.selectedActor!!.color.add(-0.25f, -0.25f, 0.3f, 0.2f)
		} else {
			this.selectedActor!!.color = this.selectedActor!!.color.add(0.05f, 0.05f, 0.3f, 0.2f)
		}
    }

}
