package de.fatox.meta.ui.windows;

import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisWindow;
import de.fatox.meta.Meta;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha;

/**
 * Created by Frotty on 08.05.2016.
 */
public class MetaWindow extends VisWindow {
    public MetaWindow(String title, boolean resizable, boolean closeButton) {
        super(title, resizable ? "resizable" : "default");
        Meta.inject(this);
        if (closeButton) {
            addCloseButton();
        }
        // Seperator
        getTitleTable().row().height(2);
        getTitleTable().add(new Separator()).growX().padTop(2).colspan(closeButton ? 2 : 1);
        getTitleTable().top();
        getTitleTable().pad(2);
        row().height(4);
        add();
        row();
        setColor(1, 1, 1, 0);
        addAction(alpha(0.9025f, 0.5f));
        if (resizable) {
            padBottom(6);
            setResizable(true);
        }
    }
}
