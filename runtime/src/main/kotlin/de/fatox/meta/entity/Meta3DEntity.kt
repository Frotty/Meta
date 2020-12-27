package de.fatox.meta.entity

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.RenderableProvider
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import de.fatox.meta.api.entity.Entity

class Meta3DEntity(override val position: Vector3 = Vector3(), modelBase: Model?, scale: Float) : Entity<Vector3> {
	val center: Vector3 = Vector3()
	val dimensions: Vector3 = Vector3()
	var radius: Float = 0f
	var scale: Float = 1f
	var actorModel: ModelInstance
	private fun calculateBounds() {
		actorModel.transform.scale(scale, scale, scale)
		actorModel.calculateBoundingBox(bounds)
		bounds.getCenter(center)
		bounds.getDimensions(dimensions)
		radius = dimensions.len() / 2f
	}

	fun isVisible(cam: Camera): Boolean {
		actorModel.transform.getTranslation(tempPos)
		tempPos.add(center)
		return cam.frustum.sphereInFrustum(tempPos, radius)
	}

	val actor: RenderableProvider
		get() = actorModel

	override val id: Int
		get() = 0

	override fun update() {}
	override fun draw() {}
	fun setPosition(vec: Vector3?) {
		position.set(vec)
		actorModel.transform.setToTranslation(vec)
	}

	companion object {
		private val bounds = BoundingBox()
		private val tempPos = Vector3()
	}

	init {
		actorModel = ModelInstance(modelBase, position)
		this.scale = scale
		calculateBounds()
	}
}