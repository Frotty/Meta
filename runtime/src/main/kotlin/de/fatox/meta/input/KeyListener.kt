package de.fatox.meta.input

import com.badlogic.gdx.utils.Timer
import com.badlogic.gdx.utils.Timer.Task

abstract class KeyListener {
	var requiredLengthMillis: Long = 0
	internal var task: Task? = null

	abstract fun onEvent()

	open fun onDown() {
		if (requiredLengthMillis > 0) {
			// A second onDown without an intervening onUp (e.g. key-repeat, or a missed keyUp while an exclusive
			// grab was active) must not orphan the previous self-rescheduling task - it would fire forever.
			task?.cancel()
			task = object : Task() {
				override fun run() {
					// Reschedule THIS task, not the field: the field may already point at a newer task.
					Timer.schedule(this, requiredLengthMillis / 1000f)
					onEvent()
				}
			}
			Timer.schedule(task, requiredLengthMillis / 1000f)
		}
	}

	open fun onUp() {
		task?.cancel()
		if (requiredLengthMillis <= 0) {
			onEvent()
		}
	}

	fun resetDelay() {
		task!!.cancel()
		Timer.schedule(task, requiredLengthMillis / 1000f)
	}
}