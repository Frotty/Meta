package de.fatox.meta.api.graphics;

public interface Renderer {
    void render(float x, float y, float width, float height);

    void rebuild(int width, int height);

    void rebuildCache();
}