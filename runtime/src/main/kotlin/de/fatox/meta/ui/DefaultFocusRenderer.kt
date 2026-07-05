package de.fatox.meta.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import de.fatox.meta.api.ui.FocusRenderer

class DefaultFocusRenderer : FocusRenderer {
	private val shapeRenderer = ShapeRenderer()
	private val highlightColor = Color.valueOf("75BDF5FF")
	private val highlightPos = Vector2(0f, 0f)

	override fun draw(stage: Stage, focusedActor: Actor?, deltaTime: Float) {
		val actor = focusedActor ?: return
		if (!actor.isVisible || actor.stage == null) return
		if (MetaFocus.isHandledByActor(actor)) return

		shapeRenderer.projectionMatrix = stage.batch.projectionMatrix
		shapeRenderer.transformMatrix = stage.batch.transformMatrix
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
		highlightPos.set(0f, 0f)
		val coordinates = actor.localToStageCoordinates(highlightPos)
		shapeRenderer.color = highlightColor
		shapeRenderer.rect(coordinates.x, coordinates.y, actor.width, actor.height)
		shapeRenderer.end()
	}

	override fun dispose() {
		shapeRenderer.dispose()
	}
}


