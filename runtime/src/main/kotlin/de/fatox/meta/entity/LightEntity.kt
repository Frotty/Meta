package de.fatox.meta.entity

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3

import de.fatox.meta.api.entity.Entity
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

/**
 * Created by Frotty on 20.03.2017.
 */
class LightEntity(override var position: Vector3, radius: Float, color: Vector3) : Entity<Vector3> {
	var color: Vector3
	var intensity = 0f
	var radius: Float
	var volumeSphere: ModelInstance

	private val modelBuilder: ModelBuilder by lazyInject()

	override val id: Int
		get() = 0

	override fun update() {}
	override fun draw() {}

	companion object {
		var model: Model? = null
		private val blendingAttribute = BlendingAttribute(GL20.GL_ONE, GL20.GL_ONE)
	}

	init {
		if (model == null) {
			model = modelBuilder.createSphere(
				2f,
				2f,
				2f,
				20,
				20,
				Material(),
				(VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.ColorUnpacked or VertexAttributes.Usage.TextureCoordinates.toLong()
					.toInt()).toLong()
			)
			model!!.materials[0].set(blendingAttribute)
			model!!.materials[0].set(IntAttribute.createCullFace(GL20.GL_FRONT))
		}
		this.position = position
		this.color = color
		this.radius = radius
		volumeSphere = ModelInstance(model, position)
		volumeSphere.transform.scl(radius * 2)
	}
}