package de.fatox.meta.api.graphics;

import com.badlogic.gdx.utils.Array;

public interface ShaderLibrary {

    ShaderInfo compileAndAdd(ShaderSource shaderInfo);

    Iterable<ShaderInfo> getLoadedShaders();

    Array<ShaderInfo> getActiveShaders();

    ShaderInfo getOutputShader();

}
