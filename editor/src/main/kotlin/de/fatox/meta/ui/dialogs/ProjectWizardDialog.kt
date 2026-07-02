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
	private var rootfile: FileHandle? = null
	private var namevalid = false
	private var locationValid = false
	private var checkbox: MetaCheckBox? = null
	private var checkboxLabel: MetaLabel? = null
	private fun createExampleCheckbox() {
		checkboxLabel = MetaLabel("Include Example:", 14)
		checkbox = MetaCheckBox(initialChecked = true)
		MetaTooltip.attach(checkboxLabel, languageBundle["newproj.dia.tooltip.example"])
	}

	private fun checkValid() {
		if (locationValid && namevalid) {
			createBtn.isDisabled = false
		}
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
							rootfile = file[0]
							folderButton.setText(file[0].pathWithoutExtension().truncate(20))
							locationValid = true
						} else {
							locationValid = false
						}
						fileChooser.fadeOut()
						checkValid()
					}
				})
				stage.addActor(fileChooser)
				fileChooser.fadeIn()
			}
		})
		MetaTooltip.attach(folderLabel, languageBundle["newproj.dia.tooltip.location"])
	}

	private fun createProjectNameTF() {
		val projectWizard = this
		projectNameTF = MetaValidTextField(languageBundle["newproj.dia.name.tf"], statusLabel)
		projectNameTF.addValidator(object : MetaInputValidator() {
			override fun validateInput(input: String, errors: MetaErrorHandler) {
				if (!input.isValidFolderName()) {
					errors.add(MetaError(languageBundle["newproj.dia.invalid.name"], "Name can only contain alphanumeric characters"))
					namevalid = false
				}
				namevalid = true
				checkValid()
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
		visTable.defaults().pad(4f)
		visTable.add(projectNameTF.description).growX()
		visTable.add(projectNameTF.textField).growX()
		visTable.row()
		visTable.add(folderLabel).growX()
		visTable.add(folderButton).growX()
		visTable.row()
		visTable.add(checkboxLabel).growX()
		visTable.add(checkbox).growX()
		contentTable.add(visTable).top().growX()
		createBtn.isDisabled = true
		pack()
		dialogListener = DialogListener { any ->
			if (any as Boolean) {
				val metaProjectData = MetaProjectData(projectNameTF.textField.text)
				projectManager.newProject(rootfile!!, metaProjectData)
				projectManager.loadProject(projectManager.currentProjectRoot)
			}
			close()
		}
	}
}
