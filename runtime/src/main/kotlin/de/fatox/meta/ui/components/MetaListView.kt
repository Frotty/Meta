package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import de.fatox.meta.reactive.Disposable
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.reactive.subscribe
import de.fatox.meta.ui.MetaSpacing

class MetaListView<ItemT>(private val adapter: MetaArrayAdapter<ItemT, out Actor>) {
	val mainTable = MetaFlexBox(
		direction = MetaFlexDirection.COLUMN,
		mainGap = MetaSpacing.XXS,
		align = MetaFlexAlign.STRETCH,
	)
	val scrollPane: ScrollPane = MetaScrollPane(mainTable)
	val selectedItemValue: Signal<ItemT?> = signal(null)
	private var itemClickListener: ((ItemT) -> Unit)? = null
	private val selectionBinding: Disposable = selectedItemValue.subscribe {
		applySelection(selectedItemValue.peek())
	}

	init { rebuildView() }

	fun rebuildView() {
		mainTable.clearChildren()
		val selectedItem = selectedItemValue.peek()
		for (item in adapter.items) {
			val view = adapter.createView(item)
			view.addListener(object : ClickListener() {
				override fun clicked(event: InputEvent, x: Float, y: Float) {
					if (event.targetsNestedButton(view)) return
					selectedItemValue.value = item
					itemClickListener?.invoke(item)
				}
			})
			mainTable.addItem(view, shrink = 0f)
		}
		applySelection(selectedItem)
	}

	fun selectItem(item: ItemT) {
		selectedItemValue.value = item
	}

	fun setItemClickListener(listener: (ItemT) -> Unit) {
		itemClickListener = listener
	}

	fun dispose() {
		selectionBinding.dispose()
	}

	private fun applySelection(selectedItem: ItemT?) {
		val children = mainTable.children
		for (i in 0 until children.size) {
			val view = children[i]
			if (i < adapter.items.size && adapter.items[i] == selectedItem) {
				adapter.selectView(view)
			} else {
				adapter.deselectView(view)
			}
		}
	}
}

/** A nested button owns its click; composed parent rows must not also activate or select themselves. */
internal fun InputEvent.targetsNestedButton(owner: Actor): Boolean {
	var current: Actor? = target
	while (current != null && current !== owner) {
		if (current is Button) return true
		current = current.parent
	}
	return false
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
