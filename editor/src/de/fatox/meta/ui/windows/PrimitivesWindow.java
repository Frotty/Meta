package de.fatox.meta.ui.windows;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.kotcrab.vis.ui.widget.VisTextButton;
import de.fatox.meta.ui.components.MetaClickListener;
import de.fatox.meta.ui.components.MetaWindow;

/**
 * Created by Frotty on 20.05.2016.
 */
public class PrimitivesWindow extends MetaWindow {
    private final VisTextButton boxButton;

    public PrimitivesWindow() {
        super("Primitives", true);

        this.boxButton = new VisTextButton("Box");
        boxButton.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {

            }
        });

        add(boxButton);
    }


}
