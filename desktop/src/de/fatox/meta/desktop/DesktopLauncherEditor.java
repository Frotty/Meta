package de.fatox.meta.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import de.fatox.meta.EditorMeta;

public class DesktopLauncherEditor {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowPosition(32, 64);
        config.setResizable(true);
        config.setTitle("Meta");
        config.useOpenGL3(true, 3, 2);
        EditorMeta editorMeta = new EditorMeta();
        new Lwjgl3Application(editorMeta, config);
    }
}
