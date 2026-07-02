package de.fatox.meta.ui.windows

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.Batch
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaTextField

/**
 * Created by Frotty on 20.05.2016.
 */
class CameraWindow : MetaWindow("Camera", true, true) {
	private val xPosField: MetaTextField
	private val yPosField: MetaTextField
	private val zPosField: MetaTextField
	private val xUpField: MetaTextField
	private val yUpField: MetaTextField
	private val zUpField: MetaTextField

	val camera: PerspectiveCamera by lazyInject()

	override fun draw(batch: Batch, parentAlpha: Float) {
		super.draw(batch, parentAlpha)
		xPosField.text = camera.position.x.toString() + ""
		yPosField.text = camera.position.y.toString() + ""
		zPosField.text = camera.position.z.toString() + ""
		xUpField.text = camera.up.x.toString() + ""
		yUpField.text = camera.up.y.toString() + ""
		zUpField.text = camera.up.z.toString() + ""
	}

	init {
		xPosField = MetaTextField("0.0")
		yPosField = MetaTextField("0.0")
		zPosField = MetaTextField("0.0")
		xUpField = MetaTextField("0.0")
		yUpField = MetaTextField("0.0")
		zUpField = MetaTextField("0.0")
		contentTable.add(MetaLabel("Position:", 14)).colspan(6).center().row()
		contentTable.add(MetaLabel("x:", 12))
		contentTable.add(xPosField).width(64f).pad(2f)
		contentTable.add(MetaLabel("y:", 12))
		contentTable.add(yPosField).width(64f).pad(2f)
		contentTable.add(MetaLabel("z:", 12))
		contentTable.add(zPosField).width(64f).pad(2f)
		contentTable.row()
		contentTable.add(MetaLabel("Up:", 14)).colspan(6).center().row()
		contentTable.add(MetaLabel("x:", 12))
		contentTable.add(xUpField).width(64f).pad(2f)
		contentTable.add(MetaLabel("y:", 12))
		contentTable.add(yUpField).width(64f).pad(2f)
		contentTable.add(MetaLabel("z:", 12))
		contentTable.add(zUpField).width(64f).pad(2f)
	}
}
