package de.fatox.meta.api

import com.badlogic.gdx.ScreenAdapter
import de.fatox.meta.reactive.ReactiveScope

/**
 * A [ScreenAdapter] with a reactive scope matching each visible presentation of the screen.
 *
 * Create effects and bindings from [onShown]. They are disposed automatically from [hide], and a fresh scope is
 * supplied if the same screen instance is shown again. This keeps screen-owned reactive wiring in one lifecycle
 * instead of requiring every consumer to maintain a list of subscriptions.
 */
abstract class ReactiveScreenAdapter : ScreenAdapter() {
	protected var reactiveScope: ReactiveScope = ReactiveScope()
		private set

	private var shown = false
	private var disposed = false

	final override fun show() {
		check(!disposed) { "A disposed screen cannot be shown again" }
		if (shown) return
		if (reactiveScope.isDisposed) reactiveScope = ReactiveScope()
		shown = true
		onShown()
	}

	final override fun hide() {
		if (!shown) return
		try {
			onHidden()
		} finally {
			shown = false
			reactiveScope.dispose()
		}
	}

	final override fun dispose() {
		if (disposed) return
		try {
			if (shown) onHidden()
		} finally {
			shown = false
			reactiveScope.dispose()
			disposed = true
			onDisposed()
		}
	}

	protected open fun onShown(): Unit = Unit
	protected open fun onHidden(): Unit = Unit
	protected open fun onDisposed(): Unit = Unit
}
