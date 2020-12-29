package de.fatox.meta.camera

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import de.fatox.meta.entity.Meta3DEntity

/**
 * Created by Frotty on 19.03.2017.
 */
class Gizmo {
    fun showFor(entity: Meta3DEntity?) {
        val modelBuilder = ModelBuilder()
        val material = Material(ColorAttribute.createDiffuse(Color.GREEN))
        val attributes =
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.ColorUnpacked).toLong()
        val xArrow = modelBuilder.createArrow(Vector3.Zero, Vector3.X, material, attributes)
        val yArrow = modelBuilder.createArrow(Vector3.Zero, Vector3.Y, material, attributes)
        val zArrow = modelBuilder.createArrow(Vector3.Zero, Vector3.Z, material, attributes)
    }
}