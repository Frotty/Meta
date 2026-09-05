package de.fatox.meta.input

import com.badlogic.gdx.InputProcessor

/**
 * Optional policy for an exclusive input processor that captures controller input from only selected players.
 *
 * Physical keyboard, pointer and scroll input remain exclusive. An exclusive processor that does not implement this
 * interface keeps the traditional behaviour of accepting every controller too.
 */
interface MetaControllerInputScope {
	fun acceptsControllerInput(player: MetaPlayer): Boolean
}

/** Shared by the real input router and the shipped dispatching test fixture so their grab semantics cannot drift. */
internal fun InputProcessor.acceptsControllerDispatch(player: MetaPlayer?): Boolean {
	if (player == null) return true
	return (this as? MetaControllerInputScope)?.acceptsControllerInput(player) != false
}
