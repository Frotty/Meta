package de.fatox.meta.api.ui

import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage

interface FocusRenderer : Disposable {
	fun draw(stage: Stage, focusedActor: Actor?, deltaTime: Float)
	override fun dispose() = Unit
}
