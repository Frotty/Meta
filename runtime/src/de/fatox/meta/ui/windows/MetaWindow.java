package de.fatox.meta.ui.windows;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisImageButton;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisWindow;
import de.fatox.meta.Meta;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.dao.MetaData;
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
    protected UIManager uiManager;
    @Inject
    protected AssetProvider assetProvider;
    @Inject
    protected MetaData metaData;

    protected Table contentTable = new VisTable();

    public MetaWindow(String title) {
        this(title, false, false);
    }

    public MetaWindow(String title, boolean resizable, boolean closeButton) {
        super(title, resizable ? "resizable" : "default");
        Meta.inject(this);
        getTitleTable().left().padLeft(2);
        getTitleLabel().setAlignment(Align.left);
        if (closeButton) {
            addExitButton();
        }
        // Separator
        getTitleTable().top();
        getTitleTable().row().height(2);
        getTitleTable().add(new Separator()).growX().padTop(2).colspan(closeButton ? 2 : 1);
        getTitleTable().padTop(2);
        if (resizable) {
            padBottom(6);
            setResizable(true);
        }
        contentTable.top().pad(5, 1, 1, 1);
        add(contentTable).top().grow();
        row();
    }

    private void addExitButton() {
        Label titleLabel = getTitleLabel();
        Table titleTable = getTitleTable();

        VisImageButton closeButton = new VisImageButton("close-window");
        closeButton.setColor(1,1,1,0.2f);
        titleTable.add(closeButton).padRight(-getPadRight() + 0.7f);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                close();
            }
        });
        closeButton.addListener(new ClickListener() {
            @Override
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                event.cancel();
                return true;
            }
        });

        if (titleLabel.getLabelAlign() == Align.center && titleTable.getChildren().size == 2)
            titleTable.getCell(titleLabel).padLeft(closeButton.getWidth() * 2);
    }

    public void setDefaultSize(float width, float height) {
        setDefault(getX(), getY(), width, height);
    }

    public void setDefaultPos(float x, float y) {
        setDefault(x, y, getWidth(), getHeight());
    }

    public void setDefault(float x, float y, float width, float height) {
        setPosition(x, y);
        setSize(width, height);
    }

    boolean startDrag = false;

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (isDragging() ) {
            startDrag = true;
        } else if(startDrag) {
            startDrag = false;
            uiManager.updateWindow(this);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        setColor(1,1,1,0);
        addAction(Actions.alpha(0.9f, 0.75f));
    }

    @Override
    public void close() {
        super.close();
        log.debug(TAG, "on close");
        uiManager.closeWindow(this);
    }
}
