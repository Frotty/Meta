package de.fatox.meta.camera;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import de.fatox.meta.entity.Meta3DEntity;

/**
 * Created by Frotty on 19.03.2017.
 */
public class Gizmo {
    public void showFor(Meta3DEntity entity) {
        ModelBuilder modelBuilder = new ModelBuilder();
        final Material material = new Material(ColorAttribute.createDiffuse(Color.GREEN));
        final long attributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.ColorUnpacked;
        Model xArrow = modelBuilder.createArrow(Vector3.Zero, Vector3.X, material, attributes);
        Model yArrow = modelBuilder.createArrow(Vector3.Zero, Vector3.Y, material, attributes);
        Model zArrow = modelBuilder.createArrow(Vector3.Zero, Vector3.Z, material, attributes);
    }
}