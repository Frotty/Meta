package de.fatox.meta.ui.windows

import com.badlogic.gdx.utils.Scaling
import com.kotcrab.vis.ui.widget.Separator
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import de.fatox.meta.api.extensions.onClick
import de.fatox.meta.api.graphics.GLShaderHandle
import de.fatox.meta.api.ui.showDialog
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.shader.MetaShaderLibrary
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaTextButton
import de.fatox.meta.ui.dialogs.ShaderWizardDialog

/**
 * Created by Frotty on 28.06.2016.
 */
object ShaderLibraryWindow : MetaWindow("Shader Library", true, true) {
	private val shaderLibrary: MetaShaderLibrary by lazyInject()

	private val visTable: VisTable
	private val scrollPane: VisScrollPane

	fun addShader(shader: GLShaderHandle) {
		val metaTextButton = MetaTextButton(shader.data.name + ".msh", 16)
		metaTextButton.row()
		metaTextButton.add(MetaLabel(shader.vertexHandle.name() + "/" + shader.fragmentHandle.name(), 14))
		metaTextButton.row()
		metaTextButton.add(MetaLabel("Targets: " + shader.targets.size, 14))
		visTable.add(metaTextButton).growX()
		visTable.row()
	}

	private fun createToolbar() {
		val visImageButton = VisImageButton(assetProvider.getDrawable("ui/appbar.page.add.png"))
		visImageButton.onClick { uiManager.showDialog<ShaderWizardDialog>() }
		visImageButton.image.setScaling(Scaling.fill)
		visImageButton.image.setSize(24f, 24f)
		contentTable.row().size(26f)
		contentTable.add(visImageButton).size(24f).top().left()
		contentTable.row().height(1f)
		contentTable.add(Separator()).growX()
		contentTable.row()
	}

	init {
		setSize(240f, 320f)
		createToolbar()
		setPosition(1200f, 328f)
		visTable = VisTable()
		visTable.top()
		visTable.defaults().pad(4f)
		scrollPane = VisScrollPane(visTable)
		contentTable.add(scrollPane).top().grow()
		for (shader in shaderLibrary.getLoadedShaders()) {
			addShader(shader)
		}
	}
}