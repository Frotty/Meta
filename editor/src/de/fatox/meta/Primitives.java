package de.fatox.meta;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.injection.Inject;

/**
 * Created by Frotty on 04.04.2017.
 */
public class Primitives {
    private Model planeLines;
    private Model planeFilled;
    private Model boxFilled;
    private Model boxLines;
    private Model sphereLines;
    private Model sphereFilled;
    private Model lineGrid;
    private Model terrainGrid;
    public static final long defaultAttr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
            | VertexAttributes.Usage.ColorUnpacked | VertexAttributes.Usage.TextureCoordinates;

    private final Material defaultMaterial = new Material();
    @Inject
    private AssetProvider assetProvider;
    @Inject
    private ModelBuilder modelBuilder;

    public Primitives() {
        Meta.inject(this);
        defaultMaterial.set(TextureAttribute.createDiffuse(assetProvider.get("textures/defaultTex.png", Texture.class)));
    }

    public Model getPlaneLines() {
        if (planeLines == null) {
            planeLines = modelBuilder.createRect(0, 0, 0,
                    1, 0, 0,
                    1, 1, 0,
                    0, 1, 0,
                    0, 0, 1, GL20.GL_LINES, defaultMaterial, defaultAttr);
        }
        return planeLines;
    }

    public Model getPlaneFilled() {
        if (planeFilled == null) {
            planeFilled = modelBuilder.createRect(0, 0, 0,
                    1, 0, 0,
                    1, 1, 0,
                    0, 1, 0,
                    0, 0, 1, defaultMaterial, defaultAttr);
        }
        return planeFilled;
    }

    public Model getBoxFilled() {
        if (boxFilled == null) {
            boxFilled = modelBuilder.createBox(1, 1, 1, defaultMaterial, defaultAttr);
        }
        return boxFilled;
    }

    public Model getBoxLines() {
        if (boxLines == null) {
            boxLines = modelBuilder.createBox(1, 1, 1, GL20.GL_LINES, defaultMaterial, defaultAttr);
        }
        return boxLines;
    }

    public Model getSphereLines() {
        if (sphereLines == null) {
            sphereLines = modelBuilder.createSphere(1, 1, 1, 32, 32, GL20.GL_LINES, defaultMaterial, defaultAttr);
        }
        return sphereLines;
    }


    public Model getSphereFilled() {
        if (sphereFilled == null) {
            sphereFilled = modelBuilder.createSphere(1, 1, 1, 32, 32, defaultMaterial, defaultAttr);
        }
        return sphereFilled;
    }

    public Model getLinegrid() {
        if (lineGrid == null) {
            lineGrid = modelBuilder.createLineGrid(16, 16, 2, 2, defaultMaterial, defaultAttr);
        }
        return lineGrid;
    }

    public Model getTerraingrid() {
        if (terrainGrid == null) {
            modelBuilder.begin();
            MeshPartBuilder partBuilder = modelBuilder.part("quads", GL20.GL_TRIANGLES, defaultAttr, defaultMaterial);
            int xDivisions = 16;
            float xSize = 2;
            int zDivisions = 16;
            float zSize = 2;
            float xlength = xDivisions * xSize, zlength = zDivisions * zSize, hxlength = xlength / 2, hzlength = zlength / 2;
            float x1 = -hxlength, z1;
            for (int i = 0; i <= xDivisions; ++i) {
                z1 = hzlength;
                for (int j = 0; j <= zDivisions; ++j) {
                    partBuilder.rect(x1, 0f, z1, x1, 0f, z1 + zSize, x1 + xSize, 0f, z1 + zSize, x1 + xSize, 0f, z1, 0f, 1f, 0f);
                    z1 -= zSize;
                }
                x1 += xSize;
            }

            terrainGrid = modelBuilder.end();
        }
        return terrainGrid;
    }
}
