package de.fatox.meta.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Timer
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.addGlobalKeyListener
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.input.KeyListener
import kotlin.math.absoluteValue
import kotlin.math.sqrt

/**
 * A helper class for keyboard/controller-based navigation over Scene2d UI widgets
 * (Buttons, SelectBoxes, etc.). It sets the currently "selected" widget and can simulate
 * click events on ENTER.
 *
 * This version:
 *  - Removes any highlight rectangle logic (left to your UIRenderer).
 *  - Uses side-based *Euclidean* distance between edges, so that moving in a direction
 *    only considers widgets in that direction, and picks the one with the smallest
 *    true distance from edge-to-edge center.
 */
class UiControlHelper {
	private val metaInput: MetaInputProcessor by lazyInject()
	private val metaUIRenderer: UIRenderer by lazyInject()

	// Whether we are actively controlling UI focus
	var activated: Boolean = true
		set(value) {
			field = value
			if (value) {
				// Re-focus the current actor if visible
				if (selectedActor.isVisible) {
					metaUIRenderer.setFocusedActor(selectedActor)
				}
			} else {
				metaUIRenderer.setFocusedActor(null)
			}
		}

	var canMove: Boolean = true

	// The actor that is currently selected/focused
	var selectedActor: Actor = Actor()
		set(value) {
			field = value
			if (!activated) return
			metaUIRenderer.setFocusedActor(value)
			scrollIfNeeded(value)
		}

	// We gather potential navigation targets from the parent's hierarchy
	private var targets = Array<Actor>()

	init {
		// Arrow listeners to move focus
		metaInput.addGlobalKeyListener(
			Input.Keys.RIGHT, 0,
			object: KeyListener() {
				override fun onDown() {
					// Repeat if held down
					task = Timer.schedule(object : Timer.Task() {
						override fun run() {
							if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
								onEvent()
							} else {
								cancel()
							}
						}
					}, 0.4f, 0.2f)
				}

				override fun onEvent() {
					if (activated && canMove) {
						selectedActor = getNextX(left = false)
					}
				}
			}
		)

