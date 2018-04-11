package de.fatox.meta.api.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.scenes.scene2d.Actor;

public interface UIRenderer {
    void load();

    void addActor(Actor actor);

    void update();

    void draw();

    void resize(int width, int height);

    Camera getCamera();
}
