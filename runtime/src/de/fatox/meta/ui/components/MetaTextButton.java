package de.fatox.meta.ui.components;

import com.kotcrab.vis.ui.widget.VisTextButton;
import de.fatox.meta.util.GoldenRatio;

/**
 * Created by Frotty on 04.06.2016.
 */
public class MetaTextButton extends VisTextButton {

    public MetaTextButton(String text) {
        super(text);
        pad(GoldenRatio.C * 10, GoldenRatio.A * 20, GoldenRatio.C * 10, GoldenRatio.A * 20);
    }
}
