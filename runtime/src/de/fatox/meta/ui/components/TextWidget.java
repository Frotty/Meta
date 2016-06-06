package de.fatox.meta.ui.components;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.utils.Align;
import de.fatox.meta.Meta;
import de.fatox.meta.api.graphics.FontProvider;
import de.fatox.meta.injection.Inject;

/**
 * Created by Frotty on 05.06.2016.
 */
public class TextWidget extends Widget {
    @Inject
    private FontProvider fontProvider;
    private String text;


    public TextWidget(String text) {
        Meta.inject(this);
        this.text = text;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        fontProvider.getFont(80).draw(batch, text, getX(), getY()+getHeight()/2, getWidth(), Align.center, false);
    }
}


