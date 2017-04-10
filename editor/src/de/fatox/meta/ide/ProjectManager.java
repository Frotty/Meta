package de.fatox.meta.ide;

import com.badlogic.gdx.files.FileHandle;
import de.fatox.meta.api.dao.MetaProjectData;

public interface ProjectManager {

    MetaProjectData getCurrentProject();

    MetaProjectData loadProject(FileHandle projectFile);

    void saveProject(MetaProjectData projectData);

    boolean verifyProjectFile(FileHandle file);

    FileHandle getCurrentProjectRoot();

    <T> T get(String key, Class<T> type);

    void save(String key, Object obj);

    String relativize(FileHandle fh);
}
