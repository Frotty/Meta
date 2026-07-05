package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import de.fatox.meta.reactive.Disposable
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.reactive.effect
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing

/**
 * A bottom-attached, content-width bar with rounded top corners. It intentionally knows nothing about prompt glyphs:
 * consumers provide any scene2d actor as content and can bind visibility/content through Meta's reactive scope.
 */
class MetaBottomBar @JvmOverloads constructor(
	content: Actor? = null,
	private val contentAlign: Int = Align.center,
) : Table(MetaSkin.skin()) {

	init {
		background = MetaSkin.skin().getDrawable(MetaSkin.BOTTOM_BAR)
		touchable = Touchable.disabled
		pad(MetaSpacing.SM, MetaSpacing.LG, MetaSpacing.SM, MetaSpacing.LG)
		defaults().center()
		setContent(content)
	}

	fun setContent(content: Actor?) {
		clearChildren()
		if (content != null) {
			add(content).align(contentAlign)
		}
		invalidateHierarchy()
	}

	fun bindShown(scope: ReactiveScope, shown: () -> Boolean): Disposable =
		scope.register(effect("MetaBottomBar.shown") { isVisible = shown() })

	fun bindContent(scope: ReactiveScope, content: () -> Actor?): Disposable =
		scope.register(effect("MetaBottomBar.content") { setContent(content()) })

	fun bottomOverlay(bottomPad: Float = MetaSpacing.NONE, horizontalAlign: Int = Align.center): Table =
		Table(MetaSkin.skin()).apply {
			touchable = Touchable.disabled
			setFillParent(true)
			align(Align.bottom or horizontalAlign)
			add(this@MetaBottomBar).padBottom(bottomPad)
		}
}
