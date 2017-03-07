package de.fatox.meta.ui.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisImage;
import com.kotcrab.vis.ui.widget.VisTextButton;
import de.fatox.meta.util.GoldenRatio;

/**
 * Created by Frotty on 04.06.2016.
 */
public class MetaIconTextButton extends Button {
    private final VisImage image;
    private final MetaLabel label;

    public MetaIconTextButton(String text, Drawable drawable, int maxWidth) {
        this(text, 12, drawable);
        label.setMaxWidth(maxWidth);
    }

    public MetaIconTextButton(String text, Drawable drawable) {
        this(text, 12, drawable);
    }

    public MetaIconTextButton(String text, int size, Drawable drawable) {
        super(VisUI.getSkin().get(VisTextButton.VisTextButtonStyle.class));
        pad(GoldenRatio.C * 10, GoldenRatio.A * 20, GoldenRatio.C * 10, GoldenRatio.A * 20);
        image = new VisImage(drawable);
        label = new MetaLabel(text, size, Color.WHITE);
        label.setAlignment(Align.center);
        add(image).center().grow();
        row();
        add(label).center().grow().pad(2);
    }

    public void setText(String text) {
        label.setText(text);
    }

    public CharSequence getText() {
        return label.getText();
    }


}
