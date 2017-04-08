package de.fatox.meta.api.dao;

import com.badlogic.gdx.utils.Array;
import de.fatox.meta.util.StringUtil;

/**
 * Created by Frotty on 02.06.2016.
 */
public class MetaProjectData {
    public static final String PROJECT_FILE_NAME = "metaproject.json";

    public String name = "unknown";
    public Array<MetaTabData> openTabs;

    public MetaProjectData() {
    }

    public MetaProjectData(String name) {
        this.name = name;
        openTabs = new Array<>();
    }

    public boolean isValid() {
        return ! StringUtil.isBlank(name);
    }
}
