package de.fatox.meta.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import de.fatox.meta.Meta;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.model.MetaAudioVideoData;
import de.fatox.meta.assets.MetaData;
import de.fatox.meta.injection.Inject;

import static de.fatox.meta.Meta.changeScreen;

public class SplashScreen extends ScreenAdapter {
    @Inject
    private MetaData metaData;
    boolean b = false;
    @Inject
    private AssetProvider assetProvider;
    @Inject
    private SpriteBatch spriteBatch;
    private Sprite sprite;

    @Override
    public void show() {
        Meta.inject(this);
        FileHandle internal = Gdx.files.internal("textures/meta_logo.png");
        sprite = new Sprite(new Texture(internal));
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();

        sprite.setPosition(width / 2 - sprite.getWidth() / 2, height / 2 - sprite.getHeight() / 2);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.146f, 0.146f, 0.147f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        spriteBatch.begin();
        sprite.draw(spriteBatch);
        spriteBatch.end();
        if (b) {
            assetProvider.loadAssetsFromFolder(Gdx.files.internal("data/"));
            if (!metaData.has("audioVideoData")) {
                metaData.save("audioVideoData", new MetaAudioVideoData());
            }
            assetProvider.load("roxpack.atlas", TextureAtlas.class);
            MetaAudioVideoData audioVideoData = metaData.get("audioVideoData", MetaAudioVideoData.class);
            changeScreen(new MetaEditorScreen());
            audioVideoData.apply();
        }
        b = true;
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }
}
