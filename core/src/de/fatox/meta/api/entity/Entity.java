package de.fatox.meta.api.entity;

import com.badlogic.gdx.math.Vector;

public interface Entity<DIMENSION extends Vector> {

    int getId();

    DIMENSION getCenter();

    void update();

    void draw();

}
