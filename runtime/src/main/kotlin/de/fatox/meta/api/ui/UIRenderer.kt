package de.fatox.meta.api.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.Disposable
import com.kotcrab.vis.ui.util.ToastManager

interface UIRenderer : Disposable {
	fun load()
	fun addActor(actor: Actor)
	fun update()
	fun draw()
	fun resize(width: Int, height: Int)
	fun getCamera(): Camera
	fun getToastManager(): ToastManager
	fun setFocusedActor(actor: Actor?)
	override fun dispose() = Unit
}
