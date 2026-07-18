package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.extensions.onClick
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.reactive.subscribe
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaControlSize
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType
import de.fatox.meta.ui.tabs.MetaTab

/** Scene2d-native tab strip with Meta-owned selection state. */
class MetaTabbedPane {
	val layout = MetaFlexBox(mainGap = MetaSpacing.XXS, align = MetaFlexAlign.CENTER)
	@Deprecated("Use layout; tab strips now use MetaFlexBox.", ReplaceWith("layout"))
	val table: MetaFlexBox get() = layout
	val tabs = Array<MetaTab>()
	val activeMetaTabValue: Signal<MetaTab?> = signal(null)
	val activeMetaTab: MetaTab? get() = activeMetaTabValue.peek()
	private val buttons = Array<TabButton>()
	private val listeners = Array<MetaTabbedPaneAdapter>()

	fun add(tab: MetaTab) {
		if (tabs.contains(tab, true)) return
		tabs.add(tab)
		tab.tabPane = this
		val button = TabButton(tab.tabTitle).onClick { switchTab(tab) }
		buttons.add(button)
		layout.addItem(button, basisHeight = TAB_HEIGHT, minWidth = TAB_MIN_WIDTH, minHeight = TAB_HEIGHT)
		if (tab.isCloseableByUser) {
			layout.addItem(MetaImageButton("ri-close-line", 14).onClick { remove(tab) },
				basisWidth = CLOSE_SIZE, basisHeight = CLOSE_SIZE, shrink = 0f)
		}
		if (activeMetaTab == null) switchTab(tab)
	}

	fun remove(tab: MetaTab): Boolean {
		val index = tabs.indexOf(tab, true)
		if (index < 0) return false
		val wasActive = activeMetaTab === tab
		tabs.removeIndex(index)
		buttons.removeIndex(index)
		tab.tabPane = null
		tab.dispose()
		rebuild()
		if (wasActive) applySwitch(if (tabs.size == 0) null else tabs[(index.coerceAtMost(tabs.size - 1))])
		return true
	}

	fun switchTab(tab: MetaTab) {
		if (!tabs.contains(tab, true)) return
		applySwitch(tab)
	}

	fun addListener(listener: MetaTabbedPaneAdapter): Boolean {
		if (listeners.contains(listener, true)) return false
		listeners.add(listener)
		return true
	}

	fun removeListener(listener: MetaTabbedPaneAdapter): Boolean = listeners.removeValue(listener, true)

	private fun applySwitch(tab: MetaTab?) {
		if (activeMetaTab === tab) return
		activeMetaTabValue.value = tab
		for (i in 0 until buttons.size) buttons[i].active = tabs[i] === tab
		if (tab != null) for (i in 0 until listeners.size) listeners[i].switchedMetaTab(tab)
	}

	private fun rebuild() {
		layout.clearChildren()
		for (i in 0 until tabs.size) {
			layout.addItem(buttons[i], basisHeight = TAB_HEIGHT, minWidth = TAB_MIN_WIDTH, minHeight = TAB_HEIGHT)
			if (tabs[i].isCloseableByUser) {
				layout.addItem(MetaImageButton("ri-close-line", 14).onClick { remove(tabs[i]) },
					basisWidth = CLOSE_SIZE, basisHeight = CLOSE_SIZE, shrink = 0f)
			}
		}
	}

	private companion object {
		val TAB_HEIGHT = MetaControlSize.STANDARD.height
		val CLOSE_SIZE = MetaControlSize.COMPACT.iconTarget
		const val TAB_MIN_WIDTH = 48f
	}
}

open class MetaTabbedPaneAdapter {
	open fun switchedMetaTab(tab: MetaTab) = Unit
}

private class TabButton(text: String) : MetaTextButton(text, MetaType.BODY) {
	private val normal = Button.ButtonStyle(MetaSkin.skin().get(MetaSkin.BUTTON, Button.ButtonStyle::class.java))
	private val selected = MetaSkin.selectedButtonStyle(normal)
	val activeValue: Signal<Boolean> = signal(false)
	var active: Boolean
		get() = activeValue.peek()
		set(value) { activeValue.value = value }
	@Suppress("unused")
	private val activeBinding = activeValue.subscribe {
		installMetaStyle(if (activeValue.peek()) selected else normal)
	}

	init {
		pad(MetaSpacing.XS, MetaSpacing.MD, MetaSpacing.XS, MetaSpacing.MD)
		installMetaStyle(normal)
	}
}
