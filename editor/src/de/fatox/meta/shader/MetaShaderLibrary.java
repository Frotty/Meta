package de.fatox.meta.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import de.fatox.meta.Meta;
import de.fatox.meta.api.dao.GLShaderData;
import de.fatox.meta.api.graphics.GLShaderHandle;
import de.fatox.meta.api.graphics.MetaGLShader;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.ide.ProjectManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Singleton;

@Singleton
public class MetaShaderLibrary {
    private static final String META_SHADER_SUFFIX = ".msh";
    private static final String INTERNAL_SHADER_PATH = "meta/shaders";
    @Inject
    private ProjectManager projectManager;
    @Inject
    private Json json;
    @Inject
    private UIManager uiManager;

    private ObjectMap<String, GLShaderHandle> loadedShaders = new ObjectMap<>();
    private Array<GLShaderHandle> metaShaders = new Array<>();

    public MetaShaderLibrary() {
        Meta.inject(this);
        projectManager.addOnLoadListener((evt) -> {
            loadProjectShaders();
            return false;
        });
        Gdx.app.postRunnable(this::loadProjectShaders);
    }

    public GLShaderHandle loadShader(FileHandle shaderHandle) {
        GLShaderData shaderData = json.fromJson(GLShaderData.class, shaderHandle.readString());
        FileHandle projRoot = projectManager.getCurrentProjectRoot();
        FileHandle vertHandle = projRoot.child(shaderData.getVertexFilePath());
        FileHandle fragHandle = projRoot.child(shaderData.getFragmentFilePath());
        if (vertHandle.exists() && !vertHandle.isDirectory() && fragHandle.exists() && !fragHandle.isDirectory()) {
            GLShaderHandle handle = new GLShaderHandle(shaderHandle, vertHandle, fragHandle, shaderData);
            metaShaders.add(handle);
            loadedShaders.put(projectManager.relativize(shaderHandle), handle);
            return handle;
        }
        return null;
    }

    public GLShaderHandle newShader(GLShaderData data) {
        FileHandle newShaderHandle = projectManager.save(INTERNAL_SHADER_PATH + data.getName() + META_SHADER_SUFFIX, data);
        return loadShader(newShaderHandle);
    }


    public Array<GLShaderHandle> getLoadedShaders() {
        return metaShaders;
    }

    public MetaGLShader getDefaultShaderPath(GLShaderHandle shaderHandle) {
        return null;
    }

    public GLShaderHandle getShaderHandle(String metaShaderPath) {
        if (loadedShaders.containsKey(metaShaderPath)) {
            return loadedShaders.get(metaShaderPath);
        }
        return null;
    }

    public void loadProjectShaders() {
        if (projectManager.getCurrentProject() != null) {
            FileHandle shaderFolder = projectManager.getCurrentProjectRoot().child(INTERNAL_SHADER_PATH);
            if (shaderFolder.exists()) {
                for (FileHandle metaShaderDef : shaderFolder.list(pathname -> pathname.getName().endsWith(META_SHADER_SUFFIX))) {
                    loadShader(metaShaderDef);
                }
            }
        }
    }

    public String getDefaultShaderPath() {
        return projectManager.relativize(loadedShaders.values().next().getShaderHandle());
    }
}
