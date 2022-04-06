package de.fatox.meta.ui.windows

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.kotcrab.vis.ui.widget.VisTextField
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.components.MetaLabel

/**
 * Created by Frotty on 20.05.2016.
 */
class CameraWindow : MetaWindow("Camera", true, true) {
	private val xPosField: VisTextField
	private val yPosField: VisTextField
	private val zPosField: VisTextField
	private val xUpField: VisTextField
	private val yUpField: VisTextField
	private val zUpField: VisTextField

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
		xPosField = VisTextField("0.0")
		yPosField = VisTextField("0.0")
		zPosField = VisTextField("0.0")
		xUpField = VisTextField("0.0")
		yUpField = VisTextField("0.0")
		zUpField = VisTextField("0.0")
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