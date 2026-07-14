package de.fatox.meta.ui.tabs

import com.badlogic.gdx.scenes.scene2d.ui.Table
import de.fatox.meta.ui.components.MetaTabbedPane

abstract class MetaTab(
	val isSavable: Boolean = false,
	val isCloseableByUser: Boolean = true,
) {
	internal var tabPane: MetaTabbedPane? = null
	abstract val tabTitle: String
	abstract val contentTable: Table
	open fun onShow() = Unit
	open fun onHide() = Unit

	/** Called once when this tab is removed from its [MetaTabbedPane], so it can release owned resources/subscriptions. */
	open fun dispose() = Unit

	fun removeFromTabPane(): Boolean = tabPane?.remove(this) ?: false
}
