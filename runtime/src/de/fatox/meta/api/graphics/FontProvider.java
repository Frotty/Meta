package de.fatox.meta.api.graphics;

import com.badlogic.gdx.graphics.g2d.BitmapFont;

public interface FontProvider {
    BitmapFont getFont(int size);

    void write(float x, float y, String text, int size);
}
