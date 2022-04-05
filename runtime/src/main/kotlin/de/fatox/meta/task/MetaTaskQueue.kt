package de.fatox.meta.task

import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.utils.Array

/**
 * A MetaTask is any action that should be reversible and actions that are not instant
 */
abstract class MetaTaskQueue(private val progressBar: ProgressBar, name: String, vararg tasks: MetaTask) :
	MetaTask(name) {
	private val tasks: Array<MetaTask> = Array(tasks)
	private lateinit var currentTask: MetaTask

	fun add(task: MetaTask, startIfEmpty: Boolean) {
		tasks.add(task)
		if (::currentTask.isInitialized && startIfEmpty && tasks.size == 1) {
			start()
		}
	}

	fun start() {
		currentTask = tasks.pop().also(MetaTask::execute)
	}

	fun onTaskFinished(task: MetaTask?) {}
}