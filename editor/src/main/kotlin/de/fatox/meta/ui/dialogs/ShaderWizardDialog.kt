package de.fatox.meta.ui.dialogs

import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.utils.Align
import de.fatox.meta.api.model.GLShaderData
import de.fatox.meta.api.ui.getWindow
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.shader.MetaShaderLibrary
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.bindDisabled
import de.fatox.meta.ui.components.AssetSelectButton
import de.fatox.meta.ui.components.MetaInputValidator
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaTable
import de.fatox.meta.ui.components.MetaTextButton
import de.fatox.meta.ui.components.MetaToggleButton
import de.fatox.meta.ui.components.MetaValidTextField
import de.fatox.meta.ui.windows.MetaDialog
import de.fatox.meta.ui.windows.MetaDialog.DialogListener
import de.fatox.meta.ui.windows.ShaderLibraryWindow

class ShaderWizardDialog : MetaDialog("Shader Wizard", true) {
	private val shaderLibrary: MetaShaderLibrary by lazyInject()
	private val projectManager: ProjectManager by lazyInject()

	private val createBtn: MetaTextButton
	private val shaderNameTF: MetaValidTextField = MetaValidTextField("Shader name:", statusLabel)
	private val renderTargetGroup = ButtonGroup<MetaToggleButton>()
	private lateinit var vertexSelect: AssetSelectButton
	private lateinit var fragmentSelect: AssetSelectButton

	private fun setupTable() {
		val visTable = MetaTable()
		visTable.defaults().pad(MetaSpacing.XS)
		visTable.add(shaderNameTF.description).growX()
		visTable.add(shaderNameTF.textField).growX()
		visTable.row()
		val visLabel = MetaLabel("Render Target:", 14)
		visLabel.setAlignment(Align.center)
		visTable.add(visLabel).colspan(2).pad(MetaSpacing.XS)
		visTable.row()
		val geometryButton = MetaToggleButton("Geometry", true)
		val fullscreenButton = MetaToggleButton("Fullscreen", false)
		visTable.add(geometryButton)
		visTable.add(fullscreenButton)
		visTable.row()
		val visLabel2 = MetaLabel("Shader Files:", 14)
		visLabel2.setAlignment(Align.center)
		visTable.add(visLabel2).colspan(2).pad(MetaSpacing.XS)
		visTable.row()
		vertexSelect = AssetSelectButton("Vertex Shader")
		visTable.add(vertexSelect.table).colspan(2).growX()
		visTable.row()
		fragmentSelect = AssetSelectButton("Fragment Shader")
		visTable.add(fragmentSelect.table).colspan(2).growX()
		visTable.row()
		renderTargetGroup.add(geometryButton)
		renderTargetGroup.add(fullscreenButton)
		contentTable.add(visTable).top().growX()
	}

	init {
		addButton<MetaTextButton>(MetaTextButton("Cancel"), Align.left, false)
		createBtn = addButton(MetaTextButton("Create"), Align.right, true)
		shaderNameTF.addValidator(MetaInputValidator.required("Invalid Shader name"))
		renderTargetGroup.setMaxCheckCount(1)
		renderTargetGroup.setMinCheckCount(1)
		setDefaultSize(300f, 450f)
		setupTable()
		dialogListener = DialogListener { any ->
			if (any == true) {
				val vertFile = projectManager.relativize(vertexSelect.file!!)
				val fragFile = projectManager.relativize(fragmentSelect.file!!)
				val shaderData = GLShaderData(shaderNameTF.textField.text, vertFile, fragFile)
				val glShaderHandle = shaderLibrary.newShader(shaderData)!!
				val window = uiManager.getWindow<ShaderLibraryWindow>()
				window.addShader(glShaderHandle)
			}
			close()
		}
	}

	override fun onShown() {
		super.onShown()
		reactiveScope.bindDisabled(createBtn) {
			!shaderNameTF.textField.inputValidValue() || !vertexSelect.hasFile() || !fragmentSelect.hasFile()
		}
	}
}
