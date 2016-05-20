package de.fatox.meta.api.graphics;

import com.badlogic.gdx.graphics.g3d.Shader;

public interface ShaderInfo {
    class RenderTarget {
        public final String name;

        public RenderTarget(String name) {
            this.name = name;
        }
    }

    RenderTarget[] getRenderTargets();

    Shader getShader();

    String getName();
}
