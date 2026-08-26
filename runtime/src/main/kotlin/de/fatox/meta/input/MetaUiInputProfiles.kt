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
 *
 * ### Two players given the same key both act
 *
 * Stated because the registry cannot prevent it. [get] hands back the live, mutable [MetaUiInputBindings], which is
 * what makes a rebinding screen possible, and it means a later `setKeyboardKeys` can assign a key already bound in
 * another profile without this object ever seeing it. Both cursors then resolve the same physical press and both move.
 *
 * Empty defaults stop that at allocation time and nothing stops it afterwards, so a game that lets players rebind
 * should call [conflictingKeys] when a profile is committed and refuse or reassign. There is no arbitration here on
 * purpose: which player keeps a contested key is a game's decision, not a framework's, and silently dropping one
 * player's binding would be the worst of the available behaviours.
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

	/**
	 * Keycodes bound to a UI action in both [a] and [b], which is a misconfiguration this class will not stop.
	 *
	 * Empty means the two players' key sets are disjoint and their cursors are independent. Anything else names the
	 * keys that will move both.
	 */
	fun conflictingKeys(a: MetaPlayer, b: MetaPlayer): IntArray {
		if (a == b) return IntArray(0)
		val first = this[a]
		val second = this[b]
		// Sized to exactly how many keys `a` binds: a guessed upper bound would overflow the moment a profile bound
		// more keys to an action than the guess allowed.
		var capacity = 0
		for (actionIndex in MetaUiAction.entries.indices) {
			capacity += first.keyboardKeysFor(MetaUiAction.entries[actionIndex]).size
		}
		val shared = IntArray(capacity)
		var count = 0
		for (actionIndex in MetaUiAction.entries.indices) {
			val keys = first.keyboardKeysFor(MetaUiAction.entries[actionIndex])
			for (keyIndex in keys.indices) {
				val key = keys[keyIndex]
				if (second.actionForKey(key) == null) continue
				if (!contains(shared, count, key)) shared[count++] = key
			}
		}
		return shared.copyOf(count)
	}

	private fun contains(values: IntArray, size: Int, value: Int): Boolean {
		for (index in 0 until size) if (values[index] == value) return true
		return false
	}

	/** Drops every profile but player one's, which is the injected singleton and not this object's to discard. */
	fun clearSecondary() {
		secondary.clear()
	}
}
