package de.fatox.meta.ide;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import de.fatox.meta.Meta;
import de.fatox.meta.api.dao.MetaProjectData;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.windows.AssetDiscovererWindow;

/**
 * Created by Frotty on 07.06.2016.
 */
public class AssetDiscoverer {
    @Inject
    private UIManager uiManager;

    private FileHandle root;

    private FileHandle currentFolder;

    private Array<FileHandle> currentChildFolders;

    private Array<FileHandle> currentChildFiles;

    private ObjectMap<String, AssetOpenListener> fileOpenListeners = new ObjectMap<>();

    public AssetDiscoverer() {
        Meta.inject(this);
    }

    public void setFromProject(MetaProjectData metaProjectData) {
        root = metaProjectData.root;
        currentFolder = root;
        refresh();
    }

    public void openFolder(FileHandle fileHandle) {
        currentFolder = fileHandle;
        refresh();
    }

    public void openChild(String name) {
        currentFolder = currentFolder.child(name);
        refresh();
    }

    public Array<FileHandle> getCurrentChildFolders() {
        return currentChildFolders;
    }

    public Array<FileHandle> getCurrentChildFiles() {
        return currentChildFiles;
    }

    public void refresh() {
        currentChildFolders = new Array<>();
        currentChildFiles = new Array<>();
        for (FileHandle child : currentFolder.list()) {
            if (child.isDirectory()) {
                currentChildFolders.add(child);
            } else {
                currentChildFiles.add(child);
            }
        }
        AssetDiscovererWindow window = uiManager.getWindow(AssetDiscovererWindow.class);
        if (window != null) {
            window.refresh();
        }
    }

    public void openFile(FileHandle fileHandle) {
        if(fileOpenListeners.containsKey(fileHandle.extension())) {
            fileOpenListeners.get(fileHandle.extension()).onOpen(fileHandle);
        }
    }

    public FileHandle getCurrentFolder() {
        return currentFolder;
    }

    public void addOpenListener(String extension, AssetOpenListener listener) {
        fileOpenListeners.put(extension, listener);
    }
}
