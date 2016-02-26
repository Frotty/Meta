package de.fatox.meta.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import de.fatox.meta.Meta;

public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setResizable(true);
        config.setTitle("Meta");
        config.useOpenGL3(true, 3, 2);
        new Lwjgl3Application(new Meta(), config);
    }
}
