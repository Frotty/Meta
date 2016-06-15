package de.fatox.meta.ui.windows;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.building.GridTableBuilder;
import com.kotcrab.vis.ui.widget.ListView;
import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import de.fatox.meta.ide.AssetManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.FolderListAdapter;
import de.fatox.meta.ui.components.MetaClickListener;
import de.fatox.meta.ui.components.MetaTextButton;
import de.fatox.meta.util.GoldenRatio;

/**
 * Created by Frotty on 07.06.2016.
 */
public class AssetManagerWindow extends MetaWindow {
    @Inject
    private AssetManager assetManager;
    private FolderListAdapter<Model> adapter;
    private ScrollPane filePane;

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

    public AssetManagerWindow() {
        super("Asset Manager", true, true);
        setSize(Gdx.graphics.getWidth() * GoldenRatio.A, 256);
        left();
        createFolderView();
        add(new Separator()).width(2).growY();
        createFilePane();
        setPosition(Math.round(Gdx.graphics.getWidth() - getWidth()) / 2, 128);
    }

    private void createFilePane() {
        if (assetManager.getCurrentChildFiles() == null) {
            return;
        }
        GridTableBuilder tableBuilder = new GridTableBuilder((int) (getWidth() - 128));
        for (FileHandle file : assetManager.getCurrentChildFiles()) {
            MetaTextButton metaTextButton = new MetaTextButton(file.name());
            metaTextButton.addListener(new MetaClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
//                    assetManager.openFolder(String.valueOf(metaTextButton.getText()));
                }
            });
            metaTextButton.setSize(78, 78);
            tableBuilder.append(metaTextButton);
        }

        if(filePane == null) {
            filePane = new VisScrollPane(tableBuilder.build());
            add(filePane).growY();
        }else {
            filePane.clear();
            filePane.setWidget(tableBuilder.build());
        }
    }

    private void createFolderView() {
        Array<Model> models = new Array<>();
        adapter = new FolderListAdapter<>(models);
        ListView<Model> view = new ListView<>(adapter);
        view.getMainTable().defaults().growY();
        add(view.getMainTable()).pad(4, 8, 4, 8).growY();
    }

    public void refresh() {
        setPosition(Math.round((getStage().getWidth() - getWidth()) / 2), 128);
        adapter.clear();
        for (FileHandle child : assetManager.getCurrentChildFolders()) {
            adapter.add(new Model(child));
        }
        createFilePane();
    }


}
