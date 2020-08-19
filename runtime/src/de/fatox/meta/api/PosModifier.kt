package de.fatox.meta.api

interface PosModifier {
	val x: Int
	val y: Int

	fun modify(x: Int, y: Int)
}

object DummyPosModifier : PosModifier {
	override val x: Int = -1
	override val y: Int = -1

	override fun modify(x: Int, y: Int) = Unit
}