package de.fatox.meta.api.extensions

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.Tooltip
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.showDialog
import de.fatox.meta.api.ui.showWindow
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.components.MetaClickListener
import de.fatox.meta.ui.windows.MetaDialog
import de.fatox.meta.ui.windows.MetaWindow

val uiManager: UIManager by lazyInject()

inline fun <reified T : Actor> T.onClick(
	button: Int = Input.Buttons.LEFT,
	crossinline action: MetaClickListener.(event: InputEvent) -> Unit,
): T {
	addListener(object : MetaClickListener(button) {
		override fun clicked(event: InputEvent, x: Float, y: Float) {
			action(event)
		}
	})
	return this
}

inline fun <reified T : Actor, reified W: MetaWindow> T.onClickShowWindow(
	button: Int = Input.Buttons.LEFT,
	crossinline config: W.() -> Unit = {},
): T {
	if (this is Disableable) uiManager.preventShowWindowObservers.add { isDisabled = it }
	addListener(object : MetaClickListener(button) {
		override fun clicked(event: InputEvent, x: Float, y: Float) {
			uiManager.showWindow(config)
		}
	})
	return this
}

inline fun <reified T : Actor, reified D: MetaDialog> T.onClickShowDialog(
	button: Int = Input.Buttons.LEFT,
	crossinline config: D.() -> Unit = {},
): T {
	if (this is Disableable) uiManager.preventShowWindowObservers.add { isDisabled = it }
	addListener(object : MetaClickListener(button) {
		override fun clicked(event: InputEvent, x: Float, y: Float) {
			uiManager.showDialog(config)
		}
	})
	return this
}

inline fun <reified T : Actor> T.onChange(crossinline action: ChangeListener.(event: ChangeEvent) -> Unit): T {
	addListener(object : ChangeListener() {
		override fun changed(event: ChangeEvent, actor: Actor) {
			action(event)
		}
	})
	return this
}

inline fun <reified T : Actor> T.tooltip(text: String, align: Int = Align.center) {
	Tooltip().apply {
		setText(text)
		align(align)
		target = this@tooltip
	}
}
