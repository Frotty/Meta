package de.fatox.meta.input

/**
 * Encodes controller actions for non-primary players onto Meta's existing input-processor path.
 *
 * Player one deliberately keeps the ordinary arrow/enter/escape keycodes: existing screen key listeners and the
 * registered singleton cursor must behave exactly as they did before controller assignment existed. Later players
 * use a private range that cannot collide with libGDX keycodes, so every global processor can receive the event while
 * only the [de.fatox.meta.ui.UiControlHelper] for the encoded player acts on it.
 */
internal object MetaUiControllerKeys {
	private const val BASE = 0x40000000
	private val actionCount = MetaUiAction.entries.size
	private val maxPlayerIndex = (Int.MAX_VALUE - BASE) / actionCount

	fun keyFor(player: MetaPlayer, action: MetaUiAction, bindings: MetaUiInputBindings): Int {
		if (player == MetaPlayer.ONE) return bindings.canonicalKeyFor(action)
		require(player.index <= maxPlayerIndex) { "Player index ${player.index} is too large for controller routing" }
		return BASE + player.index * actionCount + action.ordinal
	}

	fun actionFor(player: MetaPlayer, keycode: Int): MetaUiAction? {
		if (player == MetaPlayer.ONE || keycode < BASE) return null
		val encoded = keycode - BASE
		if (encoded / actionCount != player.index) return null
		return MetaUiAction.entries.getOrNull(encoded % actionCount)
	}
}
