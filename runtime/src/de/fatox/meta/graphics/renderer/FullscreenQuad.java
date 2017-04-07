package de.fatox.meta.graphics.renderer;

import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Encapsulates a fullscreen quad, geometry is aligned to the screen corners.
 *
 * @author bmanuel
 */
public class FullscreenQuad {
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
        float[] verts = new float[20];
        int i = 0;

        verts[i++] = -1;
        verts[i++] = -1;
        verts[i++] = 0;
        verts[i++] = 0f;
        verts[i++] = 0f;

        verts[i++] = 1f;
        verts[i++] = -1;
        verts[i++] = 0;
        verts[i++] = 1f;
        verts[i++] = 0f;

        verts[i++] = 1f;
        verts[i++] = 1f;
        verts[i++] = 0;
        verts[i++] = 1f;
        verts[i++] = 1f;

        verts[i++] = -1;
        verts[i++] = 1f;
        verts[i++] = 0;
        verts[i++] = 0f;
        verts[i++] = 1f;

        Mesh mesh = new Mesh(true, 4, 0,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

        mesh.setVertices(verts);
        return mesh;
    }

}
