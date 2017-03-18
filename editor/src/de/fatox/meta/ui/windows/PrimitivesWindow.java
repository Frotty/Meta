package de.fatox.meta.ui.windows;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.ui.components.MetaClickListener;
import de.fatox.meta.ui.components.MetaIconTextButton;

/**
 * Created by Frotty on 20.05.2016.
 */
@Singleton
public class PrimitivesWindow extends MetaWindow {
    private final MetaIconTextButton boxButton;

    public PrimitivesWindow() {
        super("Primitives", true, true);
        this.boxButton = new MetaIconTextButton("Box", assetProvider.getDrawable("ui/appbar.box.png"));
        boxButton.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
            }
        });

        contentTable.add(boxButton).size(64);
    }


}
