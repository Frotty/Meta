package de.fatox.meta.api.graphics

interface Renderer {
	fun render(x: Float, y: Float)

	fun rebuild(width: Int, height: Int)

	fun rebuildCache()

}