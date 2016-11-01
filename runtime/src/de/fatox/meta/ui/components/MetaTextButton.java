package de.fatox.meta.ui.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisTextButton;
import de.fatox.meta.util.GoldenRatio;

/**
 * Created by Frotty on 04.06.2016.
 */
public class MetaTextButton extends Button {
    private final MetaLabel label;

    public MetaTextButton(String text, int size) {
        super(VisUI.getSkin().get(VisTextButton.VisTextButtonStyle.class));
        pad(GoldenRatio.C * 10, GoldenRatio.A * 20, GoldenRatio.C * 10, GoldenRatio.A * 20);
        label = new MetaLabel(text, size, Color.WHITE);
        label.setAlignment(Align.center);
        add(label).center().grow();
    }

    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
    }

    public void setText(String text) {
        label.setText(text);
    }

    public CharSequence getText() {
        return label.getText();
    }


}
