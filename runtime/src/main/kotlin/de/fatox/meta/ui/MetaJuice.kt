package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.utils.Align

/**
 * Decorative interaction juice shared by Meta controls. All helpers respect [MetaMotion.enabled] and enable the
 * group transform only for the duration of an animation, so idle widgets keep the sprite batch unbroken.
 */

/**
 * Adds a press-squish: the control shrinks slightly while held and springs back on release. [enabled] is consulted
 * per press so composed controls (e.g. spinner segments, whose independent squish would visually split the
 * composite) can opt out after construction.
 */
fun <T : WidgetGroup> T.installPressSquish(enabled: () -> Boolean = { true }): T {
	addListener(object : InputListener() {
		// Preallocated so releases don't allocate a capturing lambda per click.
		private val resetTransform = Runnable { setTransform(false) }
		private var releaseAction: Action? = null

		override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
			if (pointer != 0 || !MetaMotion.enabled || !enabled()) return false
			// A quick re-press must not let the previous release sequence disable the transform mid-squish.
			releaseAction?.let { removeAction(it) }
			releaseAction = null
			setOrigin(Align.center)
			setTransform(true)
			addAction(Actions.scaleTo(MetaMotion.PRESS_SCALE, MetaMotion.PRESS_SCALE, MetaMotion.PRESS))
			return true
		}

		override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
			if (pointer != 0) return
			val release = Actions.sequence(
				Actions.scaleTo(1f, 1f, MetaMotion.QUICK, MetaMotion.OVERSHOOT),
				Actions.run(resetTransform),
			)
			releaseAction = release
			addAction(release)
		}
	})
	return this
}
