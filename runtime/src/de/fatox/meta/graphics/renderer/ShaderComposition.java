package de.fatox.meta.graphics.renderer;

import de.fatox.meta.api.dao.MetaRenderData;

/**
 * Created by Frotty on 10.04.2017.
 */
public class ShaderComposition {
    public MetaRenderData data;

    public ShaderComposition(String name) {
        data = new MetaRenderData(name);
    }
}
