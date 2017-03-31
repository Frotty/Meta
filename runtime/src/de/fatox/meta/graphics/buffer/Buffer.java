package de.fatox.meta.graphics.buffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import de.fatox.meta.Meta;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.graphics.ShaderInfo;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;

import java.nio.IntBuffer;

public abstract class Buffer {
    private static final String TAG = "Buffer";
    @Inject
    @Log
    private Logger log;
    public final ShaderInfo shaderInfo;
    public IntBuffer ibuffer;
    private Array<BufferTexture> texs;
    private boolean first = true;

    @Inject
    private DefaultTextureBinder textureBinder;

    public Buffer(int offset, ShaderInfo shaderInfo) {
        Meta.inject(this);
        this.shaderInfo = shaderInfo;
        ibuffer = BufferUtils.newIntBuffer(shaderInfo.getRenderTargets().length);
        for (int i = 0; i < shaderInfo.getRenderTargets().length; i++) {
            ibuffer.put(GL30.GL_COLOR_ATTACHMENT0 + offset + i);
            log.debug(TAG, shaderInfo.getName() + " Put into Buffer" + (GL30.GL_COLOR_ATTACHMENT0 + offset + i));
        }
        ibuffer.position(0);
    }

    public int getSize() {
        return texs.size;
    }

    public void bind() {
        if (first) {
            for (BufferTexture tex : texs) {
                tex.bind(textureBinder);
            }
        }
        Gdx.gl30.glDrawBuffers(ibuffer.capacity(), ibuffer);
        Gdx.gl30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        Gdx.gl30.glClearDepthf(1.0f);
        Gdx.gl30.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT |
                (Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0));
    }

    public abstract void draw(ModelBatch mbatch, PerspectiveCamera cam);

    public void setupTextures(int offset) {
        texs = new Array<>(ibuffer.capacity() + 1);
        for (int i = 0; i < ibuffer.capacity(); i++) {
            BufferTexture tex;
            if (shaderInfo.isDepth()) {
                tex = new BufferTexture(GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, offset + i, shaderInfo.getRenderTargets()[i].name);
            } else {
                tex = new BufferTexture(offset + i, shaderInfo.getRenderTargets()[i].name);
            }
            tex.bind(offset + i);
            texs.add(tex);
        }
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
    }

}
