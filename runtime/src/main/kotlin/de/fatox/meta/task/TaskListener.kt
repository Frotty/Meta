package de.fatox.meta.task

abstract class TaskListener {
    abstract fun onFinish()
    abstract fun onStart()
}