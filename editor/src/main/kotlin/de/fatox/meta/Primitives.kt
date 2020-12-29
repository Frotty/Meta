package de.fatox.meta

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import de.fatox.meta.Meta.Companion.inject
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.injection.Inject

/**
 * Created by Frotty on 04.04.2017.
 */
class Primitives {
    var planeLines: Model? = null
        get() {
            if (field == null) {
                field = modelBuilder!!.createRect(
                    0f,
                    0f,
                    0f,
                    1f,
                    0f,
                    0f,
                    1f,
                    1f,
                    0f,
                    0f,
                    1f,
                    0f,
                    0f,
                    0f,
                    1f,
                    GL20.GL_LINES,
                    defaultMaterial,
                    defaultAttr
                )
            }
            return field
        }
        private set
    var planeFilled: Model? = null
        get() {
            if (field == null) {
                field = modelBuilder!!.createRect(
                    0f,
                    0f,
                    0f,
                    1f,
                    0f,
                    0f,
                    1f,
                    1f,
                    0f,
                    0f,
                    1f,
                    0f,
                    0f,
                    0f,
                    1f,
                    defaultMaterial,
                    defaultAttr
                )
            }
            return field
        }
        private set
    var boxFilled: Model? = null
        get() {
            if (field == null) {
                field = modelBuilder!!.createBox(1f, 1f, 1f, defaultMaterial, defaultAttr)
            }
            return field
        }
        private set
    var boxLines: Model? = null
        get() {
            if (field == null) {
                field = modelBuilder!!.createBox(1f, 1f, 1f, GL20.GL_LINES, defaultMaterial, defaultAttr)
            }
            return field
        }
        private set
    var sphereLines: Model? = null
        get() {
            if (field == null) {
                field = modelBuilder!!.createSphere(1f, 1f, 1f, 32, 32, GL20.GL_LINES, defaultMaterial, defaultAttr)
            }
            return field
        }
        private set
    var sphereFilled: Model? = null
        get() {
            if (field == null) {
                field = modelBuilder!!.createSphere(1f, 1f, 1f, 32, 32, defaultMaterial, defaultAttr)
            }
            return field
        }
        private set
    private var lineGrid: Model? = null
    private var terrainGrid: Model? = null
    private val defaultMaterial = Material()

    @Inject
    private val assetProvider: AssetProvider? = null

    @Inject
    private val modelBuilder: ModelBuilder? = null
    val linegrid: Model?
        get() {
            if (lineGrid == null) {
                lineGrid = modelBuilder!!.createLineGrid(16, 16, 2f, 2f, defaultMaterial, defaultAttr)
            }
            return lineGrid
        }
    val terraingrid: Model?
        get() {
            if (terrainGrid == null) {
                modelBuilder!!.begin()
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
                        partBuilder.rect(
                            x1,
                            0f,
                            z1,
                            x1,
                            0f,
                            z1 + zSize,
                            x1 + xSize,
                            0f,
                            z1 + zSize,
                            x1 + xSize,
                            0f,
                            z1,
                            0f,
                            1f,
                            0f
                        )
                        z1 -= zSize
                    }
                    x1 += xSize
                }
                terrainGrid = modelBuilder.end()
            }
            return terrainGrid
        }

    companion object {
        const val defaultAttr = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal
                or VertexAttributes.Usage.ColorUnpacked or VertexAttributes.Usage.TextureCoordinates).toLong()
    }

    init {
        inject(this)
        defaultMaterial.set(
            TextureAttribute.createDiffuse(
                assetProvider!!.get(
                    "textures/defaultTex.png",
                    Texture::class.java
                )
            )
        )
    }
}