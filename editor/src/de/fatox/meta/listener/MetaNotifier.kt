package de.fatox.meta.listener

import com.badlogic.gdx.utils.ObjectSet

abstract class MetaNotifier {
	private val clickListeners: ObjectSet<() -> Unit> = ObjectSet()

	fun addListener(clickListener: () -> Unit): () -> Unit = clickListener.also { clickListeners.add(it) }
	fun removeListener(clickListener: () -> Unit): Boolean = clickListeners.remove(clickListener)

	fun notifyListeners(): Unit = clickListeners.forEach { it() }
}
