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
public class CompositeShader extends FullscreenShader {
    private ShaderProgram program;
    private int s_albedoTex;
    private int s_depthTex;
    private int u_nearDistance;
    private int u_farDistance;
    @Override
    public ShaderProgram getProgram() {
        return program;
    }

    private Matrix4 temp = new Matrix4();
    @Override
    public void init() {
        String vert = Gdx.files.internal("shaders/composite.vert").readString();
        String frag = Gdx.files.internal("shaders/composite.frag").readString();
        program = new ShaderProgram(vert, frag);
        if (!program.isCompiled())
            throw new GdxRuntimeException(program.getLog());

//        s_albedoTex = program.getUniformLocation("s_albedoTex");
        s_depthTex = program.getUniformLocation("s_depthTex");
        u_nearDistance = program.getUniformLocation("u_cameraNear");
        u_farDistance = program.getUniformLocation("u_cameraFar");

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
        program.setUniformf(u_nearDistance, camera.near);
        program.setUniformf(u_farDistance, camera.far);
        program.setUniformMatrix("u_invProjTrans", camera.invProjectionView);
        program.setUniformMatrix("u_projTrans", camera.projection);
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
