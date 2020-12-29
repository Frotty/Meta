package de.fatox.meta.task

import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.utils.Array

/**
 * A MetaTask is any action that should be reversible and actions that are not instant
 */
abstract class MetaTaskQueue(private val progressBar: ProgressBar, vararg tasks: MetaTask?) : MetaTask() {
    private var currentTask: MetaTask? = null
    private val tasks = Array<MetaTask>()
    fun add(task: MetaTask, startIfEmpty: Boolean) {
        tasks.add(task)
        if (currentTask == null && startIfEmpty && tasks.size == 1) {
            start()
        }
    }

    fun start() {
        currentTask = tasks.pop()
        currentTask.execute()
    }

    fun onTaskFinished(task: MetaTask?) {}

    init {
        this.tasks.addAll(*tasks)
    }
}