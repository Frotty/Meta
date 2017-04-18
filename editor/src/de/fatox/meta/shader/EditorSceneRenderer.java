package de.fatox.meta.shader;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import de.fatox.meta.Meta;
import de.fatox.meta.api.entity.EntityManager;
import de.fatox.meta.api.graphics.RenderBufferHandle;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.entity.Meta3DEntity;
import de.fatox.meta.graphics.renderer.FullscreenQuad;
import de.fatox.meta.ide.ProjectManager;
import de.fatox.meta.ide.SceneManager;
import de.fatox.meta.injection.Inject;

/**
 * Created by Frotty on 17.04.2017.
 */
public class EditorSceneRenderer implements Renderer{
    @Inject
    private SpriteBatch batch;
    @Inject
    private MetaShaderComposer shaderComposer;
    @Inject
    private ProjectManager projectManager;
    @Inject
    private PerspectiveCamera cam;
    @Inject
    private EntityManager<Meta3DEntity> entityManager;
    @Inject
    private SceneManager sceneManager;

    private ModelCache modelCache = new ModelCache();
    private ModelBatch modelBatch;

    private FullscreenQuad fsquad = new FullscreenQuad(1);
    private FullscreenQuad compositeQuad = new FullscreenQuad(1);

    private RenderContext renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.WEIGHTED));

    private ShaderComposition lastComposition;

    public EditorSceneRenderer() {
        Meta.inject(this);
    }

    @Override
    public void render(float x, float y) {

    }

    @Override
    public void rebuild(int width, int height) {
        ShaderComposition currentComposition = shaderComposer.getCurrentComposition();
        if(currentComposition != null) {
            if(currentComposition == lastComposition) {
                resize(width, height);
            } else {
                create(width, height);
            }
        }
    }

    private void create(int width, int height) {
        resizeCam(width, height);
        ShaderComposition composition = shaderComposer.getCurrentComposition();

        for(RenderBufferHandle bufferHandle : composition.getBufferHandles()) {
            int targetsNum = bufferHandle.metaShader.shaderHandle.targets.size;
            if (targetsNum > 1) {
                // MRT Shader
            } else {
                // Regular Framebuffer

            }
        }
    }

    private void resize(int width, int height) {
        resizeCam(width, height);
    }

    private void resizeCam(int width, int height) {
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
    }

    @Override
    public void rebuildCache() {

    }
}
