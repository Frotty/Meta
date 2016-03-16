package de.fatox.meta;

import com.badlogic.gdx.files.FileHandle;
import com.google.gson.Gson;
import de.fatox.meta.ide.ProjectLoader;
import de.fatox.meta.task.MetaTaskHistory;

public class JsonProjectLoader implements ProjectLoader {

    @Override
    public void loadProject(FileHandle projectFile) {
        MetaTaskHistory.
        Gson gson = new Gson();
        gson.fromJson(projectFile.reader(), MetaProject.class);
    }
}
