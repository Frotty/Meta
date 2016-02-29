package de.fatox.meta.api.graphics;

import com.badlogic.gdx.graphics.g3d.Shader;

public interface ShaderInfo {
    int getRenderTargets();

    Shader getShader();

    String getName();
}
