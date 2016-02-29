package de.fatox.meta.api.ide.ui;

import com.badlogic.gdx.InputProcessor;

public interface UIRenderer {

    void update();

    void draw();

    void resize(int width, int height);

    InputProcessor getInputProcessor();
}
