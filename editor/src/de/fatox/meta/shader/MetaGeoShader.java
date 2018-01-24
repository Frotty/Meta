package de.fatox.meta.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.graphics.GLShaderHandle;
import de.fatox.meta.injection.Inject;

/**
 * Created by Frotty on 29.06.2016.
 */
public class MetaGeoShader extends de.fatox.meta.api.graphics.MetaGLShader {
    private Camera camera;
    private RenderContext context;
    private int u_projTrans;
    private int u_worldTrans;
    private int u_normalTrans;
    private int u_mvpTrans;
    private int u_diffuseColor;
    private int s_diffuseTex;
    private int s_normalTex;
    private int u_camPos;
    private static Matrix3 tmpM3 = new Matrix3();
    private static Matrix4 tempM4 = new Matrix4();
    private static Vector3 tempV = new Vector3();
    private static Texture whiteTex;
    private static Texture emptyNormals;

    @Inject
    private AssetProvider assetProvider;

    public MetaGeoShader(GLShaderHandle shaderHandle) {
        super(shaderHandle);
    }

    @Override
    public void init() {
        super.init();
        u_projTrans = shaderProgram.getUniformLocation("u_projViewTrans");
        u_worldTrans = shaderProgram.getUniformLocation("u_worldTrans");
        u_normalTrans = shaderProgram.getUniformLocation("u_normalTrans");
        u_mvpTrans = shaderProgram.getUniformLocation("u_mvpTrans");
        u_diffuseColor = shaderProgram.getUniformLocation("u_diffuseColor");
        s_diffuseTex = shaderProgram.getUniformLocation("s_diffuseTex");
        s_normalTex = shaderProgram.getUniformLocation("s_normalTex");
        u_camPos = shaderProgram.getUniformLocation("u_camPos");

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGB888);
        pixmap.drawPixel(0, 0, Color.WHITE.toIntBits());
        whiteTex = new Texture(pixmap);
        emptyNormals = assetProvider.get("models/empty_n.png", Texture.class);
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        this.camera = camera;
        this.context = context;
        shaderProgram.begin();
        shaderProgram.setUniformMatrix(u_projTrans, camera.combined);
        shaderProgram.setUniformf(u_camPos, camera.position);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        setCameraUniforms();
    }

    private void setCameraUniforms() {
        // TODO
    }

    @Override
    public void render(Renderable renderable) {
        setRenderableUniforms(renderable);

        // Bind Textures
        // Diffuse-
        TextureAttribute diffuseTex = (TextureAttribute) renderable.material.get(TextureAttribute.Diffuse);
        if (diffuseTex != null) {
            shaderProgram.setUniformi(s_diffuseTex, context.textureBinder.bind((diffuseTex).textureDescription.texture));
        } else {
            shaderProgram.setUniformi(s_diffuseTex, context.textureBinder.bind(whiteTex));
        }
        // Normal Map (for different lighting on a plane)
        TextureAttribute normalTex = (TextureAttribute) renderable.material.get(TextureAttribute.Normal);
        if (normalTex != null) {
            shaderProgram.setUniformi(s_normalTex, context.textureBinder.bind((normalTex).textureDescription.texture));
        } else {
            shaderProgram.setUniformi(s_normalTex, context.textureBinder.bind(emptyNormals));
        }

        ColorAttribute col = (ColorAttribute) renderable.material.get(ColorAttribute.Diffuse);
        if (col != null) {
            shaderProgram.setUniformf(u_diffuseColor, col.color.r, col.color.g, col.color.b);
        } else {
            tempV.set(1, 1, 1);
            shaderProgram.setUniformf(u_diffuseColor, tempV);
        }

        renderable.meshPart.render(shaderProgram);
    }

    private void setRenderableUniforms(Renderable renderable) {
        shaderProgram.setUniformMatrix(u_worldTrans, renderable.worldTransform);
        tmpM3.set(renderable.worldTransform).inv().transpose();
        shaderProgram.setUniformMatrix(u_normalTrans, tmpM3);
        tempM4.set(camera.combined).mul(renderable.worldTransform);
        shaderProgram.setUniformMatrix(u_mvpTrans, tempM4);
    }
}
