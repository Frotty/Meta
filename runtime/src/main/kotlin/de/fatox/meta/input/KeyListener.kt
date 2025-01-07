package de.fatox.meta.input

import com.badlogic.gdx.utils.Timer
import com.badlogic.gdx.utils.Timer.Task

abstract class KeyListener {
	var requiredLengthMillis: Long = 0
	internal var task: Task? = null

	abstract fun onEvent()

	open fun onDown() {
		if (requiredLengthMillis > 0) {
			task = object : Task() {
				override fun run() {
					Timer.schedule(task, requiredLengthMillis / 1000f)
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