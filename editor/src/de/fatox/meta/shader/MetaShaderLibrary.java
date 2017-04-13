package de.fatox.meta.shader;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import de.fatox.meta.Meta;
import de.fatox.meta.api.dao.GLShaderData;
import de.fatox.meta.api.graphics.GLShaderHandle;
import de.fatox.meta.api.graphics.ShaderInfo;
import de.fatox.meta.api.graphics.ShaderLibrary;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.ide.ProjectManager;
import de.fatox.meta.injection.Inject;

public class MetaShaderLibrary implements ShaderLibrary {
    public static final String META_SHADER_SUFFIX = ".msh";
    @Inject
    private ProjectManager projectManager;
    @Inject
    private Json json;
    @Inject
    private UIManager uiManager;
    private ShaderInfo shaderInfo;
    private Array<ShaderInfo> activeShaders = new Array<>();
    private Array<Shader> metaShaders = new Array<>();

    public MetaShaderLibrary() {
        Meta.inject(this);
        loadProjectShaders();
        Shader gbufSHader = new GBufferShader();
        gbufSHader.init();
        ShaderInfo testShader = new ShaderInfo() {
            RenderTarget[] tgts = new RenderTarget[]{new RenderTarget("albedo"), new RenderTarget("normalsDepth"), new RenderTarget("aux"), new RenderTarget
                    ("pos")};

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

    private void loadProjectShaders() {
        if (projectManager.getCurrentProject() != null) {
            FileHandle shaderFolder = projectManager.getCurrentProjectRoot().child("meta/shaders");
            if (shaderFolder.exists()) {
                for (FileHandle metaShaderDef : shaderFolder.list(pathname -> pathname.getName().endsWith(META_SHADER_SUFFIX))) {
                    FileHandle projRoot = projectManager.getCurrentProjectRoot();
                    GLShaderData shaderData = json.fromJson(GLShaderData.class, metaShaderDef.readString());
                    FileHandle vertHandle = projRoot.child(shaderData.vertexFilePath);
                    FileHandle fragHandle = projRoot.child(shaderData.fragmentFilePath);
                    if (vertHandle.exists() && !vertHandle.isDirectory() && fragHandle.exists() && !fragHandle.isDirectory()) {
                        GLShaderHandle handle = new GLShaderHandle(shaderData, vertHandle, fragHandle);
                        addShader(handle);
                    }
                }
            }
        }
    }

    @Override
    public Iterable<ShaderInfo> getLoadedShaders() {
        return null;
    }

    @Override
    public Array<Shader> getActiveShaders() {
        return metaShaders;
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
