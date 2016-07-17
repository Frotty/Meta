package de.fatox.meta.ui.components;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisWindow;
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
    private FileHandle selectedAsset;
    private AssetDiscovererWindow.SelectListener selectListener;

    @Inject
    private AssetDiscovererWindow assetDiscovererWindow;

    public AssetSelectButton(FileHandle selectedAsset) {
        this.name = selectedAsset.name();
        setup();
        assetNameLabel.setText(name);
    }

    public AssetSelectButton(String name) {
        Meta.inject(this);
        this.name = name;
        setup();
        assetNameLabel.setText("Select " + name);
    }

    private void setup() {
        selectAssetButton = new VisTextButton("...");
        selectAssetButton.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                assetDiscovererWindow.enableSelectionMode((FileHandle selected) -> {
                    AssetSelectButton.this.selectedAsset = selected;
                    if (selectListener != null) {
                        selectListener.onSelect(selected);
                    }
                    assetNameLabel.setText(name + ": " + selectedAsset.name());
                    // Bring window to Front
                    Group table = AssetSelectButton.this.getTable();
                    while (table != null && !(table instanceof Window)) {
                        table = table.getParent();
                    }
                    if (table != null) {
                        VisWindow visWindow = ((VisWindow) table);
                        visWindow.toFront();
                    }

                });
            }
        });
        assetNameLabel = new VisTextField();
        assetNameLabel.setDisabled(true);
        assetNameLabel.setFocusBorderEnabled(false);
        visTable.add(assetNameLabel).growX();
        visTable.add(selectAssetButton).padLeft(2);
    }

    public VisTable getTable() {
        return visTable;
    }

    public boolean hasFile() {
        return selectedAsset != null && selectedAsset.exists();
    }

    public void setSelectListener(AssetDiscovererWindow.SelectListener selectListener) {
        this.selectListener = selectListener;
    }

    public FileHandle getFile() {
        return selectedAsset;
    }
}
