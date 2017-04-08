package de.fatox.meta.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.GdxRuntimeException;
import de.fatox.meta.graphics.renderer.FullscreenShader;

/**
 * Created by Frotty on 20.05.2016.
 */
public class BlurShader extends FullscreenShader {
    private ShaderProgram program;
    private int s_inputTex;


    @Override
    public ShaderProgram getProgram() {
        return program;
    }

    private Matrix4 temp = new Matrix4();
    @Override
    public void init() {
        String vert = Gdx.files.internal("shaders/ssaoblur.vert").readString();
        String frag = Gdx.files.internal("shaders/ssaoblur.frag").readString();
        program = new ShaderProgram(vert, frag);
        if (!program.isCompiled())
            throw new GdxRuntimeException(program.getLog());

    }

    @Override
    public int compareTo(Shader other) {
        return 0;
    }

    @Override
    public boolean canRender(Renderable instance) {
        return true;
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        program.begin();
    }


    @Override
    public void end() {
        program.end();
    }

    @Override
    public void dispose() {
        program.dispose();
    }
}
