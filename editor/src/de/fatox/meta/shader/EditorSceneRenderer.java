package de.fatox.meta.shader;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.Meta;
import de.fatox.meta.Primitives;
import de.fatox.meta.assets.MetaData;
import de.fatox.meta.api.dao.RenderBufferData;
import de.fatox.meta.api.graphics.RenderBufferHandle;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.entity.Meta3DEntity;
import de.fatox.meta.graphics.renderer.FullscreenQuad;
import de.fatox.meta.ide.ProjectManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.components.MetaLabel;

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
    @Inject
    private UIManager uiManager;
    private final Meta3DEntity grid;

    private MetaSceneHandle sceneHandle;

    private ModelCache modelCache = new ModelCache();
    private ModelBatch modelBatch;

    private FullscreenQuad fsquad = new FullscreenQuad(1);
    private FullscreenQuad compositeQuad = new FullscreenQuad(1);

    private RenderContext renderContext;
    private ShaderComposition lastComposition;

    public EditorSceneRenderer() {
        Meta.inject(this);
        grid = new Meta3DEntity(Vector3.Zero, primitives.getLinegrid());
    }

    @Override
    public void render(float x, float y) {
        if(renderContext == null) return;
        if (sceneHandle.getShaderComposition() == null) {
            Table table = new Table();
            table.add(new MetaLabel("No composition selected", 20)).pad(128).center();
            uiManager.addTable(table, true, true);
        } else {
            Array<RenderBufferHandle> bufferHandles = sceneHandle.getShaderComposition().getBufferHandles();

            for (RenderBufferHandle bufferHandle : bufferHandles) {
                renderContext.begin();
                bufferHandle.begin();


                if (bufferHandle.getData().getInType() == RenderBufferData.IN.GEOMETRY) {
                    modelBatch.begin(cam);
                    modelBatch.render(modelCache, bufferHandle.getMetaShader());
                    modelBatch.end();
                }
                bufferHandle.end();
                renderContext.end();
            }
            renderContext.begin();
            modelBatch.begin(cam);
            modelBatch.render(modelCache, sceneHandle.getShaderComposition().getOutputBuffer().getMetaShader());
            modelBatch.end();

            renderContext.end();
            debugAll(x, y, bufferHandles);
        }
    }

    private void debugAll(float x, float y, Array<RenderBufferHandle> bufferHandles) {
        batch.disableBlending();
        batch.begin();
        float debugScreens = 1;
        for (RenderBufferHandle bufferHandle : bufferHandles) {
            for (Texture ignored : bufferHandle.getColorTextures()) {
                debugScreens++;
            }

        }
        int count = 0;
        for (RenderBufferHandle bufferHandle : bufferHandles) {
            float height = bufferHandle.getHeight();
            float width = bufferHandle.getWidth();
            Array<Texture> colorTextures = bufferHandle.getColorTextures();
            for (Texture texture : colorTextures) {
                batch.draw(texture, x + (width / debugScreens) * count * 0.75f, y, width / debugScreens * 0.75f, height / debugScreens * 0.75f, 0f, 0f, 1f, 1f);
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
                lastComposition = currentComposition;
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
        if (sceneHandle.data.getShowGrid()) {
            modelCache.add(grid.actorModel);
        }
        for (Meta3DEntity entity : sceneHandle.entityManager.getStaticEntities()) {
            modelCache.add(entity.actorModel);
        }
        modelCache.end();
    }


    public void setSceneHandle(MetaSceneHandle sceneHandle) {
        this.sceneHandle = sceneHandle;
        rebuildCache();
    }
}
