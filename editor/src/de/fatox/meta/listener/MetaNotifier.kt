package de.fatox.meta.listener

abstract class MetaNotifier {
    private val clickListeners: HashSet<() -> Unit> = HashSet()

    fun addListener(clickListener: () -> Unit) {
        clickListeners.add(clickListener)
    }

    fun notifyListeners() {
        clickListeners.forEach({ listener -> listener.invoke() })
    }
}
