package de.fatox.meta.ide;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import de.fatox.meta.api.model.MetaProjectData;

public interface ProjectManager {

	MetaProjectData getCurrentProject();

	MetaProjectData loadProject(FileHandle projectFile);

	void saveProject(MetaProjectData projectData);

	void newProject(FileHandle location, MetaProjectData projectData);

	boolean verifyProjectFile(FileHandle file);

	FileHandle getCurrentProjectRoot();

	<T> T get(String key, Class<T> type);

	FileHandle save(String key, Object obj);

	String relativize(FileHandle fh);

	void addOnLoadListener(EventListener listener);
}
