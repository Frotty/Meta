package de.fatox.meta.graphics.font;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Align;
import de.fatox.meta.Meta;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.graphics.FontProvider;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;

public class MetaFontProvider implements FontProvider {

    private final BitmapFont bitmapFont;
    private FreeTypeFontGenerator.FreeTypeFontParameter param;
    @Inject
    @Log
    private Logger log;
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
        param.genMipMaps = true;
        param.kerning = true;
        param.incremental = true;
        param.minFilter = Texture.TextureFilter.MipMapLinearLinear;
        param.magFilter = Texture.TextureFilter.Linear;
        param.genMipMaps = true;
        param.size = 55;
        bitmapFont = generator.generateFont(param);
    }

    @Override
    public BitmapFont getFont(float size) {
        return bitmapFont;
    }

    @Override
    public void write(float x, float y, String text, float size) {
        bitmapFont.draw(spriteBatch, text, x, y, 0, Align.center, false);
    }
}
