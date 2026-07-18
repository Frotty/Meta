package de.fatox.meta.ui

import de.fatox.meta.input.MetaUiAction

/**
 * Optional semantic action hook for controls whose directional behavior is richer than moving focus to a neighbour.
 * Returning true keeps the current focus target and suppresses spatial navigation for that action.
 */
interface MetaUiActionHandler {
	fun handleMetaUiAction(action: MetaUiAction): Boolean
}
