package de.fatox.meta.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * Final composite Shader that turns the gbuffers into a composite texture,
 * which is then rendered to a fullscreen Quad.
 */
public class LightShader implements Shader {
    private ShaderProgram program;
    private int u_InverseScreenSize;
    private int u_WorldTrans;
    private int u_ProjTrans;
    private int u_ProjViewTrans;
    private int u_ViewTrans;
    private int u_MvpTrans;
    private int u_FarDistance;
    private int u_LightPosition;
    private int u_NormalTrans;
    private int u_CamPos;
    //Light
    private int u_LightRadius;
    private int u_LightColor;
    private int u_invProjTrans;
    private int u_MvTrans;

    @Override
    public void init() {
        String vert = Gdx.files.internal("shaders/lightpoint.vert.glsl").readString();
        String frag = Gdx.files.internal("shaders/lightpoint.frag.glsl").readString();
        program = new ShaderProgram(vert, frag);
        if (!program.isCompiled()) {
            throw new GdxRuntimeException(program.getLog());
        } else {
            System.out.println("Shader compiled correctly. Appending log:\n" + program.getLog());
        }
        u_InverseScreenSize = program.getUniformLocation("u_inverseScreenSize");
        u_WorldTrans = program.getUniformLocation("u_worldTrans");
        u_MvpTrans = program.getUniformLocation("u_mvpTrans");
        u_MvTrans = program.getUniformLocation("u_mvTrans");
        u_FarDistance = program.getUniformLocation("u_farDistance");
        u_LightPosition = program.getUniformLocation("u_lightPosition");
        u_ProjTrans = program.getUniformLocation("u_projTrans");
        u_ProjViewTrans = program.getUniformLocation("u_projViewTrans");
        u_invProjTrans = program.getUniformLocation("u_invProjTrans");
        u_ViewTrans = program.getUniformLocation("u_viewTrans");
        u_NormalTrans = program.getUniformLocation("u_normalTrans");
        u_LightRadius = program.getUniformLocation("u_lightRadius");
        u_LightColor = program.getUniformLocation("u_lightColor");
        u_CamPos = program.getUniformLocation("u_camPos");
    }

    public ShaderProgram getProgram() {
        return program;
    }

    @Override
    public void dispose() {
        program.dispose();
    }

    private final Matrix3 tmpM = new Matrix3();
    final Matrix4 temp = new Matrix4();
    Vector3 tempV = new Vector3();
    private Camera camera;
    private RenderContext context;

    @Override
    public void render(Renderable renderable) {
        context.setDepthMask(false);
        context.setDepthTest(GL20.GL_GEQUAL);
        context.setBlending(true, GL20.GL_ONE, GL20.GL_ONE);
        context.setCullFace(GL20.GL_FRONT);
        renderable.worldTransform.getTranslation(tempV);
        program.setUniformf(u_LightPosition, tempV);

        program.setUniformf(u_LightRadius, 50f);


        program.setUniformMatrix(u_WorldTrans, renderable.worldTransform);
        temp.set(camera.combined).mul(renderable.worldTransform);
        program.setUniformMatrix(u_MvpTrans, temp);
        temp.set(camera.view).mul(renderable.worldTransform);
        program.setUniformMatrix(u_MvTrans, temp);
        tmpM.set(renderable.worldTransform).inv().transpose();
        program.setUniformMatrix(u_NormalTrans, tmpM);
        renderable.meshPart.render(program);
    }

    @Override
    public void end() {
        program.end();
    }

    @Override
    public int compareTo(Shader other) {
        return 0;
    }

    @Override
    public boolean canRender(Renderable renderable) {
        return true;
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        this.camera = camera;
        this.context = context;
        program.begin();
        program.setUniformMatrix(u_ProjTrans, camera.projection);
        program.setUniformMatrix(u_invProjTrans, camera.invProjectionView);
        program.setUniformMatrix(u_ViewTrans, camera.view);
        program.setUniformMatrix(u_ProjViewTrans, camera.combined);
//        program.setUniformf("u_nearDistance", camera.near);
//        program.setUniformf("u_farDistance", camera.far);
        program.setUniformf(u_FarDistance, camera.far);
        program.setUniformf(u_CamPos, camera.position);
    }
}