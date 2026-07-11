package de.fatox.meta.ui.windows

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import de.fatox.meta.api.extensions.onClick
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaTextButton
import kotlin.math.roundToInt

/**
 * Created by Frotty on 14.06.2016.
 */
class MetaConfirmDialog(title: String = "", message: String?) :
	MetaWindow(title, false, true, hasHeader = title.isNotBlank()) {
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
		defaults().pad(MetaSpacing.SM)
		add(MetaLabel(message ?: "", MetaType.BODY)).growX()
		row()
		add(MetaTextButton("Close").onClick { close() }).right()
	}
}
