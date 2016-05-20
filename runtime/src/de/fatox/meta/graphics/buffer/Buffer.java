package de.fatox.meta.graphics.buffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import de.fatox.meta.api.graphics.ShaderInfo;

import java.nio.IntBuffer;

public abstract class Buffer {
    private final ShaderInfo.RenderTarget[] renderTargets;
    public IntBuffer ibuffer;
    private Array<BufferTexture> texs;
    private boolean first = true;

    public Buffer(int offset, ShaderInfo.RenderTarget... renderTargets) {
        this.renderTargets = renderTargets;
        ibuffer = BufferUtils.newIntBuffer(renderTargets.length);
        for (int i = 0; i < renderTargets.length; i++) {
            ibuffer.put(GL30.GL_COLOR_ATTACHMENT0 + offset + i);
            System.out.println("Put into Buffer" + (GL30.GL_COLOR_ATTACHMENT0 + offset + i));
        }
        ibuffer.position(0);
    }

    public int getSize() {
        return texs.size;
    }

    public void bind() {
        if (first) {
            int i = 0;
            for (BufferTexture tex : texs) {
                tex.bind(i);
                i++;
            }
        }
        Gdx.gl30.glDrawBuffers(ibuffer.capacity(), ibuffer);
        Gdx.gl30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        Gdx.gl30.glClearDepthf(1.0f);
        Gdx.gl30.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
    }

    public abstract void draw(ModelBatch mbatch, PerspectiveCamera cam);

    public void setupTextures(int offset) {
        texs = new Array<>(ibuffer.capacity() + 1);
        for (int i = 0; i < ibuffer.capacity(); i++) {
            BufferTexture tex = new BufferTexture(offset + i, renderTargets[i].name);
            tex.bind(offset + i);
            System.out.println("bound to " + (offset + i));
            texs.add(tex);
        }
        BufferTexture tex = new BufferTexture(GL30.GL_DEPTH_COMPONENT32F, GL30.GL_FLOAT, offset + ibuffer.capacity(), true, "depth");
        tex.bind(offset + ibuffer.capacity());
        texs.add(tex);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
    }

}
