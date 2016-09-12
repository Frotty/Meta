package de.fatox.meta.graphics.font;

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
    private final FreeTypeFontGenerator.FreeTypeFontParameter param;
    private final FreeTypeFontGenerator generator;

    @Inject
    private AssetProvider assetProvider;
    @Inject
    private SpriteBatch spriteBatch;

    @Inject
    public MetaFontProvider() {
        Meta.inject(this);
        log.debug("NetaFontProvider", "init");
        generator = new FreeTypeFontGenerator(assetProvider.get("fonts/Vulpes.ttf"));
        param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        fontDefaults();
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
        param.size = size;
        return bitmapFontMap.put(param.size, generator.generateFont(param));
    }

    private void fontDefaults() {
        param.genMipMaps = true;
        param.incremental = true;
        param.minFilter = Texture.TextureFilter.MipMapLinearLinear;
        param.magFilter = Texture.TextureFilter.Linear;
        param.borderStraight = true;
    }
}
