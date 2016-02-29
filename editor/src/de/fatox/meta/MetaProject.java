package de.fatox.meta;

import de.fatox.meta.Meta;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.api.ide.persist.Persist;
import de.fatox.meta.api.ide.persist.PersistentValue;

public class MetaProject {

    @Persist(key = "Project Name", defaultValue = "New Project")
    public PersistentValue<String> projectName;

    @Persist(key = "Project Renderer", defaultValue = "BufferedRenderer")
    public PersistentValue<Renderer> projectRenderer;

    public MetaProject() {
        Meta.inject(this);
    }
}
