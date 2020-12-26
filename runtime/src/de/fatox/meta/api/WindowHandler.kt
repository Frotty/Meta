package de.fatox.meta.api

interface WindowHandler {
	/** Current X position of the current window. Defaults to zero if unknown. */
	val x: Int

	/** Current Y position of the current window. Defaults to zero if unknown. */
	val y: Int

	/** Tries to set the current window position to the given coordinates ([x], [y]). */
	fun modify(x: Int, y: Int)

	/** Tries to iconify the current window. */
	fun iconify()
}

object NoWindowHandler : WindowHandler {
	override val x: Int = 0
	override val y: Int = 0

	override fun modify(x: Int, y: Int): Unit = Unit

	override fun iconify(): Unit = Unit
}