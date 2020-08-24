package de.fatox.meta.ui.components

/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.utils.TimeUtils
import kotlin.math.abs

/**
 * Detects mouse over, mouse or finger touch presses, and clicks on an actor. A touch must go down over the actor and is
 * considered pressed as long as it is over the actor or within the [tap square][tapSquareSize]. This behavior makes it
 * easier to press buttons on a touch interface when the initial touch happens near the edge of the actor. Double clicks
 * can be detected using [tapCount]. Any touch (not just the first) will trigger this listener. While pressed, other
 * touch downs are ignored.
 *
 * @author Nathan Sweet
 * @property button The button to listen for, all other buttons are ignored. Default is [Buttons.LEFT]. Use -1 for any
 * button.
 */
abstract class MetaClickListener(val button: Int = Buttons.LEFT) : InputListener() {
	var tapSquareSize = 14f
	var touchDownX = -1f
		private set
	var touchDownY = -1f
		private set

	/** The pointer that initially pressed this button or -1 if the button is not pressed.  */
	var pressedPointer = -1
		private set

	/** The button that initially pressed this button or -1 if the button is not pressed.  */
	var pressedButton = -1
		private set

	/** Returns true if a touch is over the actor or within the tap square.  */
	var isPressed = false
		private set
	private var over = false
	private var cancelled = false
	private var visualPressedTime: Long = 0
	private var tapCountInterval = (0.4f * 1000000000L).toLong()

	/** Returns the number of taps within the tap count interval for the most recent click event.  */
	var tapCount = 0
		private set
	private var lastTapTime: Long = 0

	override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
		if (isPressed) return false
		isPressed = true
		pressedPointer = pointer
		pressedButton = button
		touchDownX = x
		touchDownY = y
		visualPressedTime = TimeUtils.millis() + (visualPressedDuration * 1000).toLong()
		return true
	}

	override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
		if (pointer != pressedPointer || cancelled) return
		isPressed = isOver(event.listenerActor, x, y)
		if (isPressed && pointer == 0 && button != -1 && !Gdx.input.isButtonPressed(button)) isPressed = false
		if (!isPressed) {
			// Once outside the tap square, don't use the tap square anymore.
			invalidateTapSquare()
		}
	}

	override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
		if (pointer == pressedPointer) {
			if (!cancelled) {
				val touchUpOver = isOver(event.listenerActor, x, y)
				// Ignore touch up if the wrong mouse button.
				if (touchUpOver) {
					val time = TimeUtils.nanoTime()
					if (time - lastTapTime > tapCountInterval) tapCount = 0
					tapCount++
					lastTapTime = time
					clicked(event, x, y)
				}
			}
			isPressed = false
			pressedPointer = -1
			pressedButton = -1
			cancelled = false
		}
	}

	override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
		if (pointer == -1 && !cancelled) over = true
	}

	override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
		if (pointer == -1 && !cancelled) over = false
	}

	/** If a touch down is being monitored, the drag and touch up events are ignored until the next touch up.  */
	fun cancel() {
		if (pressedPointer == -1) return
		cancelled = true
		isPressed = false
	}

	abstract fun clicked(event: InputEvent, x: Float, y: Float)

	/** Returns true if the specified position is over the specified actor or within the tap square.  */
	fun isOver(actor: Actor, x: Float, y: Float): Boolean {
		val hit = actor.hit(x, y, true)
		return if (hit == null || !hit.isDescendantOf(actor)) inTapSquare(x, y) else true
	}

	fun inTapSquare(x: Float, y: Float): Boolean {
		return if (touchDownX == -1f && touchDownY == -1f) false else abs(x - touchDownX) < tapSquareSize && abs(y - touchDownY) < tapSquareSize
	}

	/** Returns true if a touch is within the tap square.  */
	fun inTapSquare(): Boolean {
		return touchDownX != -1f
	}

	/** The tap square will no longer be used for the current touch.  */
	fun invalidateTapSquare() {
		touchDownX = -1f
		touchDownY = -1f
	}
	/** Returns true if a touch is over the actor or within the tap square or has been very recently. This allows the UI to show a
	 * press and release that was so fast it occurred within a single frame.  */
	val isVisualPressed: Boolean
		get() {
			if (isPressed) return true
			if (visualPressedTime <= 0) return false
			if (visualPressedTime > TimeUtils.millis()) return true
			visualPressedTime = 0
			return false
		}

	/** Returns true if the mouse or touch is over the actor or pressed and within the tap square.  */
	val isOver: Boolean get() = over || isPressed

	/** @param tapCountInterval time in seconds that must pass for two touch down/up sequences to be detected as consecutive taps.
	 */
	fun setTapCountInterval(tapCountInterval: Float) {
		this.tapCountInterval = (tapCountInterval * 1000000000L).toLong()
	}

	companion object {
		/** Time in seconds [isVisualPressed] reports true after a press resulting in a click is released.  */
		var visualPressedDuration = 0.1f
	}
}
