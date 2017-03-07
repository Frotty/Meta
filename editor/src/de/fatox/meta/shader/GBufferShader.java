package de.fatox.meta.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class GBufferShader implements Shader {
    ShaderProgram program;
    Camera camera;
    RenderContext context;
    int u_projTrans;
    int u_worldTrans;
    private int u_normalTrans;
    private int u_mvpTrans;
    private int u_mat;
    private int u_mvTrans;
    private int s_diffuseTex;
    private int s_normalTex;
    private int u_diffuseColor;
    private int u_camPos;

    @Override
    public void init() {
        String vert = Gdx.files.internal("shaders/gbuffer.vert.glsl").readString();
        String frag = Gdx.files.internal("shaders/gbuffer.frag.glsl").readString();
        program = new ShaderProgram(vert, frag);
        if (!program.isCompiled())
            throw new GdxRuntimeException(program.getLog());
        u_projTrans = program.getUniformLocation("u_projViewTrans");
        u_worldTrans = program.getUniformLocation("u_worldTrans");
        u_normalTrans = program.getUniformLocation("u_normalTrans");
        u_mvpTrans = program.getUniformLocation("u_mvpTrans");
        u_mat = program.getUniformLocation("u_mat");
        u_mvTrans = program.getUniformLocation("u_mvTrans");
        u_diffuseColor = program.getUniformLocation("u_diffuseColor");
        s_diffuseTex = program.getUniformLocation("s_diffuseTex");
        s_normalTex = program.getUniformLocation("s_normalTex");
        u_camPos = program.getUniformLocation("u_camPos");
    }

    @Override
    public void dispose() {
        program.dispose();
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        this.camera = camera;
        this.context = context;
        program.begin();
        program.setUniformMatrix(u_projTrans, camera.combined);
        program.setUniformMatrix(u_mvTrans, camera.view);
        program.setUniformf(u_camPos, camera.position);
        context.setDepthTest(Gdx.gl.GL_LEQUAL);
        context.setCullFace(Gdx.gl.GL_BACK);
    }

    private final Matrix3 tmpM = new Matrix3();
    final Matrix4 temp = new Matrix4();
    Vector3 tempV = new Vector3();
    private final static Matrix4 idtMatrix = new Matrix4();

    @Override
    public void render(Renderable renderable) {
        program.setUniformMatrix(u_worldTrans, renderable.worldTransform);
        tmpM.set(renderable.worldTransform).inv().transpose();
        program.setUniformMatrix(u_normalTrans, tmpM);
        temp.set(camera.combined).mul(renderable.worldTransform);
        program.setUniformMatrix(u_mvpTrans, temp);
        tempV.set(.1f, 1f, 0);
        program.setUniformf(u_mat, tempV);

        // Bind Textures
        // Diffuse-
        TextureAttribute tex = (TextureAttribute) renderable.material.get(TextureAttribute.Diffuse);
        if (tex != null) {
            program.setUniformi(s_diffuseTex, context.textureBinder.bind((tex).textureDescription.texture));
        }
        // Normal Map (for different lighting on a plane)
        TextureAttribute texn = (TextureAttribute) renderable.material.get(TextureAttribute.Normal);
        if (texn != null) {
            program.setUniformi(s_normalTex, context.textureBinder.bind((texn).textureDescription.texture));
        }

        ColorAttribute col = (ColorAttribute) renderable.material.get(ColorAttribute.Diffuse);
        if (col != null) {
            program.setUniformf(u_diffuseColor, col.color.r, col.color.g, col.color.b);
        } else {
            tempV.set(1, 1, 1);
            program.setUniformf(u_diffuseColor, tempV);
        }

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
    public boolean canRender(Renderable instance) {
        return true;
    }
}