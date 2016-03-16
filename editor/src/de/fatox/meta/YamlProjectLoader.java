package de.fatox.meta;

import com.badlogic.gdx.files.FileHandle;
import de.fatox.meta.ide.ProjectLoader;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

public class YamlProjectLoader implements ProjectLoader {

    @Override
    public void loadProject(FileHandle projectFile) {
        Yaml yaml = new Yaml();
        String document = projectFile.readString();
        Map data = (Map) yaml.load(document);

    }
}
