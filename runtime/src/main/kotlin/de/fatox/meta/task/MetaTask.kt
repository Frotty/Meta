package de.fatox.meta.task

/**
 * A MetaTask is any action that should be reversible and actions that are not instant
 */
abstract class MetaTask(val name: String) {
	fun run() = execute()

	abstract fun execute()
	abstract fun undo()
}
