package de.fatox.meta.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import de.fatox.meta.EditorMeta;

public class Launcher {
	public static void main(String[] args) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setResizable(true);
		config.setTitle("Meta");
		config.setWindowIcon("icon.png");
		EditorMeta editorMeta = new EditorMeta(new WindowPosModifier());
		MetaDesktopLauncher.INSTANCE.init(config, editorMeta);
	}
}
