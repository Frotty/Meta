package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array

class MetaListView<ItemT>(private val adapter: MetaArrayAdapter<ItemT, out Actor>) {
	val mainTable = MetaTable()
	val scrollPane: ScrollPane = MetaScrollPane(mainTable)
	private var itemClickListener: ((ItemT) -> Unit)? = null
	private var selectedView: Actor? = null

	init {
		rebuildView()
	}

	fun rebuildView() {
		mainTable.clearChildren()
		mainTable.top().left()
		for (item in adapter.items) {
			val view = adapter.createView(item)
			view.addListener(object : ClickListener() {
				override fun clicked(event: InputEvent, x: Float, y: Float) {
					selectedView?.let { adapter.deselectView(it) }
					selectedView = view
					adapter.selectView(view)
					itemClickListener?.invoke(item)
				}
			})
			mainTable.add(view).growX().row()
		}
	}

	fun setItemClickListener(listener: (ItemT) -> Unit) {
		itemClickListener = listener
	}
}

abstract class MetaArrayAdapter<ItemT, ViewT : Actor>(array: Array<ItemT>?) {
	val items: Array<ItemT> = array ?: Array()

	abstract fun createView(item: ItemT): ViewT

	open fun selectView(view: Actor) = Unit

	open fun deselectView(view: Actor) = Unit

	fun indexOf(item: ItemT): Int = items.indexOf(item, false)
	fun size(): Int = items.size
	fun get(index: Int): ItemT = items[index]
	fun add(item: ItemT) = items.add(item)
	fun clear() = items.clear()
}
