package de.fatox.meta.graphics.font;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import de.fatox.meta.Meta;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.graphics.FontProvider;
import de.fatox.meta.injection.Inject;

/**
 * Created by Frotty on 23.10.2016.
 */
public class DistanceFieldFontProvider implements FontProvider {
    private BitmapFont distanceFieldFont;


    @Inject
    private AssetProvider assetProvider;

    public DistanceFieldFontProvider() {
        Meta.inject(this);
    }

    @Override
    public BitmapFont getFont(int size) {
        return null;
    }

    @Override
    public void write(float x, float y, String text, int size) {

    }
}
