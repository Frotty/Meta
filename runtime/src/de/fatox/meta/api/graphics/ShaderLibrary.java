package de.fatox.meta.api.graphics;

import com.badlogic.gdx.utils.Array;

public interface ShaderLibrary {

    Iterable<ShaderInfo> getLoadedShaders();

    Array<ShaderInfo> getActiveShaders();

    ShaderInfo getOutputShader();

    void addShader(GLShaderHandle glShaderHandle);
}
