package de.fatox.meta.ui.windows;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.util.adapter.SimpleListAdapter;
import com.kotcrab.vis.ui.widget.ListView;
import de.fatox.meta.ide.AssetManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.components.MetaWindow;
import de.fatox.meta.util.GoldenRatio;

/**
 * Created by Frotty on 07.06.2016.
 */
public class AssetManagerWindow extends MetaWindow {
    private class Model {

        FileHandle fileHandle;

        public Model(FileHandle fileHandle) {
            this.fileHandle = fileHandle;
        }

        @Override
        public String toString() {
            return fileHandle.nameWithoutExtension();
        }

    }
    @Inject
    private AssetManager assetManager;

    public AssetManagerWindow() {
        super("Asset Manager", true, true);
        setSize(Gdx.graphics.getWidth() * GoldenRatio.A, 256);
    }

    private void createFolderView() {
        if (assetManager.getCurrentChildFolders() != null || assetManager.getCurrentChildFiles() != null) {
            Array<FileHandle> currentChildFolders = assetManager.getCurrentChildFolders();
            Array<Model> models = new Array<>();
            for (FileHandle child : currentChildFolders) {
                models.add(new Model(child));
            }
            SimpleListAdapter<Model> adapter = new SimpleListAdapter<>(models);
            ListView<Model> view = new ListView<>(adapter);
            add(view.getMainTable()).grow();
        }
    }

    public void refresh() {
        setPosition(Math.round((getStage().getWidth() - getWidth()) / 2), 128);
        createFolderView();
    }


}
