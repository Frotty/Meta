package de.fatox.meta.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.adapter.ArrayAdapter;
import com.kotcrab.vis.ui.util.adapter.SimpleListAdapter;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import de.fatox.meta.Meta;
import de.fatox.meta.ide.AssetManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.components.MetaClickListener;
import de.fatox.meta.ui.components.MetaTextButton;

public class FolderListAdapter<ItemT> extends ArrayAdapter<ItemT, VisTable> {
	private final SimpleListAdapter.SimpleListAdapterStyle style;

	@Inject
	private AssetManager assetManager;

	public FolderListAdapter(Array<ItemT> array) {
		super(array);
		Meta.inject(this);
		style = VisUI.getSkin().get("default", SimpleListAdapter.SimpleListAdapterStyle.class);
	}


	@Override
	protected VisTable createView (ItemT item) {
		VisTable table = new VisTable();
		table.left();
        VisTextButton visTextButton = new MetaTextButton(item.toString());
		visTextButton.addListener(new MetaClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				assetManager.openFolder(String.valueOf(visTextButton.getText()));
			}
		});
        table.add(visTextButton).growX();
		return table;
	}

	@Override
	protected void selectView (VisTable view) {
		view.setBackground(style.selection);
	}

	@Override
	protected void deselectView (VisTable view) {
		view.setBackground(style.background);
	}
}