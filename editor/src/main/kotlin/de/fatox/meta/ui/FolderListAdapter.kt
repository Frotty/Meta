package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Array
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.components.MetaArrayAdapter
import de.fatox.meta.ui.components.MetaTable
import de.fatox.meta.ui.components.MetaTextButton

class FolderListAdapter<ItemT>(array: Array<ItemT>?) : MetaArrayAdapter<ItemT, MetaTable>(array) {
    private val background: Drawable = MetaSkin.skin().getDrawable("meta.panel")
    private val selection: Drawable = MetaSkin.skin().getDrawable("meta.selection")

    override fun createView(item: ItemT): MetaTable {
        val table = MetaTable()
        table.pad(1f)
        val visTextButton = MetaTextButton(item.toString(), 12)
        table.add(visTextButton).growX().prefWidth(128f).pad(1f)
        return table
    }

    override fun selectView(view: Actor) {
        (view as? MetaTable)?.background = selection
    }

    override fun deselectView(view: Actor) {
        (view as? MetaTable)?.background = background
    }

}
