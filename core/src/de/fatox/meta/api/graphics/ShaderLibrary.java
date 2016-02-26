package de.fatox.meta.api.graphics;

public interface ShaderLibrary {

    ShaderInfo compileAndAdd(ShaderSource shaderInfo);

    Iterable<ShaderInfo> getLoadedShaders();

    Iterable<ShaderInfo> getActiveShaders();

    ShaderInfo getOutputShader();

}
