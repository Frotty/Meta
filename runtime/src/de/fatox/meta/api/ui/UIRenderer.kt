package de.fatox.meta.api.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.scenes.scene2d.Actor

interface UIRenderer {
	fun load()
	fun addActor(actor: Actor?)
	fun update()
	fun draw()
	fun resize(width: Int, height: Int)
	val camera: Camera?
}