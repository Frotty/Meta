package de.fatox.meta.api.graphics;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.api.dao.GLShaderData;
import de.fatox.meta.api.dao.RenderTargetData;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Frotty on 02.07.2016.
 */
public class GLShaderHandle {
    public GLShaderData data;
    public Array<RenderTargetData> targets = new Array<>();
    private FileHandle shaderHandle;
    private FileHandle vertexHandle;
    private FileHandle fragmentHandle;

    private static final Pattern outPattern = Pattern.compile("(.*)(out)(\\s)+(vec[2-4])(\\s)+(\\w+)(;)");

    public GLShaderHandle(FileHandle metaShaderHandle, FileHandle vertexHandle, FileHandle fragmentHandle, GLShaderData shaderData) {
        this.shaderHandle = metaShaderHandle;
        this.vertexHandle = vertexHandle;
        this.fragmentHandle = fragmentHandle;
        this.data = shaderData;
        fetchRendertargets();
    }

    public FileHandle getVertexHandle() {
        return vertexHandle;
    }

    public void setVertexHandle(FileHandle vertexHandle) {
        this.vertexHandle = vertexHandle;
        data.setVertexFilePath(vertexHandle.path());
    }

    public FileHandle getFragmentHandle() {
        return fragmentHandle;
    }

    public void setFragmentHandle(FileHandle fragmentHandle) {
        this.fragmentHandle = fragmentHandle;
        data.setFragmentFilePath(fragmentHandle.path());
    }

    private void fetchRendertargets() {
        targets.clear();
        try(BufferedReader br = new BufferedReader(fragmentHandle.reader())) {
            String line;
            while ((line = br.readLine()) != null) {
                if(line.startsWith("layout")) {
                    Matcher matcher = outPattern.matcher(line);
                    if(matcher.matches()) {
                        String type = matcher.group(4);
                        String name = matcher.group(6);
                        targets.add(new RenderTargetData(type, name));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(targets.size == 0) {
            // add default gl out
            targets.add(new RenderTargetData("vec4", "gl_FragColor"));
        }
    }

    @Override
    public String toString() {
        return data.getName();
    }

    public FileHandle getShaderHandle() {
        return shaderHandle;
    }
}
