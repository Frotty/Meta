package de.fatox.meta.ui.windows;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.kotcrab.vis.ui.widget.*;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.dao.AssetDiscovererData;
import de.fatox.meta.ide.AssetDiscoverer;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.ui.FolderListAdapter;
import de.fatox.meta.ui.components.MetaClickListener;
import de.fatox.meta.ui.components.MetaIconTextButton;

/**
 * Created by Frotty on 07.06.2016.
 */
@Singleton
public class AssetDiscovererWindow extends MetaWindow {
    private static String TAG = "adwSettings";

    @Inject
    private AssetProvider assetProvider;
    @Inject
    private AssetDiscoverer assetDiscoverer;

    private FolderListAdapter<FolderModel> adapter;
    private ScrollPane filePane;
    private ListView view;

    private VisTable toolbarTable = new VisTable();
    private VisTable fileViewTable = new VisTable();

    private boolean selectionMode = false;
    private SelectListener listener;

    private AssetDiscovererData data;

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
        loadLastFolder();

        setup();
        refresh();
    }

    private void loadLastFolder() {
        assetDiscoverer.setRoot("");
        if(uiManager.metaHas(TAG)) {
            data = uiManager.metaGet(TAG, AssetDiscovererData.class);
            assetDiscoverer.openChild(data.getLastFolder());
        }
    }

    @Override
    public void addAction(Action action) {
        super.addAction(action);
    }

    private void setup() {
        adapter = new FolderListAdapter<>(new Array<FolderModel>());
        view = new ListView<>(adapter);
        view.getMainTable().defaults().pad(2);
        view.setItemClickListener(item -> assetDiscoverer.openFolder(((FolderModel) item).fileHandle));
        contentTable.top().left();
        contentTable.row().left().top().height(24);
        contentTable.add(toolbarTable).growX();
        contentTable.row().height(1);
        contentTable.add(new Separator()).growX();
        contentTable.row();
        contentTable.add(fileViewTable).left().grow();
        createToolbarBar();
        createFileView();
    }

    private void createToolbarBar() {
        toolbarTable.left().top();
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
        fileViewTable.add(view.getMainTable()).growY().pad(2).minWidth(128);
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
            MetaIconTextButton fileButton = new MetaIconTextButton(file.name(), assetProvider.getDrawable("ui/appbar.page.text.png"), 78);
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
                }
            });
            visTable2.add(fileButton).top().size(78, 78);
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
        if (! assetDiscoverer.getCurrentFolder().path().equals(assetDiscoverer.getRoot().path())) {
            adapter.add(new FolderModel(assetDiscoverer.getCurrentFolder().parent()) {
                @Override
                public String toString() {
                    return "../";
                }

            });
        }
        for (FileHandle child : assetDiscoverer.getCurrentChildFolders()) {
            adapter.add(new FolderModel(child));
        }
        view.rebuildView();
    }

    public void refresh() {
        createFilePane();
        refreshFolderView();
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