		metaInput.addGlobalKeyListener(
			Input.Keys.LEFT, 0,
			object: KeyListener() {
				override fun onDown() {
					task = Timer.schedule(object : Timer.Task() {
						override fun run() {
							if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
								onEvent()
							} else {
								cancel()
							}
						}
					}, 0.4f, 0.2f)
				}

				override fun onEvent() {
					if (activated && canMove) {
						selectedActor = getNextX(left = true)
					}
				}
			}
		)

		metaInput.addGlobalKeyListener(
			Input.Keys.DOWN, 0,
			object: KeyListener() {
				override fun onDown() {
					task = Timer.schedule(object : Timer.Task() {
						override fun run() {
							if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
								onEvent()
							} else {
								cancel()
							}
						}
					}, 0.4f, 0.2f)
				}

				override fun onEvent() {
					if (activated && canMove) {
						selectedActor = getNextY(up = false)
					}
				}
			}
		)

		metaInput.addGlobalKeyListener(
			Input.Keys.UP, 0,
			object: KeyListener() {
				override fun onDown() {
					task = Timer.schedule(object : Timer.Task() {
						override fun run() {
							if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
								onEvent()
							} else {
								cancel()
							}
						}
					}, 0.4f, 0.2f)
				}

				override fun onEvent() {
					if (activated && canMove) {
						selectedActor = getNextY(up = true)
					}
				}
			}
		)

		// Simulate clicking the selectedActor on ENTER
		metaInput.addGlobalKeyListener(Input.Keys.ENTER) {
			if (
				activated && selectedActor.stage != null &&
				!Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) &&
				!Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
			) {
				val inputEvent = InputEvent().apply {
					button = Input.Buttons.LEFT
					stageX = selectedActor.x
					stageY = selectedActor.y
					type = InputEvent.Type.touchDown
					listenerActor = selectedActor
					stage = selectedActor.stage
				}
				selectedActor.listeners.forEach { eventListener ->
					eventListener.handle(inputEvent)
				}
				inputEvent.type = InputEvent.Type.touchUp
				selectedActor.listeners.forEach { eventListener ->
					eventListener.handle(inputEvent)
				}
			}
		}
	}

	// --------------------------------------------------------------
	// SCROLL HANDLING (same as your original code)
	// --------------------------------------------------------------

	private val helper1 = Vector2()
	private val helper2 = Vector2()

	private fun scrollIfNeeded(value: Actor) {
		var parent = value.parent
		while (parent != null) {
			if (parent is ScrollPane) {
				helper1.set(0f, 0f)
				helper2.set(0f, 0f)
				parent.localToStageCoordinates(helper1)
				value.localToStageCoordinates(helper2)

				val yDiff1 = (helper2.y + value.height + 40f) - (helper1.y + parent.height)
				val yDiff2 = (helper2.y - value.height - 40f) - (helper1.y + parent.height)
				if (yDiff1 > 0) {
					parent.scrollY = parent.scrollY - yDiff1
					break
				}
				if (yDiff2.absoluteValue > parent.height) {
					parent.scrollY = parent.scrollY + (yDiff2.absoluteValue - parent.height)
					break
				}
				break
			}
			parent = parent.parent
		}
	}

	// --------------------------------------------------------------
	// POTENTIAL TARGETS
	// --------------------------------------------------------------

	/**
	 * All potential Buttons/SelectBoxes in the same parent lineage as selectedActor.
	 */
	private val possibleTargets: Array<Actor>
		get() {
			targets.clear()

			// If selectedActor is invisible but has a valid stage, fallback
			if (!selectedActor.isVisible && selectedActor.stage != null && selectedActor.stage.actors.size > 0) {
				selectedActor = selectedActor.stage.actors[0]
			}
			val rootGroup = if (selectedActor is Group) selectedActor as Group else selectedActor.parent
			var parent: Group? = rootGroup

			while (parent != null) {
				targetsInGroup(parent)
				parent = parent.parent
			}
			return targets
		}

	private fun targetsInGroup(group: Group) {
		for (actor in group.children) {
			if (actor.isVisible) {
				if (actor != selectedActor && (actor is Button || actor is SelectBox<*>) && !targets.contains(actor, true)) {
					targets.add(actor)
				} else if (actor is Group) {
					targetsInGroup(actor)
				}
			}
		}
	}

	// --------------------------------------------------------------
	// NAVIGATION: SIDE-BASED + EUCLIDEAN DISTANCE
	// --------------------------------------------------------------

	/**
	 * Return the next actor to the left or right.
	 *
	 * - If [left] = true, we only consider actors whose RIGHT edge is strictly less than
	 *   this actor's LEFT edge.
	 * - If [left] = false, we only consider actors whose LEFT edge is strictly greater
	 *   than this actor's RIGHT edge.
	 *
	 * Among those filtered, we pick the smallest Euclidean distance between the "side center"
	 * of the current actor and the "opposite side center" of the candidate.
	 */
	private fun getNextX(left: Boolean): Actor {
		val candidates = possibleTargets.filter { !(it is Disableable && it.isDisabled) }

		// Determine which side is relevant
		val currRight = selectedActor.rightEdgeOnStage()
		val currLeft = selectedActor.leftEdgeOnStage()

		val filtered = if (left) {
			candidates.filter { it.rightEdgeOnStage() <= currLeft }
		} else {
			candidates.filter { it.leftEdgeOnStage() >= currRight }
		}

		if (filtered.isEmpty()) return selectedActor

		// We'll compute the Euclidian distance from currentActor's "rightEdgeCenter" (or leftEdgeCenter)
		// to candidateActor's "leftEdgeCenter" (or rightEdgeCenter)
		val startPoint = if (left) selectedActor.leftEdgeCenterOnStage() else selectedActor.rightEdgeCenterOnStage()

		var best: Actor? = null
		var bestDist = Float.MAX_VALUE

		for (cand in filtered) {
			val endPoint = if (left) cand.rightEdgeCenterOnStage() else cand.leftEdgeCenterOnStage()
			val dx = endPoint.x - startPoint.x
			val dy = (endPoint.y - startPoint.y) * 2f // Y-axis weighted more to prefer straight targets
			val dist = sqrt(dx * dx + dy * dy)

			if (dist < bestDist) {
				bestDist = dist
				best = cand
			}
		}
		return best ?: selectedActor
	}

	/**
	 * Return the next actor up or down.
	 *
	 * - If [up] = true, we only consider actors whose bottom edge is strictly greater than
	 *   this actor's top edge.
	 * - If [up] = false, we only consider actors whose top edge is strictly less than
	 *   this actor's bottom edge.
	 *
	 * Among those filtered, we pick the smallest Euclidean distance between the "topEdgeCenter"
	 * or "bottomEdgeCenter" of the current actor and the corresponding "bottomEdgeCenter" or
	 * "topEdgeCenter" of the candidate.
	 */
	private fun getNextY(up: Boolean): Actor {
		val candidates = possibleTargets.filter { !(it is Disableable && it.isDisabled) }

		val currTop = selectedActor.topEdgeOnStage()
		val currBottom = selectedActor.bottomEdgeOnStage()

		val filtered = if (up) {
			candidates.filter { it.bottomEdgeOnStage() >= currTop }
		} else {
			candidates.filter { it.topEdgeOnStage() <= currBottom }
		}

		if (filtered.isEmpty()) return selectedActor

		val startPoint = if (up) selectedActor.topEdgeCenterOnStage() else selectedActor.bottomEdgeCenterOnStage()

		var best: Actor? = null
		var bestDist = Float.MAX_VALUE

		for (cand in filtered) {
			val endPoint = if (up) cand.bottomEdgeCenterOnStage() else cand.topEdgeCenterOnStage()
			val dx = (endPoint.x - startPoint.x) * 2f // X-axis weighted more to prefer straight targets
			val dy = endPoint.y - startPoint.y
			val dist = sqrt(dx * dx + dy * dy)

			if (dist < bestDist) {
				bestDist = dist
				best = cand
			}
		}
		return best ?: selectedActor
	}

	// --------------------------------------------------------------
	// EXTENSION-LIKE HELPERS FOR EDGE POSITIONS
	// --------------------------------------------------------------

	private val tmpVec = Vector2()

	/**
	 * Left edge in stage coordinates.
	 */
	private fun Actor.leftEdgeOnStage(): Float {
		tmpVec.set(0f, 0f)
		localToStageCoordinates(tmpVec)
		return tmpVec.x
	}

	/**
	 * Bottom edge in stage coordinates.
	 */
	private fun Actor.bottomEdgeOnStage(): Float {
		tmpVec.set(0f, 0f)
		localToStageCoordinates(tmpVec)
		return tmpVec.y
	}

	/**
	 * Right edge in stage coordinates (left edge + width).
	 */
	private fun Actor.rightEdgeOnStage(): Float {
		return leftEdgeOnStage() + width
	}

	/**
	 * Top edge in stage coordinates (bottom edge + height).
	 */
	private fun Actor.topEdgeOnStage(): Float {
		return bottomEdgeOnStage() + height
	}

	/**
	 * Center of the right edge in stage coordinates.
	 */
	private fun Actor.rightEdgeCenterOnStage(): Vector2 {
		// x = rightEdge, y = vertical center
		return Vector2(rightEdgeOnStage(), bottomEdgeOnStage() + height * 0.5f)
	}

	/**
	 * Center of the left edge in stage coordinates.
	 */
	private fun Actor.leftEdgeCenterOnStage(): Vector2 {
		return Vector2(leftEdgeOnStage(), bottomEdgeOnStage() + height * 0.5f)
	}

	/**
	 * Center of the top edge in stage coordinates.
	 */
	private fun Actor.topEdgeCenterOnStage(): Vector2 {
		return Vector2(leftEdgeOnStage() + width * 0.5f, topEdgeOnStage())
	}

	/**
	 * Center of the bottom edge in stage coordinates.
	 */
	private fun Actor.bottomEdgeCenterOnStage(): Vector2 {
		return Vector2(leftEdgeOnStage() + width * 0.5f, bottomEdgeOnStage())
	}
}
