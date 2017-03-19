package de.fatox.meta.ui.windows;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.kotcrab.vis.ui.widget.VisTextField;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.ui.components.MetaLabel;

/**
 * Created by Frotty on 20.05.2016.
 */
@Singleton
public class CameraWindow extends MetaWindow {
    private final VisTextField xPosField;
    private final VisTextField yPosField;
    private final VisTextField zPosField;

    private final VisTextField xUpField;
    private final VisTextField yUpField;
    private final VisTextField zUpField;

    @Inject
    public PerspectiveCamera camera;

    public CameraWindow() {
        super("Camera", true, true);
        xPosField = new VisTextField("0.0");
        yPosField = new VisTextField("0.0");
        zPosField = new VisTextField("0.0");

        xUpField = new VisTextField("0.0");
        yUpField = new VisTextField("0.0");
        zUpField = new VisTextField("0.0");

        contentTable.add(new MetaLabel("Position:", 14)).colspan(6).center().row();
        contentTable.add(new MetaLabel("x:", 12));
        contentTable.add(xPosField).width(64).pad(2);
        contentTable.add(new MetaLabel("y:", 12));
        contentTable.add(yPosField).width(64).pad(2);
        contentTable.add(new MetaLabel("z:", 12));
        contentTable.add(zPosField).width(64).pad(2);
        contentTable.row();

        contentTable.add(new MetaLabel("Up:", 14)).colspan(6).center().row();
        contentTable.add(new MetaLabel("x:", 12));
        contentTable.add(xUpField).width(64).pad(2);
        contentTable.add(new MetaLabel("y:", 12));
        contentTable.add(yUpField).width(64).pad(2);
        contentTable.add(new MetaLabel("z:", 12));
        contentTable.add(zUpField).width(64).pad(2);
    }


    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        xPosField.setText(camera.position.x + "");
        yPosField.setText(camera.position.y + "");
        zPosField.setText(camera.position.z + "");

        xUpField.setText(camera.up.x + "");
        yUpField.setText(camera.up.y + "");
        zUpField.setText(camera.up.z + "");
    }
}
