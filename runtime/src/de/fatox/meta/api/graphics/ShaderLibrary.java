package de.fatox.meta.api.graphics;

import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.utils.Array;

public interface ShaderLibrary {

    Iterable<ShaderInfo> getLoadedShaders();

    Array<Shader> getActiveShaders();

    ShaderInfo getOutputShader();

    void addShader(GLShaderHandle glShaderHandle);
}
