package de.fatox.meta.api.dao;

import com.badlogic.gdx.utils.Array;

/**
 * Created by Frotty on 08.04.2017.
 */
public class RenderBufferData {
    public Array<String> renderTargets = new Array<>(1);

    public RenderBufferData() {
    }

    public boolean hasMultipleTargets() {
        return renderTargets.size > 1;
    }
}
