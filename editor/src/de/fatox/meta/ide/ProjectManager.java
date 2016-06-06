package de.fatox.meta.ide;

import com.badlogic.gdx.files.FileHandle;

public interface ProjectManager {

    MetaProjectData getCurrentProject();

    MetaProjectData loadProject(FileHandle projectFile);

    void saveProject(MetaProjectData projectData);

    boolean verifyProjectFile(FileHandle file);
}
