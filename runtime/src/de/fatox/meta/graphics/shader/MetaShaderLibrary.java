package de.fatox.meta.graphics.shader;

import de.fatox.meta.api.graphics.ShaderInfo;
import de.fatox.meta.api.graphics.ShaderLibrary;
import de.fatox.meta.api.graphics.ShaderSource;

public class MetaShaderLibrary implements ShaderLibrary {

    @Override
    public ShaderInfo compileAndAdd(ShaderSource shaderInfo) {
        return null;
    }

    @Override
    public Iterable<ShaderInfo> getLoadedShaders() {
        return null;
    }

    @Override
    public Iterable<ShaderInfo> getActiveShaders() {
        return null;
    }

    @Override
    public ShaderInfo getOutputShader() {
        return null;
    }
}
