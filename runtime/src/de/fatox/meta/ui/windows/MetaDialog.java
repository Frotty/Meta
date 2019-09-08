package de.fatox.meta.ui.windows;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisImageButton;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import de.fatox.meta.ui.components.MetaClickListener;

/**
 * Created by Frotty on 04.06.2016.
 */
public abstract class MetaDialog extends MetaWindow {
    protected final VisTable buttonTable = new VisTable();
    protected final VisLabel statusLabel = new VisLabel();
    protected DialogListener dialogListener;
    private int buttonCount = 0;

    public interface DialogListener {
        void onResult(Object object);
    }

    public MetaDialog(String title, boolean hasCloseButton) {
        super(title, false, hasCloseButton);
        if (hasCloseButton) {
            Actor btn = getTitleTable().getCells().get(getTitleTable().getCells().size - 1).getActor();
            if (btn instanceof VisImageButton) {
                btn.addListener(event -> {
                    dialogListener.onResult(null);
                    return false;
                });
            }
        }
        getContentTable().top().padTop(4);
        statusLabel.setAlignment(Align.center);
        statusLabel.setWrap(true);

        add(statusLabel).growX();
        row();
        add(buttonTable).bottom().growX();
    }

    public <T extends Button> T addButton(Button button, int align, Object result) {
        button.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!button.isDisabled() && dialogListener != null) {
                    dialogListener.onResult(result);
                }
            }
        });
        if (buttonCount > 0) {
            buttonTable.add().growX();
        }
        buttonCount++;
        buttonTable.add(button).align(align);
        return (T) button;
    }

    public void show() {
        // Set color invisible for fade in to work
        centerWindow();
        setColor(1, 1, 1, 0);
        addAction(Actions.alpha(0.95f, 0.75f));
        Gdx.input.setCursorCatched(false);
    }

    public void setDialogListener(DialogListener dialogListener) {
        this.dialogListener = dialogListener;
    }
}
