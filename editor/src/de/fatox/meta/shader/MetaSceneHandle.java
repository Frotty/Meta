package de.fatox.meta.shader;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import de.fatox.meta.Meta;
import de.fatox.meta.Primitives;
import de.fatox.meta.api.dao.MetaSceneData;
import de.fatox.meta.api.entity.EntityManager;
import de.fatox.meta.entity.Meta3DEntity;
import de.fatox.meta.entity.MetaEntityManager;
import de.fatox.meta.injection.Inject;

/**
 * Created by Frotty on 18.04.2017.
 */
public class MetaSceneHandle {
    public MetaSceneData data;
    public EntityManager<Meta3DEntity> entityManager = new MetaEntityManager();

    private FileHandle sceneFile;
    private ShaderComposition shaderComposition;
    @Inject
    private Primitives primitives;

    public MetaSceneHandle(MetaSceneData sceneData, ShaderComposition shaderComposition, FileHandle fileHandle) {
        Meta.inject(this);
        this.data = sceneData;
        this.shaderComposition = shaderComposition;
        this.sceneFile = fileHandle;
        entityManager.addEntity(new Meta3DEntity(Vector3.Zero, primitives.getLinegrid()));
    }

    public ShaderComposition getShaderComposition() {
        return shaderComposition;
    }

    public FileHandle getSceneFile() {
        return sceneFile;
    }
}
