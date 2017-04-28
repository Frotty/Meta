package de.fatox.meta.shader;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.Meta;
import de.fatox.meta.Primitives;
import de.fatox.meta.api.dao.MetaData;
import de.fatox.meta.api.dao.RenderBufferData;
import de.fatox.meta.api.graphics.RenderBufferHandle;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.entity.Meta3DEntity;
import de.fatox.meta.graphics.renderer.FullscreenQuad;
import de.fatox.meta.ide.ProjectManager;
import de.fatox.meta.injection.Inject;

/**
 * Created by Frotty on 17.04.2017.
 */
public class EditorSceneRenderer implements Renderer {
    @Inject
    private SpriteBatch batch;
    @Inject
    private MetaShaderComposer shaderComposer;
    @Inject
    private ProjectManager projectManager;
    @Inject
    private PerspectiveCamera cam;
    @Inject
    private Primitives primitives;
    @Inject
    private MetaData metaData;
    private final Meta3DEntity grid;

    private MetaSceneHandle sceneHandle;

    private ModelCache modelCache = new ModelCache();
    private ModelBatch modelBatch;

    private FullscreenQuad fsquad = new FullscreenQuad(1);
    private FullscreenQuad compositeQuad = new FullscreenQuad(1);

    private RenderContext renderContext;

    private ShaderComposition lastComposition;

    public EditorSceneRenderer(MetaSceneHandle sceneHandle) {
        Meta.inject(this);
        grid = new Meta3DEntity(Vector3.Zero, primitives.getLinegrid());
        modelCache.begin();
        modelCache.add(grid.actorModel);
        modelCache.end();
        this.sceneHandle = sceneHandle;
    }

    @Override
    public void render(float x, float y) {
        if (sceneHandle.getShaderComposition() != null) {
            Array<RenderBufferHandle> bufferHandles = sceneHandle.getShaderComposition().getBufferHandles();
            renderContext.begin();

            for (RenderBufferHandle bufferHandle : bufferHandles) {
                bufferHandle.begin();
                if (bufferHandle.data.inType == RenderBufferData.IN.GEOMETRY) {
                    modelBatch.begin(cam);
                    modelBatch.render(modelCache, bufferHandle.metaShader);
                    modelBatch.end();
                }
                bufferHandle.end();
            }

            renderContext.end();

            if (sceneHandle.data.showGrid) {

            }
            debugAll(x, y, bufferHandles);
        }
    }

    private void debugAll(float x, float y, Array<RenderBufferHandle> bufferHandles) {
        batch.disableBlending();
        batch.begin();
        float debugScreens = bufferHandles.size;
        int count = 0;
        for (RenderBufferHandle bufferHandle : bufferHandles) {
            float height = bufferHandle.getHeight();
            float width = bufferHandle.getWidth();
            Array<Texture> colorTextures = bufferHandle.getColorTextures();
            for (Texture texture : colorTextures) {
                batch.draw(texture, x + (width / debugScreens) * count, y, width / debugScreens, height / debugScreens / 2, 0f, 0f, 1f, 1f);
                count++;
            }

        }
        batch.end();
    }

    @Override
    public void rebuild(int width, int height) {
        ShaderComposition currentComposition = sceneHandle.getShaderComposition();
        if (currentComposition != null) {
            if (currentComposition == lastComposition) {
                resize(width, height);
            } else {
                create(width, height);
            }
        }
    }

    private void create(int width, int height) {
        renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.WEIGHTED));
        modelBatch = new ModelBatch(renderContext);
        resize(width, height);
    }

    private void resize(int width, int height) {
        resizeCam(width, height);

        ShaderComposition composition = shaderComposer.getCurrentComposition();
        for (RenderBufferHandle bufferHandle : composition.getBufferHandles()) {
            bufferHandle.rebuild(width, height);
        }
    }

    private void resizeCam(int width, int height) {
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
    }

    @Override
    public void rebuildCache() {
        modelCache.begin();
        for (Meta3DEntity entity : sceneHandle.entityManager.getStaticEntities()) {
            modelCache.add(entity.actorModel);
        }
        modelCache.end();
    }
}
