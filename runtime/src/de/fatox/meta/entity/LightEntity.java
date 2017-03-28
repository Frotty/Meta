package de.fatox.meta.entity;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import de.fatox.meta.Meta;
import de.fatox.meta.api.entity.Entity;
import de.fatox.meta.injection.Inject;

import static com.badlogic.gdx.graphics.VertexAttributes.Usage;

/**
 * Created by Frotty on 20.03.2017.
 */
public class LightEntity implements Entity<Vector3> {
    public static Model model;
    public Vector3 position;
    public Vector3 color;
    public float intensity;
    public float radius;

    public ModelInstance volumeSphere;
    private static BlendingAttribute blendingAttribute = new BlendingAttribute(GL20.GL_ONE, GL20.GL_ONE);

    @Inject
    private ModelBuilder modelBuilder;

    public LightEntity(Vector3 pos, float radius, Vector3 color) {
        if (model == null) {
            Meta.inject(this);
            model = modelBuilder.createSphere(1f, 1f, 1f, 20, 20, new Material(), Usage.Position | Usage.Normal | Usage.ColorUnpacked | Usage.TextureCoordinates);
            model.materials.get(0).set(blendingAttribute);
            model.materials.get(0).set(IntAttribute.createCullFace(GL20.GL_FRONT));
        }
        this.position = pos;
        this.color = color;
        this.radius = radius;
        volumeSphere = new ModelInstance(model, pos);
        volumeSphere.transform.scl(radius * 2);
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public Vector3 getPosition() {
        return null;
    }

    @Override
    public void update() {

    }

    @Override
    public void draw() {

    }
}
