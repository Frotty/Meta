package de.fatox.meta.ui.components

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.utils.TimeUtils
import kotlin.math.abs

class MetaListener(private val cb: () -> Unit) : InputListener() {
	var tapSquareSize: Float = 14f
	var touchDownX: Float = -1f
		private set
	var touchDownY: Float = -1f
		private set

	/** The pointer that initially pressed this button or -1 if the button is not pressed.  */
	var pressedPointer: Int = -1
		private set

	/** The button that initially pressed this button or -1 if the button is not pressed.  */
	var pressedButton: Int = -1
		private set
	/** @see .setButton
	 */
	/** Sets the button to listen for, all other buttons are ignored. Default is [Buttons.LEFT]. Use -1 for any button.  */
	var button: Int = 0

	/** Returns true if a touch is over the actor or within the tap square.  */
	var isPressed: Boolean = false
		private set
	private var over: Boolean = false
	private var cancelled: Boolean = false
	private var visualPressedTime: Long = 0
	private var tapCountInterval = (0.4f * 1000000000L).toLong()

	/** Returns the number of taps within the tap count interval for the most recent click event.  */
	var tapCount: Int = 0
		private set
	private var lastTapTime: Long = 0

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
	val isOver: Boolean
		get() = over || isPressed

	override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
		if (isPressed) return false
		isPressed = true
		pressedPointer = pointer
		pressedButton = button
		touchDownX = x
		touchDownY = y
		visualPressedTime = TimeUtils.millis() + (visualPressedDuration * 1000).toLong()
		return true
	}

	override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
		if (pointer != pressedPointer || cancelled) return
		isPressed = isOver(event!!.listenerActor, x, y)
		if (isPressed && pointer == 0 && button != -1 && !Gdx.input.isButtonPressed(button)) isPressed = false
		if (!isPressed) {
			// Once outside the tap square, don't use the tap square anymore.
			invalidateTapSquare()
		}
	}

	override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
		if (pointer == pressedPointer) {
			if (!cancelled) {
				val touchUpOver = isOver(event!!.listenerActor, x, y)
				// Ignore touch up if the wrong mouse button.
				if (touchUpOver) {
					val time = TimeUtils.nanoTime()
					if (time - lastTapTime > tapCountInterval) tapCount = 0
					tapCount++
					lastTapTime = time
					cb.invoke()
				}
			}
			isPressed = false
			pressedPointer = -1
			pressedButton = -1
			cancelled = false
		}
	}

	override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
		if (pointer == -1 && !cancelled) over = true
	}

	override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
		if (pointer == -1 && !cancelled) over = false
	}

	/** If a touch down is being monitored, the drag and touch up events are ignored until the next touch up.  */
	fun cancel() {
		if (pressedPointer == -1) return
		cancelled = true
		isPressed = false
	}

	/** Returns true if the specified position is over the specified actor or within the tap square.  */
	fun isOver(actor: Actor, x: Float, y: Float): Boolean {
		val hit = actor.hit(x, y, true)
		return if (hit == null || !hit.isDescendantOf(actor)) inTapSquare(x, y) else true
	}

	fun inTapSquare(x: Float, y: Float): Boolean {
		return if (touchDownX == -1f && touchDownY == -1f) false else abs(x - touchDownX) < tapSquareSize && abs(
			y - touchDownY
		) < tapSquareSize
	}

	/** Returns true if a touch is within the tap square.  */
	fun inTapSquare(): Boolean {
		return touchDownX != -1f
	}

	/** The tap square will not longer be used for the current touch.  */
	fun invalidateTapSquare() {
		touchDownX = -1f
		touchDownY = -1f
	}

	/** @param tapCountInterval time in seconds that must pass for two touch down/up sequences to be detected as consecutive taps.
	 */
	fun setTapCountInterval(tapCountInterval: Float) {
		this.tapCountInterval = (tapCountInterval * 1000000000L).toLong()
	}

	companion object {
		/** Time in seconds [.isVisualPressed] reports true after a press resulting in a click is released.  */
		var visualPressedDuration: Float = 0.1f
	}
}
