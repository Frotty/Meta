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
	override val preserveCenterOnAutoFit: Boolean = true
	fun show(stage: Stage) {
		stage.addActor(this)
		fitStaticSurfaceToContent(stage.width, stage.height)
		validate()
		setColor(1f, 1f, 1f, 0f)
		setPosition(
			((stage.width - width) / 2).roundToInt().toFloat(),
			((stage.height - height) / 2).roundToInt().toFloat()
		)
		addAction(Actions.alpha(0.925f, 0.5f))
	}

	init {
		contentTable.defaults().pad(MetaSpacing.SM)
		contentTable.add(MetaLabel(message ?: "", MetaType.BODY).apply { setWrap(true) }).growX()
		// Keep the only action outside the body viewport, matching MetaDialog's sticky action-row contract.
		add(MetaTextButton("Close").onClick { close() }).right().pad(MetaSpacing.SM)
	}
}
