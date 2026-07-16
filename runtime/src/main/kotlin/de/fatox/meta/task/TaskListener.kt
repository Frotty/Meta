package de.fatox.meta.task

@Deprecated("MetaTask has no listener lifecycle; expose task state through de.fatox.meta.reactive signals")
abstract class TaskListener {
	abstract fun onFinish()
	abstract fun onStart()
}
