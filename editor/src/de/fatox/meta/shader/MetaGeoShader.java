package de.fatox.meta.shader;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import de.fatox.meta.api.graphics.*;

/**
 * Created by Frotty on 29.06.2016.
 */
public class MetaGeoShader extends de.fatox.meta.api.graphics.MetaGLShader {
    private Camera camera;
    private RenderContext context;


    public MetaGeoShader(GLShaderHandle shaderHandle) {
        super(shaderHandle);
    }

    @Override
    public void init() {
        super.init();
        if(shaderProgram.isCompiled()) {
            String[] uniforms = shaderProgram.getUniforms();
        }
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        this.camera = camera;
        this.context = context;
        shaderProgram.begin();
        setCameraUniforms();
    }

    private void setCameraUniforms() {
        // TODO
    }

    @Override
    public void render(Renderable renderable) {
        setRenderableUniforms();
    }

    private void setRenderableUniforms() {

    }
}
