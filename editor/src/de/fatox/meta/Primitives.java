package de.fatox.meta;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
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
    private final long defaultAttr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
            | VertexAttributes.Usage.ColorUnpacked | VertexAttributes.Usage.TextureCoordinates;

    private final Material defaultMaterial = new Material();
    @Inject
    private ModelBuilder modelBuilder;

    public Primitives() {
        Meta.inject(this);
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
}
