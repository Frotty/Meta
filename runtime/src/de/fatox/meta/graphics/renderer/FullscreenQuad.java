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

    public FullscreenQuad(float height) {
        quad = createFullscreenQuad(height);
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

    private Mesh createFullscreenQuad(float height) {
        float nheight = 1f -((1f -height) * 0.5f);
        float[] verts = new float[]{
            -1, -1, 0, 0, 0,
             1, -1, 0, 1, 0,
             1,  height, 0, 1, nheight,
            -1,  height, 0, 0, nheight
        };

        Mesh mesh = new Mesh(true, 4, 0,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

        mesh.setVertices(verts);
        return mesh;
    }

}
