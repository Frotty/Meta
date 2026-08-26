package de.fatox.meta.input

/**
 * A local player slot, for UI that more than one person drives at once.
 *
 * Local and nothing else: not a network identity, not a save profile, not a controller index. It exists so two people
 * at one machine - two key sets on one keyboard, or a pad each - can hold a cursor each, which the UI layer otherwise
 * has no way to express.
 *
 * [ONE] is what everything gets without asking. A single-player game never names a player and never notices this type.
 */
@JvmInline
value class MetaPlayer(val index: Int) {
	init {
		require(index >= 0) { "A player index cannot be negative, was $index" }
	}

	override fun toString(): String = "MetaPlayer($index)"

	companion object {
		/**
		 * The primary player, and the default everywhere.
		 *
		 * Privileged in two respects, both deliberate and both documented where they bite:
		 * `UiControlHelper` synthesizes canonical keys only for this player, and only this player's cursor drives
		 * [de.fatox.meta.api.ui.UIRenderer.setFocusedActor].
		 */
		val ONE = MetaPlayer(0)
	}
}
