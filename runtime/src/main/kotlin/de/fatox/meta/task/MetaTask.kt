package de.fatox.meta.task

import com.badlogic.gdx.utils.Array

/**
 * A MetaTask is any action that should be reversible and actions that are not instant
 */
abstract class MetaTask(val name: String) {
	private val listeners = Array<TaskListener>()

	fun run() {
		listeners.forEach(TaskListener::onStart)
		execute()
		listeners.forEach(TaskListener::onFinish)
	}

	abstract fun execute()
	abstract fun undo()
}