package de.fatox.meta.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Timer
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.input.MetaUiAction
import de.fatox.meta.input.MetaUiInputBindings
import de.fatox.meta.reactive.ReactiveValue
import de.fatox.meta.reactive.effect
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.windows.MetaDialog
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
	private val uiBindings: MetaUiInputBindings by lazyInject()
	private val focusedActorSignal = signal<Actor?>(null) { a, b -> a === b }

	/** The widget currently focused by keyboard/controller navigation, as a reactive value (read it in an effect). */
	val focusedActor: ReactiveValue<Actor?> get() = focusedActorSignal

	private val clickPosition = Vector2()
	private val emptySelection = Actor()
	private var selectedActorBacking: Actor = emptySelection
	private var focusedRoot: Group? = null
	private var synthesizingCanonicalAction = false

	// Whether we are actively controlling UI focus
	var activated: Boolean = true
		set(value) {
			field = value
			if (value && canFocus(selectedActor)) {
				setFocusedActor(selectedActor)
			} else {
				setFocusedActor(null)
			}
		}

	var canMove: Boolean = true

	// The actor that is currently selected/focused
	var selectedActor: Actor
		get() = selectedActorBacking
		set(value) {
			selectActor(value, focus = true)
		}

	@Deprecated("Read focusedActor (a ReactiveValue) inside an effect instead.")
	fun addFocusListener(listener: (Actor?) -> Unit): () -> Unit {
		// Bridge the old listener API onto the signal: fires immediately with the current value and on each change.
		val handle = effect { listener(focusedActorSignal.value) }
		return handle::dispose
	}

	private fun setFocusedActor(actor: Actor?) {
		if (focusedActorSignal.peek() === actor) return
		metaUIRenderer.setFocusedActor(actor)
		focusedActorSignal.value = actor // notifies any effect observing focusedActor (incl. legacy bridges)
	}

	private fun selectActor(actor: Actor, focus: Boolean) {
		selectedActorBacking = actor
		if (!activated || !focus) return
		if (canFocus(actor)) {
			setFocusedActor(actor)
			scrollIfNeeded(actor)
		} else {
			setFocusedActor(null)
		}
	}

	fun focusFirstIn(root: Group, preferred: Actor? = null): Actor? {
		focusedRoot = root
		if (preferred != null && isNavigable(preferred) && preferred.isDescendantOf(root)) {
			selectedActor = preferred
			return preferred
		}
		val first = firstNavigable(root) ?: return null
		selectedActor = first
		return first
	}

	fun clearFocusIfInside(root: Group) {
		val focused = focusedActorSignal.peek()
		if (focused != null && focused.isDescendantOf(root)) setFocusedActor(null)
		if (selectedActor.isDescendantOf(root)) selectActor(emptySelection, focus = false)
		val scopedRoot = focusedRoot
		if (scopedRoot != null && scopedRoot.isDescendantOf(root)) focusedRoot = null
	}

	// We gather potential navigation targets from the parent's hierarchy
	private var targets = Array<Actor>()
	private val repeatTasks = ObjectMap<MetaUiAction, Timer.Task>()

	init {
		metaInput.addGlobalInputProcessor(object : InputAdapter() {
			override fun keyDown(keycode: Int): Boolean {
				if (synthesizingCanonicalAction) return false
				val action = uiBindings.actionForKey(keycode) ?: return false
				handleActionDown(action, keycode)
				return false
			}

			override fun keyUp(keycode: Int): Boolean {
				if (synthesizingCanonicalAction) return false
				val action = uiBindings.actionForKey(keycode) ?: return false
				handleActionUp(action, keycode)
				return false
			}
		})
	}

	private fun handleActionDown(action: MetaUiAction, keycode: Int) {
		if (isNavigationAction(action)) {
			navigate(action)
			scheduleRepeat(action)
		}
		synthesizeCanonicalKeyDown(action, keycode)
	}

	private fun handleActionUp(action: MetaUiAction, keycode: Int) {
		cancelRepeat(action)
		if (action == MetaUiAction.CONFIRM) activateSelectedActor()
		synthesizeCanonicalKeyUp(action, keycode)
	}

	private fun synthesizeCanonicalKeyDown(action: MetaUiAction, keycode: Int) {
		val canonicalKey = uiBindings.canonicalKeyFor(action)
		if (keycode == canonicalKey) return
		synthesizingCanonicalAction = true
		try {
			metaInput.keyDown(canonicalKey)
		} finally {
			synthesizingCanonicalAction = false
		}
	}

	private fun synthesizeCanonicalKeyUp(action: MetaUiAction, keycode: Int) {
		val canonicalKey = uiBindings.canonicalKeyFor(action)
		if (keycode == canonicalKey) return
		synthesizingCanonicalAction = true
		try {
			metaInput.keyUp(canonicalKey)
		} finally {
			synthesizingCanonicalAction = false
		}
	}

	private fun scheduleRepeat(action: MetaUiAction) {
		cancelRepeat(action)
		val task = object : Timer.Task() {
			override fun run() {
				// While an exclusive grab is active, MetaInput routes keyUp to the grab owner only, so cancelRepeat
				// would never be reached - self-cancel instead of stepping navigation forever behind the grab.
				if (metaInput.exclusiveProcessor != null) {
					cancel()
					return
				}
				navigate(action)
			}
		}
		repeatTasks.put(action, task)
		Timer.schedule(task, NAV_REPEAT_DELAY_SECONDS, NAV_REPEAT_SECONDS)
	}

	private fun cancelRepeat(action: MetaUiAction) {
		repeatTasks.remove(action)?.cancel()
	}

	private fun navigate(action: MetaUiAction) {
		if (!activated || !canMove) return
		selectedActor = when (action) {
			MetaUiAction.NAVIGATE_UP -> getNextY(up = true)
			MetaUiAction.NAVIGATE_DOWN -> getNextY(up = false)
			MetaUiAction.NAVIGATE_LEFT -> getNextX(left = true)
			MetaUiAction.NAVIGATE_RIGHT -> getNextX(left = false)
			else -> selectedActor
		}
	}

	private fun isNavigationAction(action: MetaUiAction): Boolean =
		action == MetaUiAction.NAVIGATE_UP ||
			action == MetaUiAction.NAVIGATE_DOWN ||
			action == MetaUiAction.NAVIGATE_LEFT ||
			action == MetaUiAction.NAVIGATE_RIGHT

	private fun activateSelectedActor() {
		if (
			!activated || selectedActor.stage == null ||
			Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) ||
			Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ||
			selectedActor is Disableable && (selectedActor as Disableable).isDisabled
		) return

		val localX = selectedActor.width * 0.5f
		val localY = selectedActor.height * 0.5f
		clickPosition.set(localX, localY)
		selectedActor.localToStageCoordinates(clickPosition)

		selectedActor.fire(InputEvent().apply {
			button = Input.Buttons.LEFT
			pointer = 0
			stageX = clickPosition.x
			stageY = clickPosition.y
			type = InputEvent.Type.touchDown
			stage = selectedActor.stage
		})
		selectedActor.fire(InputEvent().apply {
			button = Input.Buttons.LEFT
			pointer = 0
			stageX = clickPosition.x
			stageY = clickPosition.y
			type = InputEvent.Type.touchUp
			stage = selectedActor.stage
		})
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

			val scopedRoot = selectedActor.enclosingDialog()
				?: focusedRoot?.takeIf { selectedActor.isDescendantOf(it) }
			if (scopedRoot != null) {
				targetsInGroup(scopedRoot)
				return targets
			}

			var rootGroup = selectedActor.parent ?: selectedActor as? Group ?: return targets
			while (true) {
				targetsInGroup(rootGroup)
				rootGroup = rootGroup.parent ?: break
			}
			return targets
		}

	private fun targetsInGroup(group: Group) {
		for (actor in group.children) {
			if (actor.isVisible) {
				if (actor != selectedActor && isNavigable(actor) && !targets.contains(actor, true)) {
					targets.add(actor)
				} else if (actor is Group) {
					targetsInGroup(actor)
				}
			}
		}
	}

	private fun firstNavigable(group: Group): Actor? {
		for (actor in group.children) {
			if (!actor.isVisible) continue
			if (isNavigable(actor)) return actor
			if (actor is Group) firstNavigable(actor)?.let { return it }
		}
		return null
	}

	private fun canFocus(actor: Actor): Boolean =
		actor.isVisible &&
			actor.stage != null &&
			!(actor is Disableable && actor.isDisabled)

	private fun isNavigable(actor: Actor): Boolean {
		if (!canFocus(actor)) return false
		return when (actor) {
			is Button -> !actor.isDisabled
			is SelectBox<*> -> !actor.isDisabled
			is TextField -> !actor.isDisabled
			is MetaFocusable -> true
			else -> false
		}
	}

	private fun Actor.isDescendantOf(root: Group): Boolean {
		var actor: Actor? = this
		while (actor != null) {
			if (actor === root) return true
			actor = actor.parent
		}
		return false
	}

	private fun Actor.enclosingDialog(): MetaDialog? {
		var actor: Actor? = this
		while (actor != null) {
			if (actor is MetaDialog) return actor
			actor = actor.parent
		}
		return null
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
		val candidates = possibleTargets

		// Determine which side is relevant
		val currRight = selectedActor.rightEdgeOnStage()
		val currLeft = selectedActor.leftEdgeOnStage()

		// We'll compute the Euclidian distance from currentActor's "rightEdgeCenter" (or leftEdgeCenter)
		// to candidateActor's "leftEdgeCenter" (or rightEdgeCenter). This runs at repeat rate (5Hz) while a nav
		// key is held, so filtering and edge centers are computed in-place - no intermediate lists or vectors.
		if (left) selectedActor.leftEdgeCenterOnStage(navStart) else selectedActor.rightEdgeCenterOnStage(navStart)

		var best: Actor? = null
		var bestDist = Float.MAX_VALUE

		for (i in 0 until candidates.size) {
			val cand = candidates[i]
			if (cand is Disableable && cand.isDisabled) continue
			if (left) {
				if (cand.rightEdgeOnStage() > currLeft) continue
				cand.rightEdgeCenterOnStage(navEnd)
			} else {
				if (cand.leftEdgeOnStage() < currRight) continue
				cand.leftEdgeCenterOnStage(navEnd)
			}
			val dx = navEnd.x - navStart.x
			val dy = (navEnd.y - navStart.y) * 2f // Y-axis weighted more to prefer straight targets
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
		val candidates = possibleTargets

		val currTop = selectedActor.topEdgeOnStage()
		val currBottom = selectedActor.bottomEdgeOnStage()

		// See getNextX: filtering and edge centers are computed in-place to avoid per-step allocation.
		if (up) selectedActor.topEdgeCenterOnStage(navStart) else selectedActor.bottomEdgeCenterOnStage(navStart)

		var best: Actor? = null
		var bestDist = Float.MAX_VALUE

		for (i in 0 until candidates.size) {
			val cand = candidates[i]
			if (cand is Disableable && cand.isDisabled) continue
			if (up) {
				if (cand.bottomEdgeOnStage() < currTop) continue
				cand.bottomEdgeCenterOnStage(navEnd)
			} else {
				if (cand.topEdgeOnStage() > currBottom) continue
				cand.topEdgeCenterOnStage(navEnd)
			}
			val dx = (navEnd.x - navStart.x) * 2f // X-axis weighted more to prefer straight targets
			val dy = navEnd.y - navStart.y
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

	// Reusable endpoints for the navigation distance search (getNextX/getNextY). Separate from tmpVec, which the
	// edge helpers below clobber internally.
	private val navStart = Vector2()
	private val navEnd = Vector2()

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
	 * Center of the right edge in stage coordinates, written into [out] (no allocation).
	 */
	private fun Actor.rightEdgeCenterOnStage(out: Vector2): Vector2 {
		// x = rightEdge, y = vertical center
		return out.set(rightEdgeOnStage(), bottomEdgeOnStage() + height * 0.5f)
	}

	/**
	 * Center of the left edge in stage coordinates, written into [out] (no allocation).
	 */
	private fun Actor.leftEdgeCenterOnStage(out: Vector2): Vector2 {
		return out.set(leftEdgeOnStage(), bottomEdgeOnStage() + height * 0.5f)
	}

	/**
	 * Center of the top edge in stage coordinates, written into [out] (no allocation).
	 */
	private fun Actor.topEdgeCenterOnStage(out: Vector2): Vector2 {
		return out.set(leftEdgeOnStage() + width * 0.5f, topEdgeOnStage())
	}

	/**
	 * Center of the bottom edge in stage coordinates, written into [out] (no allocation).
	 */
	private fun Actor.bottomEdgeCenterOnStage(out: Vector2): Vector2 {
		return out.set(leftEdgeOnStage() + width * 0.5f, bottomEdgeOnStage())
	}

	private companion object {
		const val NAV_REPEAT_DELAY_SECONDS = 0.4f
		const val NAV_REPEAT_SECONDS = 0.2f
	}
}
