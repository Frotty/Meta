package de.fatox.meta.ui.components;

import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

/**
 * Created by Frotty on 04.07.2016.
 */
public class AssetSelectButton {
    private VisTable visTable = new VisTable();
    private VisTextButton selectAssetButton;
    private VisTextField assetNameLabel;
    private String name;

    public AssetSelectButton(String name) {
        this.name = name;
        selectAssetButton = new VisTextButton("...");
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
