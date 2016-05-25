package de.fatox.meta.ui.components;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisDialog;
import de.fatox.meta.Meta;

/**
 * Created by Frotty on 08.05.2016.
 */
public class MetaDialog extends VisDialog {

    public MetaDialog(String title) {
        super(title);
        Meta.inject(this);
        // Seperator
        getContentTable().row().height(2);
        getContentTable().add(new Separator()).width(getWidth()).minWidth(0).maxWidth(9999).padTop(2);
        getContentTable().top();
        getContentTable().row();
    }

    @Override
    public VisDialog text(String text, Label.LabelStyle labelStyle) {
        Label label = new Label(text, labelStyle);
        label.setAlignment(Align.center);
        return text(label);
    }
}
