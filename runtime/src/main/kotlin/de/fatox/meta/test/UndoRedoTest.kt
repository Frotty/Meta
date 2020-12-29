package de.fatox.meta.test;

import com.badlogic.gdx.Game;

public class UndoRedoTest extends Game {

    @Override
    public void create() {

    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        System.out.println("w" + width);
    }
}
