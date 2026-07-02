package de.fatox.meta.ui.dialogs

import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.utils.Align
import de.fatox.meta.api.model.GLShaderData
import de.fatox.meta.api.ui.getWindow
import de.fatox.meta.error.MetaError
import de.fatox.meta.error.MetaErrorHandler
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.shader.MetaShaderLibrary
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

/**
 * Created by Frotty on 29.06.2016.
 */
class ShaderWizardDialog : MetaDialog("Shader Wizard", true) {
	private val shaderLibrary: MetaShaderLibrary by lazyInject()
	private val projectManager: ProjectManager by lazyInject()

	private val cancelBtn: MetaTextButton = addButton(MetaTextButton("Cancel"), Align.left, false)
	private val createBtn: MetaTextButton = addButton(MetaTextButton("Create"), Align.right, true)
	private val shaderNameTF: MetaValidTextField = MetaValidTextField("Shader name:", statusLabel)
	private val renderTargetGroup = ButtonGroup<MetaToggleButton>()
	private lateinit var vertexSelect: AssetSelectButton
	private lateinit var fragmentSelect: AssetSelectButton
	private fun checkButton() {
		createBtn.isDisabled = (shaderNameTF.textField.text.isBlank()
			|| !vertexSelect.hasFile()
			|| !fragmentSelect.hasFile())
	}

	private fun setupTable() {
		val visTable = MetaTable()
		visTable.defaults().pad(4f)
		visTable.add(shaderNameTF.description).growX()
		visTable.add(shaderNameTF.textField).growX()
		visTable.row()
		val visLabel = MetaLabel("Render Target:", 14)
		visLabel.setAlignment(Align.center)
		visTable.add(visLabel).colspan(2).pad(4f)
		visTable.row()
		val geometryButton = MetaToggleButton("Geometry", true)
		val fullscreenButton = MetaToggleButton("Fullscreen", false)
		visTable.add(geometryButton)
		visTable.add(fullscreenButton)
		visTable.row()
		val visLabel2 = MetaLabel("Shader Files:", 14)
		visLabel2.setAlignment(Align.center)
		visTable.add(visLabel2).colspan(2).pad(4f)
		visTable.row()
		vertexSelect = AssetSelectButton("Vertex Shader")
		vertexSelect.setSelectListener { checkButton() }
		visTable.add(vertexSelect.table).colspan(2).growX()
		visTable.row()
		fragmentSelect = AssetSelectButton("Fragment Shader")
		fragmentSelect.setSelectListener { checkButton() }
		visTable.add(fragmentSelect.table).colspan(2).growX()
		visTable.row()
		renderTargetGroup.add(geometryButton)
		renderTargetGroup.add(fullscreenButton)
		contentTable.add(visTable).top().growX()
	}

	init {
		shaderNameTF.addValidator(object : MetaInputValidator() {
			override fun validateInput(input: String, errors: MetaErrorHandler) {
				if (input.isBlank()) {
					errors.add(MetaError("Invalid Shader name", ""))
				} else {
					checkButton()
				}
			}
		})
		renderTargetGroup.setMaxCheckCount(1)
		renderTargetGroup.setMinCheckCount(1)
		createBtn.isDisabled = true
		setDefaultSize(300f, 450f)
		setupTable()
		dialogListener = DialogListener { any ->
			if (any as Boolean) {
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
}
