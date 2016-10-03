package de.fatox.meta.ui.windows;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisWindow;
import de.fatox.meta.Meta;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.dao.MetaData;
import de.fatox.meta.api.dao.MetaWindowData;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;

/**
 * Created by Frotty on 08.05.2016.
 */
public abstract class MetaWindow extends VisWindow {
    private static final String TAG = "MetaWindow";
    @Inject
    @Log
    private Logger log;
    @Inject
    private UIManager uiManager;
    @Inject
    private MetaData metaData;

    public MetaWindow(String title) {
        this(title, false, false);
    }

    public MetaWindow(String title, boolean resizable, boolean closeButton) {
        super(title, resizable ? "resizable" : "default");
        Meta.inject(this);

        defaults().pad(2);
        if (closeButton) {
            addCloseButton();
        }
        // Separator
        getTitleTable().row().height(2);
        getTitleTable().add(new Separator()).growX().padTop(2).colspan(closeButton ? 2 : 1);
        getTitleTable().top();
        getTitleTable().pad(2);
        row();
        if (resizable) {
            padBottom(6);
            setResizable(true);
        }
    }

    public void setDefaultSize(float width, float height) {
        setDefault(getX(), getY(), getWidth(), getHeight());
    }

    public void setDefaultPos(float x, float y) {
        setDefault(x, y, getWidth(), getHeight());
    }

    public void setDefault(float x, float y, float width, float height) {
        if (metaData.hasWindowData(this.getClass())) {
            MetaWindowData windowData = metaData.getWindowData(this);
            windowData.set(this);
        } else {
            setPosition(x,y);
            setSize(width,height);
            metaData.getWindowData(this);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (isDragging()) {
            metaData.getWindowData(this).setFrom(this);
            metaData.write();
        }
    }

    @Override
    public void close() {
        super.close();
        log.debug(TAG, "on close");
        uiManager.closeWindow(this);
    }
}
