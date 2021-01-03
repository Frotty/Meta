package de.fatox.meta.ui.windows

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.kotcrab.vis.ui.widget.VisLabel
import de.fatox.meta.api.extensions.onClick
import de.fatox.meta.ui.components.MetaTextButton
import kotlin.math.roundToInt

/**
 * Created by Frotty on 14.06.2016.
 */
class MetaConfirmDialog(title: String = "", message: String?) : MetaWindow(title, false, true) {
	fun show(stage: Stage) {
		pack()
		setColor(1f, 1f, 1f, 0f)
		setPosition(
			((stage.width - width) / 2).roundToInt().toFloat(),
			((stage.height - height) / 2).roundToInt().toFloat()
		)
		stage.addActor(this)
		addAction(Actions.alpha(0.925f, 0.5f))
	}

	init {
		defaults().pad(4f)
		add(VisLabel(message)).growX()
		row()
		add(MetaTextButton("Close", 14).onClick { close() })
	}
}