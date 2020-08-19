package de.fatox.meta.task

import com.badlogic.gdx.utils.Array

/**
 * A MetaTask is any action that should be reversible and actions that are not instant
 */
abstract class MetaTask(val name: String) {
	private val listeners = Array<TaskListener>()

	fun run() {
		for (listener in listeners) {
			listener.onStart()
		}
		execute()
		for (listener in listeners) {
			listener.onFinish()
		}
	}


	abstract fun execute()
	abstract fun undo()
}