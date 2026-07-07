package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal

class MetaListView<ItemT>(private val adapter: MetaArrayAdapter<ItemT, out Actor>) {
	val mainTable = MetaTable()
	val scrollPane: ScrollPane = MetaScrollPane(mainTable)
	val selectedItemValue: Signal<ItemT?> = signal(null)
	private var itemClickListener: ((ItemT) -> Unit)? = null
	private var selectedView: Actor? = null

	init {
		rebuildView()
	}

	fun rebuildView() {
		mainTable.clearChildren()
		mainTable.top().left()
		selectedView = null
		val selectedItem = selectedItemValue.peek()
		for (item in adapter.items) {
			val view = adapter.createView(item)
			view.addListener(object : ClickListener() {
				override fun clicked(event: InputEvent, x: Float, y: Float) {
					selectItem(item, view)
					itemClickListener?.invoke(item)
				}
			})
			if (selectedItem == item) {
				selectedView = view
				adapter.selectView(view)
			}
			mainTable.add(view).growX().row()
		}
	}

	fun selectItem(item: ItemT) {
		val children = mainTable.children
		for (i in 0 until adapter.items.size) {
			if (adapter.items[i] == item && i < children.size) {
				selectItem(item, children[i])
				return
			}
		}
	}

	fun setItemClickListener(listener: (ItemT) -> Unit) {
		itemClickListener = listener
	}

	private fun selectItem(item: ItemT, view: Actor) {
		selectedView?.let { adapter.deselectView(it) }
		selectedView = view
		adapter.selectView(view)
		selectedItemValue.value = item
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
