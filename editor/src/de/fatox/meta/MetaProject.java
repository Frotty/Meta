package de.fatox.meta;

import com.google.gson.annotations.SerializedName;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.ide.persist.PersistentValue;

public class MetaProject {

    @SerializedName("Project Name")
    public PersistentValue<String> projectName;

    @SerializedName("Project Renderer")
    public PersistentValue<Renderer> projectRenderer;

    public MetaProject() {
        Meta.inject(this);
    }
}
