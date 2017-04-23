package de.fatox.meta.shader;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import de.fatox.meta.Meta;
import de.fatox.meta.api.dao.GLShaderData;
import de.fatox.meta.api.graphics.*;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.ide.ProjectManager;
import de.fatox.meta.injection.Inject;

public class MetaShaderLibrary {
    public static final String META_SHADER_SUFFIX = ".msh";
    public static final String INTERNAL_SHADER_PATH = "meta/shaders";
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
        loadProjectShaders();
    }

    public GLShaderHandle loadShader(FileHandle shaderHandle) {
        GLShaderData shaderData = json.fromJson(GLShaderData.class, shaderHandle.readString());
        FileHandle projRoot = projectManager.getCurrentProjectRoot();
        FileHandle vertHandle = projRoot.child(shaderData.vertexFilePath);
        FileHandle fragHandle = projRoot.child(shaderData.fragmentFilePath);
        if (vertHandle.exists() && !vertHandle.isDirectory() && fragHandle.exists() && !fragHandle.isDirectory()) {
            GLShaderHandle handle = new GLShaderHandle(shaderHandle, vertHandle, fragHandle, shaderData);
            metaShaders.add(handle);
            loadedShaders.put(projectManager.relativize(shaderHandle), handle);
            return handle;
        }
        return null;
    }

    public GLShaderHandle newShader(GLShaderData data) {
        FileHandle newShaderHandle = projectManager.save(INTERNAL_SHADER_PATH + data.name + META_SHADER_SUFFIX, data);
        return loadShader(newShaderHandle);
    }


    private void loadProjectShaders() {
        if (projectManager.getCurrentProject() != null) {
            FileHandle shaderFolder = projectManager.getCurrentProjectRoot().child(INTERNAL_SHADER_PATH);
            if (shaderFolder.exists()) {
                for (FileHandle metaShaderDef : shaderFolder.list(pathname -> pathname.getName().endsWith(META_SHADER_SUFFIX))) {
                    loadShader(metaShaderDef);
                }
            }
        }
    }

    public Array<GLShaderHandle> getLoadedShaders() {
        return metaShaders;
    }

    public GLShaderHandle getShader(String metaShaderPath) {
        if(loadedShaders.containsKey(metaShaderPath)) {
            return loadedShaders.get(metaShaderPath);
        }
        return null;
    }


}
