package de.fatox.meta.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import de.fatox.meta.Meta;
import de.fatox.meta.api.dao.MetaRenderData;
import de.fatox.meta.api.dao.RenderBufferData;
import de.fatox.meta.api.graphics.RenderBufferHandle;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.ide.ProjectManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.windows.ShaderComposerWindow;

/**
 * Created by Frotty on 10.04.2017.
 */
public class MetaShaderComposer {
    public static final String META_COMP_SUFFIX = ".mco";
    @Inject
    private ProjectManager projectManager;
    @Inject
    private Json json;
    @Inject
    private UIManager uiManager;
    @Inject
    private MetaShaderLibrary shaderLibrary;

    private Array<ShaderComposition> compositions = new Array<>(2);

    private ShaderComposition currentComposition;

    public MetaShaderComposer() {
        Meta.inject(this);
        Gdx.app.postRunnable(() -> loadProjectCompositions());
    }

    public void loadProjectCompositions() {
        if (projectManager.getCurrentProject() != null) {
            FileHandle compositionFolder = projectManager.getCurrentProjectRoot().child("meta/compositions/");
            if (compositionFolder.exists()) {
                for (FileHandle metaComp : compositionFolder.list(pathname -> pathname.getName().endsWith(META_COMP_SUFFIX))) {
                    MetaRenderData compositionData = json.fromJson(MetaRenderData.class, metaComp.readString());
                    if (compositionData != null) {
                        addComposition(new ShaderComposition(compositionData));
                    }
                }
            }
        }
    }

    public void addComposition(ShaderComposition composition) {
        if(composition != null) {
            compositions.add(composition);
            saveComposition(composition);
            ShaderComposerWindow window = uiManager.getWindow(ShaderComposerWindow.class);
            if(window != null) {
                window.addComposition(composition);
            }
            currentComposition = composition;
        }
    }

    private FileHandle saveComposition(ShaderComposition composition) {
        return projectManager.save("meta/compositions/" + composition.data.name + META_COMP_SUFFIX, composition.data);
    }

    public ShaderComposition getComposition(String compName) {
        for(ShaderComposition comp : compositions) {
            if(comp.data.name.equals(compName)) {
                return comp;
            }
        }
        return null;
    }


    public Array<ShaderComposition> getCompositions() {
        return compositions;
    }

    public ShaderComposition getCurrentComposition() {
        return currentComposition;
    }

    public void setCurrentComposition(ShaderComposition currentComposition) {
        this.currentComposition = currentComposition;
    }

    public RenderBufferHandle addRenderBuffer(RenderBufferData data) {
        RenderBufferHandle bufferHandle = new RenderBufferHandle(data, null);
        currentComposition.addBufferHandle(bufferHandle);
        saveComposition(currentComposition);
        return bufferHandle;
    }
}
