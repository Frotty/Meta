package de.fatox.meta.shader;

import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.api.graphics.ShaderInfo;
import de.fatox.meta.api.graphics.ShaderLibrary;
import de.fatox.meta.api.graphics.ShaderSource;

public class MetaShaderLibrary implements ShaderLibrary {

    private ShaderInfo shaderInfo;
    private Array<ShaderInfo> activeShaders = new Array<>();

    public MetaShaderLibrary() {
        Shader gbufSHader = new GBufferShader();
        gbufSHader.init();
        ShaderInfo testShader = new ShaderInfo() {
            RenderTarget[] tgts = new RenderTarget[]{new RenderTarget("albedo"), new RenderTarget("normalsDepth"), new RenderTarget("aux"), new RenderTarget("pos")};

            @Override
            public boolean isDepth() {
                return false;
            }

            @Override
            public RenderTarget[] getRenderTargets() {
                return tgts;
            }

            @Override
            public Shader getShader() {
                return gbufSHader;
            }

            @Override
            public String getName() {
                return "Test";
            }
        };
        activeShaders.add(testShader);
    }

    @Override
    public ShaderInfo compileAndAdd(ShaderSource shaderInfo) {
        return null;
    }

    @Override
    public Iterable<ShaderInfo> getLoadedShaders() {
        return null;
    }

    @Override
    public Array<ShaderInfo> getActiveShaders() {
        return activeShaders;
    }

    @Override
    public ShaderInfo getOutputShader() {
        if (shaderInfo == null) {
            Shader shader = new CompositeShader();
            shader.init();
            shaderInfo = new ShaderInfo() {

                @Override
                public boolean isDepth() {
                    return false;
                }

                @Override
                public RenderTarget[] getRenderTargets() {
                    return new RenderTarget[]{new RenderTarget("gl_FragColor")};
                }

                @Override
                public Shader getShader() {
                    return shader;
                }

                @Override
                public String getName() {
                    return "Test";
                }
            };
        }
        return shaderInfo;
    }
}