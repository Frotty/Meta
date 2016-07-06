package de.fatox.meta.ui.components;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import de.fatox.meta.Meta;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.windows.AssetDiscovererWindow;

/**
 * Created by Frotty on 04.07.2016.
 */
public class AssetSelectButton {
    private VisTable visTable = new VisTable();
    private VisTextButton selectAssetButton;
    private VisTextField assetNameLabel;
    private String name;

    @Inject
    private AssetDiscovererWindow assetDiscovererWindow;

    public AssetSelectButton(String name) {
        Meta.inject(this);
        this.name = name;
        selectAssetButton = new VisTextButton("...");
        selectAssetButton.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                assetDiscovererWindow.enableSelectionMode();
            }
        });
        assetNameLabel = new VisTextField("Select " + name);
        assetNameLabel.setDisabled(true);
        assetNameLabel.setFocusBorderEnabled(false);
        visTable.add(assetNameLabel).growX();
        visTable.add(selectAssetButton).padLeft(2);
    }

    public VisTable getTable() {
        return visTable;
    }
}
