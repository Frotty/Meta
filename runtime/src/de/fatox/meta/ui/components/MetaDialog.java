package de.fatox.meta.ui.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import de.fatox.meta.util.GoldenRatio;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha;

/**
 * Created by Frotty on 04.06.2016.
 */
public abstract class MetaDialog extends MetaWindow {
    protected final VisTable buttonTable;
    protected final VisLabel statusLabel = new VisLabel();
    protected final VisTable contentTable;
    protected final MetaTextButton leftButton;
    protected final MetaTextButton rightButton;

    public MetaDialog(String title, String left, String right) {
        super(title, false, true);
        statusLabel.setAlignment(Align.center);
        buttonTable = new VisTable();
        leftButton = new MetaTextButton(left);
        leftButton.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!leftButton.isDisabled()) {
                    onResult(false);
                }
            }
        });
        buttonTable.add(leftButton).pad(4).left();
        buttonTable.add(statusLabel).growX();
        rightButton = new MetaTextButton(right);
        rightButton.setColor(0.9f, 0.8f, 1, 1);
        rightButton.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!rightButton.isDisabled()) {
                    onResult(true);
                }
            }
        });
        buttonTable.add(rightButton).pad(4).right();
        contentTable = new VisTable();
        contentTable.top().padTop(2);
        add(contentTable).top().grow();
        row();
        add(buttonTable).bottom().growX();
    }

    @Override
    public float getPrefWidth() {
        return GoldenRatio.C * Gdx.graphics.getWidth();
    }

    @Override
    public float getPrefHeight() {
        return GoldenRatio.B * Gdx.graphics.getHeight();
    }

    public void show(Stage stage) {
        pack();
        setColor(1, 1, 1, 0);
        setPosition(Math.round((stage.getWidth() - getWidth()) / 2), Math.round((stage.getHeight() - getHeight()) / 2));
        stage.addActor(this);
        addAction(alpha(0.85f, 0.5f));
    }

    public abstract void onResult(Object object);
}
