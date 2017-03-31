package de.fatox.meta.entity;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import de.fatox.meta.Meta;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.entity.Entity;
import de.fatox.meta.injection.Inject;

public class Meta3DEntity implements Entity<Vector3> {
    public final Vector3 position = new Vector3();
    public final Vector3 center = new Vector3();
    public final Vector3 dimensions = new Vector3();
    public float radius;
    public float scale = 1;
    public ModelInstance actorModel;

    private final static BoundingBox bounds = new BoundingBox();
    private static Vector3 tempPos = new Vector3();

    @Inject
    private AssetProvider assetProvider;

    public Meta3DEntity(Vector3 pos, Model modelBase) {
        Meta.inject(this);
        this.actorModel = new ModelInstance(modelBase, pos);
        calculateBounds();
    }

    private void calculateBounds() {
        actorModel.transform.scale(scale, scale, scale);
        actorModel.calculateBoundingBox(bounds);
        bounds.getCenter(center);
        bounds.getDimensions(dimensions);
        radius = dimensions.len() / 2f;
    }

    public boolean isVisible(final Camera cam) {
        actorModel.transform.getTranslation(tempPos);
        tempPos.add(center);
        return cam.frustum.sphereInFrustum(tempPos, radius);
    }

    public RenderableProvider getActor() {
        return actorModel;
    }

    @Override
    public Vector3 getPosition() {
        return position;
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public void update() {
    }

    @Override
    public void draw() {

    }

    public void setPosition(Vector3 vec) {
        position.set(vec);
        actorModel.transform.setToTranslation(vec);
    }

}
