package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.Actor

/** Shared focus handoff semantics for keyboard/controller navigation. */
object MetaFocus {
	fun assign(current: Actor?, next: Actor?): Actor? {
		if (current === next) return current
		(current as? MetaFocusable)?.takeIf { it.handlesMetaFocus }?.setMetaFocused(false)
		(next as? MetaFocusable)?.takeIf { it.handlesMetaFocus }?.setMetaFocused(true)
		return next
	}

	fun isHandledByActor(actor: Actor?): Boolean =
		actor is MetaFocusable && actor.handlesMetaFocus
}
