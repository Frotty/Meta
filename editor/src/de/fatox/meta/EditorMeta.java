package de.fatox.meta;

import de.fatox.meta.modules.MetaEditorModule;
import de.fatox.meta.modules.MetaUIModule;
import de.fatox.meta.screens.SplashScreen;
import de.fatox.meta.sound.MetaMusicPlayer;
import de.fatox.meta.sound.MetaSoundPlayer;

public class EditorMeta extends Meta {

    private MetaMusicPlayer metaMusicPlayer;

    private MetaSoundPlayer metaSoundPlayer;


    public EditorMeta() {
        super();
        addModule(new MetaEditorModule());
        addModule(new MetaUIModule());
    }

    @Override
    public void create() {
        inject(this);
        changeScreen(new SplashScreen());
    }

}
