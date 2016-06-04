package de.fatox.meta.ide;

import com.badlogic.gdx.files.FileHandle;

public interface ProjectManager {

    MetaProjectData loadProject(FileHandle projectFile);

    void saveProject(MetaProjectData projectData);

}
