package de.fatox.meta.graphics.renderer;

import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Encapsulates a fullscreen quad, geometry is aligned to the screen corners.
 *
 * @credits bmanuel
 */
public class FullscreenQuad {
    private static final int VERT_SIZE = 16;
    private static final int X1 = 0;
    private static final int Y1 = 1;
    private static final int U1 = 2;
    private static final int V1 = 3;
    private static final int X2 = 4;
    private static final int Y2 = 5;
    private static final int U2 = 6;
    private static final int V2 = 7;
    private static final int X3 = 8;
    private static final int Y3 = 9;
    private static final int U3 = 10;
    private static final int V3 = 11;
    private static final int X4 = 12;
    private static final int Y4 = 13;
    private static final int U4 = 14;
    private static final int V4 = 15;
    private static float[] vertices = new float[VERT_SIZE];
    private Mesh quad;

    public FullscreenQuad() {
        quad = createFullscreenQuad();
    }

    public void dispose() {
        quad.dispose();
    }

    /**
     * Renders the quad with the specified shader program.
     */
    public void render(ShaderProgram program) {
        quad.render(program, GL30.GL_TRIANGLE_FAN, 0, 4);
    }

    private Mesh createFullscreenQuad() {
        // vertex coord
        vertices[X1] = -1;
        vertices[Y1] = -1;

        vertices[X2] = 1;
        vertices[Y2] = -1;

        vertices[X3] = 1;
        vertices[Y3] = 1;

        vertices[X4] = -1;
        vertices[Y4] = 1;

        // tex coords
        vertices[U1] = 0f;
        vertices[V1] = 0f;

        vertices[U2] = 1f;
        vertices[V2] = 0f;

        vertices[U3] = 1f;
        vertices[V3] = 1f;

        vertices[U4] = 0f;
        vertices[V4] = 1f;

        Mesh tmpMesh = new Mesh(true, 4, 0, new VertexAttribute(Usage.Position, 2, "a_position"),
                new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0"));

        tmpMesh.setVertices(vertices);
        return tmpMesh;
    }
}
