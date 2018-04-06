package de.fatox.meta.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import de.fatox.meta.EditorMeta;

public class DesktopLauncherEditor {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowPosition(2, 24);
        config.setResizable(true);
        config.setTitle("Meta");
        config.setWindowedMode(1280, 720);
        config.useOpenGL3(true, 3, 2);
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
        EditorMeta editorMeta = new EditorMeta();
        new Lwjgl3Application(editorMeta, config);
    }
}
