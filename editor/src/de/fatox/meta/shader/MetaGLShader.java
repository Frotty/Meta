package de.fatox.meta.shader;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.Meta;
import de.fatox.meta.api.graphics.GLShaderHandle;
import de.fatox.meta.error.MetaError;
import de.fatox.meta.error.MetaErrorHandler;
import de.fatox.meta.injection.Inject;

/**
 * Created by Frotty on 29.06.2016.
 */
public abstract class MetaGLShader implements Shader {
    @Inject
    private MetaErrorHandler metaErrorHandler;
    protected GLShaderHandle shaderHandle;
    protected ShaderProgram shaderProgram;
    private Array<UniformDef> uniformDefs = new Array<>();

    public MetaGLShader(GLShaderHandle shaderHandle) {
        Meta.inject(this);
        this.shaderHandle = shaderHandle;
    }

    public void addUniform(String name) {
        uniformDefs.add(new UniformDef(name, shaderProgram.getUniformLocation(name)));
    }

    @Override
    public void init() {
        shaderProgram = new ShaderProgram(shaderHandle.getVertexHandle(), shaderHandle.getFragmentHandle());
        if (!shaderProgram.isCompiled()) {
            metaErrorHandler.add(new MetaError("Shader compilation failed", "") {
                @Override
                public void gotoError() {
                    // TODO
                }
            });
        }
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
    public void end() {
        shaderProgram.end();
    }

    @Override
    public void dispose() {
        shaderProgram.dispose();
    }

    private static final Matrix3 tempM3 = new Matrix3();
    private static final Matrix4 tempM4 = new Matrix4();
    private static final Vector3 tempV3 = new Vector3();
}
