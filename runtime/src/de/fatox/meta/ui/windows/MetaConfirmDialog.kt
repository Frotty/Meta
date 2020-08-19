package de.fatox.meta.ui.windows;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.kotcrab.vis.ui.widget.VisLabel;
import de.fatox.meta.ui.components.MetaClickListener;
import de.fatox.meta.ui.components.MetaTextButton;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha;

/**
 * Created by Frotty on 14.06.2016.
 */
public class MetaConfirmDialog extends MetaWindow {
    public MetaConfirmDialog(String title, String message) {
        super(title, false, true);
        defaults().pad(4);
        add(new VisLabel(message)).growX();
        row();
        MetaTextButton close = new MetaTextButton("Close", 14);
        close.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                close();
            }
        });
        add(close);
    }

    public void show(Stage stage) {
        pack();
        setColor(1, 1, 1, 0);
        setPosition(Math.round((stage.getWidth() - getWidth()) / 2), Math.round((stage.getHeight() - getHeight()) / 2));
        stage.addActor(this);
        addAction(alpha(0.925f, 0.5f));
    }
}
