package de.fatox.meta.ui

import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.util.adapter.ArrayAdapter
import com.kotcrab.vis.ui.util.adapter.SimpleListAdapter.SimpleListAdapterStyle
import com.kotcrab.vis.ui.widget.VisTable
import de.fatox.meta.ui.components.MetaTextButton

class FolderListAdapter<ItemT>(array: Array<ItemT>?) : ArrayAdapter<ItemT, VisTable>(array) {
    private val style: SimpleListAdapterStyle
    override fun createView(item: ItemT): VisTable {
        val table = VisTable()
        table.pad(1f)
        val visTextButton = MetaTextButton(item.toString(), 12)
        table.add(visTextButton).growX().prefWidth(128f).pad(1f)
        return table
    }

    override fun selectView(view: VisTable) {
        view.background = style.selection
    }

    override fun deselectView(view: VisTable) {
        view.background = style.background
    }

    init {
        style = VisUI.getSkin().get("default", SimpleListAdapterStyle::class.java)
    }
}