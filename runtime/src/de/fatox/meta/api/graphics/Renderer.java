package de.fatox.meta.api.graphics;

public interface Renderer {
    void render(float x, float y);

    void rebuild(int width, int height);

    void rebuildCache();
}