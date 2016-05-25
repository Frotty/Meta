package de.fatox.meta.api.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;

public interface UIRenderer {

    void addActor(Actor actor);

    void update();

    void draw();

    void resize(int width, int height);

    Stage getStage();

}
