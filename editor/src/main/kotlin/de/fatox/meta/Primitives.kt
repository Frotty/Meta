package de.fatox.meta

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

/**
 * Created by Frotty on 04.04.2017.
 */
object Primitives {
	private val assetProvider: AssetProvider by lazyInject()
	private val modelBuilder: ModelBuilder by lazyInject()

	private val defaultMaterial = Material().apply {
		set(TextureAttribute.createDiffuse(assetProvider.getResource("textures/defaultTex.png", Texture::class.java)))
	}
	private const val defaultAttr: Long = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal
		or VertexAttributes.Usage.ColorUnpacked or VertexAttributes.Usage.TextureCoordinates).toLong()

	val planeLines: Model by lazy(LazyThreadSafetyMode.NONE) {
		modelBuilder.createRect(0f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, GL20.GL_LINES, defaultMaterial, defaultAttr)
	}
	val planeFilled: Model by lazy(LazyThreadSafetyMode.NONE) {
		modelBuilder.createRect(0f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, defaultMaterial, defaultAttr)
	}
	val boxFilled: Model by lazy(LazyThreadSafetyMode.NONE) {
		modelBuilder.createBox(1f, 1f, 1f, defaultMaterial, defaultAttr)
	}
	val boxLines: Model by lazy(LazyThreadSafetyMode.NONE) {
		modelBuilder.createBox(1f, 1f, 1f, GL20.GL_LINES, defaultMaterial, defaultAttr)
	}
	val sphereLines: Model by lazy(LazyThreadSafetyMode.NONE) {
		modelBuilder.createSphere(1f, 1f, 1f, 32, 32, GL20.GL_LINES, defaultMaterial, defaultAttr)
	}
	val sphereFilled: Model by lazy(LazyThreadSafetyMode.NONE) {
		modelBuilder.createSphere(1f, 1f, 1f, 32, 32, defaultMaterial, defaultAttr)
	}
	val lineGrid: Model by lazy(LazyThreadSafetyMode.NONE) {
		modelBuilder.createLineGrid(16, 16, 2f, 2f, defaultMaterial, defaultAttr)
	}

	val terrainGrid: Model by lazy(LazyThreadSafetyMode.NONE) {
		modelBuilder.begin()
		val partBuilder = modelBuilder.part("quads", GL20.GL_TRIANGLES, defaultAttr, defaultMaterial)
		val xDivisions = 16
		val xSize = 2f
		val zDivisions = 16
		val zSize = 2f
		val xlength = xDivisions * xSize
		val zlength = zDivisions * zSize
		val hxlength = xlength / 2
		val hzlength = zlength / 2
		var x1 = -hxlength
		var z1: Float
		for (i in 0..xDivisions) {
			z1 = hzlength
			for (j in 0..zDivisions) {
				partBuilder.rect(x1, 0f, z1, x1, 0f, z1 + zSize, x1 + xSize, 0f, z1 + zSize, x1 + xSize, 0f, z1, 0f, 1f, 0f)
				z1 -= zSize
			}
			x1 += xSize
		}
		modelBuilder.end()
	}
}