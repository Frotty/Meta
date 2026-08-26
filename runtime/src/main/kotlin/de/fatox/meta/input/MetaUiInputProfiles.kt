package de.fatox.meta.input

import com.badlogic.gdx.utils.IntMap
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

/**
 * One [MetaUiInputBindings] per local player.
 *
 * ### Player one's profile is the registered singleton, not a copy
 *
 * That is the whole of the backwards-compatibility story. A consumer that injects `MetaUiInputBindings` and mutates it
 * - which is how a game mirrors its own remappable profile onto Meta's actions - is configuring player one, exactly as
 * it was before this type existed. Handing back a copy would have left every such consumer configuring something
 * nothing reads.
 *
 * ### Later players start empty
 *
 * Deliberately, and it is the one thing here that would be wrong the other way round. `MetaUiInputBindings` defaults
 * to the arrows, ENTER and ESCAPE; copying those into player two would give two people one cursor either could move,
 * which is precisely the outcome per-player input exists to avoid. An unconfigured second player answers nothing, so
 * a game that allocates a profile and forgets to fill it gets a cursor that does not move rather than one that fights.
 *
 * Profiles are created on first request, so asking about a player is enough to allocate one.
 */
class MetaUiInputProfiles {
	private val primary: MetaUiInputBindings by lazyInject()
	private val secondary = IntMap<MetaUiInputBindings>()

	/** The profile driving [player], created empty on first request for anyone but [MetaPlayer.ONE]. */
	operator fun get(player: MetaPlayer): MetaUiInputBindings {
		if (player == MetaPlayer.ONE) return primary
		secondary.get(player.index)?.let { return it }
		return MetaUiInputBindings().also {
			it.clear()
			secondary.put(player.index, it)
		}
	}

	/**
	 * Whether [player] has a profile yet. Player one always does.
	 *
	 * Distinct from [get], which creates one - use this to ask without allocating.
	 */
	fun has(player: MetaPlayer): Boolean =
		player == MetaPlayer.ONE || secondary.containsKey(player.index)

	/** How many players have profiles. One until a game asks for more. */
	val playerCount: Int get() = 1 + secondary.size

	/** Drops every profile but player one's, which is the injected singleton and not this object's to discard. */
	fun clearSecondary() {
		secondary.clear()
	}
}
