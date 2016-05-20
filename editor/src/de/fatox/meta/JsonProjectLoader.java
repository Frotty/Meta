package de.fatox.meta;

import com.badlogic.gdx.files.FileHandle;
import com.google.gson.Gson;
import de.fatox.meta.ide.ProjectLoader;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.task.MetaTask;
import de.fatox.meta.task.MetaTaskManager;

public class JsonProjectLoader implements ProjectLoader {

    @Inject
    private MetaTaskManager metaTaskManager;

    public JsonProjectLoader() {
        Meta.inject(this);
    }

    @Override
    public void loadProject(final FileHandle projectFile) {
        metaTaskManager.runTask(new MetaTask() {
            @Override
            public String getName() {
                return "Loading JSON Project";
            }

            @Override
            public void execute() {
                Gson gson = new Gson();
                gson.fromJson(projectFile.reader(), MetaProject.class);
            }

            @Override
            public void reverse() {
            }
        });

    }
}
