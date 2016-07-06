package de.fatox.meta.ide;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.Meta;
import de.fatox.meta.api.dao.MetaProjectData;

/**
 * Created by Frotty on 07.06.2016.
 */
public class AssetDiscoverer {

    private FileHandle root;

    private FileHandle currentFolder;

    private Array<FileHandle> currentChildFolders;

    private Array<FileHandle> currentChildFiles;

    public AssetDiscoverer() {
        Meta.inject(this);
    }

    public void setFromProject(MetaProjectData metaProjectData) {
        root = metaProjectData.root.child("assets");
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
    }

    public void openFile(String s) {

    }

    public FileHandle getCurrentFolder() {
        return currentFolder;
    }
}
