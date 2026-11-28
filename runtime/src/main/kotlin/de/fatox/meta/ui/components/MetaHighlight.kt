package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import de.fatox.meta.ui.MetaSkin

/** Wraps a component with an optional outline that contributes zero size and passes input through. */
class MetaHighlight @JvmOverloads constructor(
	content: Actor,
	outline: Drawable = MetaSkin.skin().getDrawable(MetaSkin.SEARCH_HIGHLIGHT),
) : MetaStack() {
	private val overlay = Image(outline).apply {
		touchable = Touchable.disabled
		isVisible = false
	}
	private var flashAction: Action? = null

	var highlighted: Boolean
		get() = overlay.isVisible
		set(value) {
			cancelFlash()
			overlay.isVisible = value
		}

	init {
		touchable = Touchable.childrenOnly
		addItem(content)
		addItem(overlay, basisWidth = 0f, basisHeight = 0f)
	}

	/** Reveals this mounted component inside any enclosing Meta scroll panes and briefly outlines it. */
	fun reveal(): Boolean {
		if (!scrollIntoView()) return false
		if (highlighted && flashAction == null) return true
		cancelFlash()
		overlay.isVisible = true
		flashAction = Actions.delay(2.5f, Actions.run {
			flashAction = null
			overlay.isVisible = false
		}).also { addAction(it) }
		return true
	}

	override fun setStage(stage: Stage?) {
		super.setStage(stage)
		if (stage == null && flashAction != null) {
			cancelFlash()
			overlay.isVisible = false
		}
	}

	private fun cancelFlash() {
		val action = flashAction ?: return
		flashAction = null
		removeAction(action)
	}
}
