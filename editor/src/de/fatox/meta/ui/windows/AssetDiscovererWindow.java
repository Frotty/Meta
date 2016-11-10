package de.fatox.meta.ui.windows;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.kotcrab.vis.ui.widget.*;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.dao.MetaData;
import de.fatox.meta.ide.AssetDiscoverer;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.FolderListAdapter;
import de.fatox.meta.ui.components.MetaClickListener;
import de.fatox.meta.ui.components.MetaTextButton;

/**
 * Created by Frotty on 07.06.2016.
 */
public class AssetDiscovererWindow extends MetaWindow {
    @Inject
    private AssetProvider assetProvider;
    @Inject
    private AssetDiscoverer assetDiscoverer;
    @Inject
    private MetaData metaData;
    private FolderListAdapter<FolderModel> adapter;
    private ScrollPane filePane;
    private ListView view;

    private VisTable toolbarTable = new VisTable();
    private VisTable fileViewTable = new VisTable();

    private boolean selectionMode = false;
    private SelectListener listener;

    private class FolderModel {

        public final FileHandle fileHandle;

        public FolderModel(FileHandle fileHandle) {
            this.fileHandle = fileHandle;
        }

        @Override
        public String toString() {
            return fileHandle.nameWithoutExtension() + "/";
        }

    }

    public AssetDiscovererWindow() {
        super("Asset Discoverer", true, true);
        setSize(500, 200);
        setup();
    }

    @Override
    public void addAction(Action action) {
        super.addAction(action);
        refresh();
    }

    private void setup() {
        adapter = new FolderListAdapter<>(new Array<FolderModel>());
        view = new ListView<>(adapter);
        view.getMainTable().defaults().pad(2);
        view.setItemClickListener(item -> {
            assetDiscoverer.openFolder(((FolderModel) item).fileHandle);
            refresh();
        });
        left();
        row().height(24);
        add(toolbarTable).left().growX();
        row().height(1);
        add(new Separator()).growX();
        row();
        add(fileViewTable).left().grow();
        createToolbarBar();
        createFileView();
    }

    private void createToolbarBar() {
        toolbarTable.left();
        toolbarTable.row().height(24);
        VisImageButton newFileButton = new VisImageButton(assetProvider.getDrawable("ui/appbar.page.add.png"));
        newFileButton.getImage().setScaling(Scaling.fill);
        toolbarTable.add(newFileButton).size(24).left();

        VisImage searchIcon = new VisImage(assetProvider.getDrawable("ui/appbar.page.search.png"));
        searchIcon.setScaling(Scaling.fill);
        toolbarTable.add(searchIcon).size(24).left();

        VisTextField searchTF = new VisTextField();
        toolbarTable.add(searchTF).height(24).growX();
    }

    private void createFileView() {
        fileViewTable.left();
        fileViewTable.add(view.getMainTable()).growY().pad(2);
        fileViewTable.add(new Separator()).width(2).growY();
        createFilePane();
    }

    private void createFilePane() {
        top();
        if (assetDiscoverer.getCurrentChildFiles() == null) {
            return;
        }
        VisTable visTable2 = new VisTable();
        visTable2.defaults().pad(2);
        visTable2.top();
        visTable2.setSize((int) (getWidth() - 128), getHeight() - 64);
        visTable2.row().height(78);
        float counter = 0;
        for (FileHandle file : assetDiscoverer.getCurrentChildFiles()) {
            MetaTextButton fileButton = new MetaTextButton(file.name(), 12);
            fileButton.addListener(new MetaClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (selectionMode) {
                        listener.onSelect(file);
                        listener = null;
                        selectionMode = false;
                    } else {
                        assetDiscoverer.openFile(file);
                    }
                    refresh();
                }
            });
            fileButton.setSize(78, 78);
            visTable2.add(fileButton).top();
            counter += 78;
            if (counter > getWidth() - 128) {
                visTable2.row().height(78);
            }
        }

        if (filePane == null) {
            filePane = new VisScrollPane(visTable2);
            fileViewTable.add(filePane).growY().top().pad(2);
        } else {
            filePane.clear();
            filePane.setWidget(visTable2);
        }
    }

    private void refreshFolderView() {
        adapter.clear();
        adapter.add(new FolderModel(assetDiscoverer.getCurrentFolder().parent()) {
            @Override
            public String toString() {
                return "../";
            }

        });
        for (FileHandle child : assetDiscoverer.getCurrentChildFolders()) {
            adapter.add(new FolderModel(child));
        }
    }

    public void refresh() {
        if (getStage() != null) {
            refreshFolderView();
            createFilePane();
        }
    }

    public void enableSelectionMode(SelectListener selectListener) {
        toFront();
        getStage().setKeyboardFocus(this);
        this.listener = selectListener;
        selectionMode = true;
    }

    public interface SelectListener {
        void onSelect(FileHandle fileHandle);
    }

}
