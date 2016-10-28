package de.fatox.meta.graphics.font;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.IntMap;
import de.fatox.meta.Meta;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.graphics.FontProvider;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;

public class MetaFontProvider implements FontProvider {
    @Inject
    @Log
    private Logger log;
    private final IntMap<BitmapFont> bitmapFontMap = new IntMap<>();
    private final FreeTypeFontGenerator generator;

    @Inject
    private AssetProvider assetProvider;
    @Inject
    private SpriteBatch spriteBatch;

    @Inject
    public MetaFontProvider() {
        Meta.inject(this);
        log.debug("MetaFontProvider", "init");
        generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Montserrat.ttf"));
    }

    @Override
    public BitmapFont getFont(int size) {
        if (!bitmapFontMap.containsKey(size)) {
            generateFont(size);
        }
        return bitmapFontMap.get(size);
    }

    @Override
    public void write(float x, float y, String text, int size) {
        spriteBatch.setColor(Color.WHITE);
        spriteBatch.enableBlending();
        spriteBatch.setShader(null);
        spriteBatch.begin();
        getFont(size).draw(spriteBatch, text, x, y);
        spriteBatch.end();
    }

    private BitmapFont generateFont(int size) {
        FreeTypeFontGenerator.FreeTypeFontParameter param = defaultFontParam();
        param.size = size;
        BitmapFont value = generator.generateFont(param);
        return bitmapFontMap.put(size, value);
    }

    private FreeTypeFontGenerator.FreeTypeFontParameter defaultFontParam() {
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.incremental = true;
        param.minFilter = Texture.TextureFilter.Linear;
        param.magFilter = Texture.TextureFilter.Linear;
        param.hinting = FreeTypeFontGenerator.Hinting.Medium;
        return param;
    }
}
