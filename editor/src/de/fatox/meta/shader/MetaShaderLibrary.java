package de.fatox.meta.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.Shaders;
import de.fatox.meta.api.graphics.ShaderInfo;
import de.fatox.meta.api.graphics.ShaderLibrary;
import de.fatox.meta.api.graphics.ShaderSource;
import de.fatox.meta.graphics.renderer.FullscreenShader;

public class MetaShaderLibrary implements ShaderLibrary {

    private ShaderInfo shaderInfo;
    private Array<ShaderInfo> activeShaders = new Array<>();

    public MetaShaderLibrary() {
        Shader gbufSHader = new GBufferShader();
        gbufSHader.init();
//        Shader gbufSHader = new GBufferShader();
//        gbufSHader.init();
        ShaderInfo testShader = new ShaderInfo() {

            @Override
            public RenderTarget[] getRenderTargets() {
                return new RenderTarget[]{new RenderTarget("albedo"), new RenderTarget("normalsDepth"), new RenderTarget("aux"), new RenderTarget("pos")};
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
    public Iterable<ShaderInfo> getActiveShaders() {
        return activeShaders;
    }

    @Override
    public ShaderInfo getOutputShader() {
        if (shaderInfo == null) {
            shaderInfo = new ShaderInfo() {
                private Shader shader;

                @Override
                public RenderTarget[] getRenderTargets() {
                    return new RenderTarget[]{new RenderTarget("gl_FragColor")};
                }

                @Override
                public Shader getShader() {
                    if (shader == null) {
                        shader = new FullscreenShader() {

                            @Override
                            public ShaderProgram getProgram() {
                                return Shaders.getStartMenuBgShader();
                            }

                            @Override
                            public void init() {

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
                            public void begin(Camera camera, RenderContext context) {
                                Shaders.getStartMenuBgShader().begin();
                                Shaders.getStartMenuBgShader().setUniformf("time", 5.0f);
                                Shaders.getStartMenuBgShader().setUniformf("resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

                            }

                            @Override
                            public void render(Renderable renderable) {
                            }

                            @Override
                            public void end() {
                                Shaders.getStartMenuBgShader().end();
                            }

                            @Override
                            public void dispose() {

                            }
                        };
                    }
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
