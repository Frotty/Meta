package de.fatox.meta.ui;

import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.adapter.ArrayAdapter;
import com.kotcrab.vis.ui.util.adapter.SimpleListAdapter;
import com.kotcrab.vis.ui.widget.VisTable;
import de.fatox.meta.ui.components.MetaTextButton;

public class FolderListAdapter<ItemT> extends ArrayAdapter<ItemT, VisTable> {
	private final SimpleListAdapter.SimpleListAdapterStyle style;

	public FolderListAdapter(Array<ItemT> array) {
		super(array);
		style = VisUI.getSkin().get("default", SimpleListAdapter.SimpleListAdapterStyle.class);
	}


	@Override
	protected VisTable createView(ItemT item) {
		VisTable table = new VisTable();
		table.pad(1);
		MetaTextButton visTextButton = new MetaTextButton(item.toString(), 12);
		table.add(visTextButton).growX().prefWidth(128).pad(1);
		return table;
	}

	@Override
	protected void selectView(VisTable view) {
		view.setBackground(style.selection);
	}

	@Override
	protected void deselectView(VisTable view) {
		view.setBackground(style.background);
	}
}