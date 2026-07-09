package de.fatox.meta.ui.dialogs

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.api.model.MetaProjectData
import de.fatox.meta.error.MetaError
import de.fatox.meta.error.MetaErrorHandler
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.bindDisabled
import de.fatox.meta.ui.components.MetaCheckBox
import de.fatox.meta.ui.components.MetaFileChooser
import de.fatox.meta.ui.components.MetaFileChooserAdapter
import de.fatox.meta.ui.components.MetaInputValidator
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaTable
import de.fatox.meta.ui.components.MetaTextButton
import de.fatox.meta.ui.components.MetaTooltip
import de.fatox.meta.ui.components.MetaValidTextField
import de.fatox.meta.ui.windows.MetaDialog
import de.fatox.meta.ui.windows.MetaDialog.DialogListener
import de.fatox.meta.util.isValidFolderName
import de.fatox.meta.util.truncate

class ProjectWizardDialog : MetaDialog("Project Wizard", true) {
	private val createBtn: MetaTextButton

	private val languageBundle: LanguageBundle by lazyInject()
	private val fileChooser: MetaFileChooser by lazyInject("open")
	private val projectManager: ProjectManager by lazyInject()

	private lateinit var projectNameTF: MetaValidTextField
	private lateinit var folderButton: MetaTextButton
	private lateinit var folderLabel: MetaLabel
	private val rootFileValue = signal<FileHandle?>(null)
	private lateinit var checkbox: MetaCheckBox
	private lateinit var checkboxLabel: MetaLabel
	private fun createExampleCheckbox() {
		checkboxLabel = MetaLabel("Include Example:", 14)
		checkbox = MetaCheckBox(initialChecked = true)
		MetaTooltip.attach(checkboxLabel, languageBundle["newproj.dia.tooltip.example"])
	}

	private fun createFolderButton() {
		folderLabel = MetaLabel(languageBundle["newproj.dia.project.root"], 14)
		folderButton = MetaTextButton(languageBundle["newproj.dia.select.folder"])
		folderButton.addListener(object : ClickListener() {
			override fun clicked(event: InputEvent, x: Float, y: Float) {
				fileChooser.selectionMode = MetaFileChooser.SELECT_DIRECTORIES
				fileChooser.fadeIn()
				fileChooser.setListener(object : MetaFileChooserAdapter() {
					override fun selected(file: Array<FileHandle>) {
						if (file.size == 1) {
							rootFileValue.value = file[0]
							folderButton.setText(file[0].pathWithoutExtension().truncate(20))
						} else {
							rootFileValue.value = null
						}
						fileChooser.fadeOut()
					}
				})
				stage.addActor(fileChooser)
				fileChooser.fadeIn()
			}
		})
		MetaTooltip.attach(folderLabel, languageBundle["newproj.dia.tooltip.location"])
	}

	private fun createProjectNameTF() {
		projectNameTF = MetaValidTextField(languageBundle["newproj.dia.name.tf"], statusLabel)
		projectNameTF.addValidator(object : MetaInputValidator() {
			override fun validateInput(input: String, errors: MetaErrorHandler) {
				if (!input.isValidFolderName()) {
					errors.add(MetaError(languageBundle["newproj.dia.invalid.name"], "Name can only contain alphanumeric characters"))
				}
			}
		})
		MetaTooltip.attach(projectNameTF.description, languageBundle["newproj.dia.tooltip.name"])
	}

	init {
		addButton<Button>(MetaTextButton("Cancel"), Align.left, false)
		createBtn = addButton(MetaTextButton("Create"), Align.right, true)
		createProjectNameTF()
		createFolderButton()
		createExampleCheckbox()
		val visTable = MetaTable()
		visTable.defaults().pad(5f)
		visTable.columnDefaults(0).right().width(LABEL_WIDTH)
		visTable.columnDefaults(1).left().width(CONTROL_WIDTH)
		visTable.add(projectNameTF.description).right()
		visTable.add(projectNameTF.textField).growX().height(FIELD_HEIGHT)
		visTable.row()
		visTable.add(folderLabel).right()
		visTable.add(folderButton).growX().height(BUTTON_HEIGHT)
		visTable.row()
		visTable.add(checkboxLabel).right()
		visTable.add(checkbox).left().size(CHECKBOX_SIZE)
		contentTable.add(visTable).top().growX()
		pack()
		dialogListener = DialogListener { any ->
			if (any == true) {
				val metaProjectData = MetaProjectData(projectNameTF.textField.text)
				projectManager.newProject(rootFileValue.peek()!!, metaProjectData)
				projectManager.loadProject(projectManager.currentProjectRoot)
			}
			close()
		}
	}

	override fun onShown() {
		super.onShown()
		reactiveScope.bindDisabled(createBtn) {
			rootFileValue() == null || !projectNameTF.textField.inputValidValue()
		}
	}

	private companion object {
		const val LABEL_WIDTH = 128f
		const val CONTROL_WIDTH = 190f
		const val FIELD_HEIGHT = 34f
		const val BUTTON_HEIGHT = 30f
		const val CHECKBOX_SIZE = 28f
	}
}
