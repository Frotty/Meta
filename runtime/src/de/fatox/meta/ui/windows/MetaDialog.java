package de.fatox.meta.ui.windows;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import de.fatox.meta.ui.components.MetaClickListener;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha;

/**
 * Created by Frotty on 04.06.2016.
 */
public abstract class MetaDialog extends MetaWindow {
    protected final VisTable contentTable = new VisTable();
    protected final VisTable buttonTable = new VisTable();
    protected final VisLabel statusLabel = new VisLabel();
    protected DialogListener dialogListener;
    private int buttonCount = 0;

    public interface DialogListener {
        void onResult(Object object);
    }

    public MetaDialog(String title) {
        super(title, false, false);
        contentTable.top().padTop(2);
        statusLabel.setAlignment(Align.center);
        statusLabel.setWrap(true);

        add(contentTable).top().grow();
        row();
        add(statusLabel).growX();
        row();
        add(buttonTable).bottom().growX();
        pack();
    }


    public VisTextButton addButton(VisTextButton button, int align, Object result) {
        button.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (dialogListener != null) {
                    dialogListener.onResult(result);
                }
            }
        });
        if(buttonCount > 0) {
            buttonTable.add().growX();
        }
        buttonCount++;
        buttonTable.add(button).align(align);
        return button;
    }

    public void show() {
        pack();
        // Set color invisible for fade in to work
        setColor(1, 1, 1, 0);
        // Center to middle of target stage
        setPosition(Math.round((getStage().getWidth() - getWidth()) / 2), Math.round((getStage().getHeight() - getHeight()) / 2));
        addAction(alpha(0.925f, 0.5f));
    }

    public void setDialogListener(DialogListener dialogListener) {
        this.dialogListener = dialogListener;
    }
}
