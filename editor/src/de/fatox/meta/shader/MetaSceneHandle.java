package de.fatox.meta.shader;

import de.fatox.meta.api.dao.MetaSceneData;
import de.fatox.meta.api.entity.EntityManager;
import de.fatox.meta.entity.Meta3DEntity;
import de.fatox.meta.entity.MetaEntityManager;

/**
 * Created by Frotty on 18.04.2017.
 */
public class MetaSceneHandle {
    public MetaSceneData data;
    public EntityManager<Meta3DEntity> entityManager = new MetaEntityManager();
    private ShaderComposition shaderComposition;

    public ShaderComposition getShaderComposition() {
        return shaderComposition;
    }
}
