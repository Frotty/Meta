package de.fatox.meta.shader;

import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.Meta;
import de.fatox.meta.api.graphics.GLShaderHandle;
import de.fatox.meta.api.graphics.ShaderInfo;
import de.fatox.meta.api.graphics.ShaderLibrary;

public class MetaShaderLibrary implements ShaderLibrary {
    private ShaderInfo shaderInfo;
    private Array<ShaderInfo> activeShaders = new Array<>();
    private Array<MetaGLShader> metaShaders = new Array<>();

    public MetaShaderLibrary() {
        Meta.inject(this);
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

        Shader lightShader = new de.fatox.meta.entity.LightShader();
        lightShader.init();
        ShaderInfo testShader2 = new ShaderInfo() {
            RenderTarget[] tgts = new RenderTarget[]{new RenderTarget("outColor")};

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
                return lightShader;
            }

            @Override
            public String getName() {
                return "Test2";
            }
        };
        activeShaders.add(testShader2);

        Shader blurSahder = new BlurShader();
        blurSahder.init();
        ShaderInfo testShader3 = new ShaderInfo() {
            RenderTarget[] tgts = new RenderTarget[]{new RenderTarget("outColor")};

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
                return blurSahder;
            }

            @Override
            public String getName() {
                return "Test3";
            }
        };
        activeShaders.add(testShader3);
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

    @Override
    public void addShader(GLShaderHandle glShaderHandle) {
        MetaGLShader metaShader = new MetaGeoShader(glShaderHandle);
        metaShader.init();
        metaShaders.add(metaShader);
    }
}
