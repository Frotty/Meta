package de.fatox.meta.ui.windows;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import de.fatox.meta.api.dao.MetaWindowData;
import de.fatox.meta.ui.components.MetaClickListener;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha;

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
        contentTable.top().padTop(4);
        statusLabel.setAlignment(Align.center);
        statusLabel.setWrap(true);

        add(statusLabel).growX();
        row();
        add(buttonTable).bottom().growX();

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
        if (buttonCount > 0) {
            buttonTable.add().growX();
        }
        buttonCount++;
        buttonTable.add(button).align(align);
        return button;
    }

    public void show(boolean firstSetup) {
        // Set color invisible for fade in to work
        setColor(1, 1, 1, 0);
        // Center to middle of target stage
        if (firstSetup) {
            setPosition(Math.round((getStage().getWidth() - getWidth()) / 2), Math.round((getStage().getHeight() - getHeight()) / 2));
            MetaWindowData windowData = metaData.getWindowData(this);
            windowData.setFrom(this);
            metaData.write();
        }
        addAction(alpha(0.925f, 0.5f));
    }

    public void setDialogListener(DialogListener dialogListener) {
        this.dialogListener = dialogListener;
    }
}
