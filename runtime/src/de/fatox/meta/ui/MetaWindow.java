package de.fatox.meta.ui;

import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisWindow;
import de.fatox.meta.Meta;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha;

/**
 * Created by Frotty on 08.05.2016.
 */
public class MetaWindow extends VisWindow {
    public MetaWindow(String title, boolean resizable) {
        super(title, resizable ? "resizable" : "default");
        Meta.inject(this);
        // Seperator
        getTitleTable().row().height(2);
        getTitleTable().add(new Separator()).width(9999).minWidth(0);
        getTitleTable().top();
        setColor(1, 1, 1, 0);
        addAction(alpha(0.85f, 0.5f));
        if(resizable) {
            padBottom(6);
        }
    }
}
