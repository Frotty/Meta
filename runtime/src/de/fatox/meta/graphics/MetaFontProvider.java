package de.fatox.meta.graphics;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.fatox.meta.Meta;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.graphics.FontProvider;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;

public class MetaFontProvider implements FontProvider{

    private final BitmapFont staticFont;
    @Inject
    @Log
    private Logger log;

    @Inject
    private AssetProvider assetProvider;
    @Inject
    private SpriteBatch spriteBatch;

    @Inject
    public MetaFontProvider() {
        Meta.inject(this);
        log.debug("NetaFontProvider", "init");
        this.staticFont = getFont(14);
        log.debug("NetaFontProvider", "inited");
    }

    @Override
    public BitmapFont getFont(float size) {
        BitmapFont bitmapFont = assetProvider.get("fonts/meta.fnt", BitmapFont.class);
        return bitmapFont;
    }

    @Override
    public void write(float x, float y, String text, float size) {
        staticFont.draw(spriteBatch, text, x, y);
    }
}
