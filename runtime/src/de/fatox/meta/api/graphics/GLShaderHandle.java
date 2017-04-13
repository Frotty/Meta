package de.fatox.meta.api.graphics;

import com.badlogic.gdx.files.FileHandle;
import de.fatox.meta.api.dao.GLShaderData;

/**
 * Created by Frotty on 02.07.2016.
 */
public class GLShaderHandle {
    public GLShaderData data;
    private FileHandle vertexHandle;
    private FileHandle fragmentHandle;

    public GLShaderHandle(String name, FileHandle vertexHandle, FileHandle fragmentHandle) {
        this.data = new GLShaderData(name, vertexHandle.path(), fragmentHandle.path());
        this.vertexHandle = vertexHandle;
        this.fragmentHandle = fragmentHandle;
    }

    public GLShaderHandle(GLShaderData data, FileHandle vertexHandle, FileHandle fragmentHandle) {
        this.data = data;
        this.vertexHandle = vertexHandle;
        this.fragmentHandle = fragmentHandle;
    }

    public FileHandle getVertexHandle() {
        return vertexHandle;
    }

    public void setVertexHandle(FileHandle vertexHandle) {
        this.vertexHandle = vertexHandle;
        data.vertexFilePath = vertexHandle.path();
    }

    public FileHandle getFragmentHandle() {
        return fragmentHandle;
    }

    public void setFragmentHandle(FileHandle fragmentHandle) {
        this.fragmentHandle = fragmentHandle;
        data.fragmentFilePath = fragmentHandle.path();
    }
}
