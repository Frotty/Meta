package de.fatox.meta.listener

abstract class MetaNotifier {
    private val clickListeners: HashSet<MetaListener> = HashSet()

    fun addListener(clickListener: MetaListener) {
        clickListeners.add(clickListener)
    }

    fun notifyListeners() {
        clickListeners.forEach({ listener -> listener.onEvent() })
    }
}
