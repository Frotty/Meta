package de.fatox.meta.ui.dialogs

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.*
import com.kotcrab.vis.ui.widget.file.FileChooser
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter
import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.api.model.MetaProjectData
import de.fatox.meta.error.MetaError
import de.fatox.meta.error.MetaErrorHandler
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.components.MetaInputValidator
import de.fatox.meta.ui.components.MetaTextButton
import de.fatox.meta.ui.components.MetaValidTextField
import de.fatox.meta.ui.windows.MetaDialog
import de.fatox.meta.ui.windows.MetaDialog.DialogListener
import de.fatox.meta.util.isValidFolderName
import de.fatox.meta.util.truncate

class ProjectWizardDialog : MetaDialog("Project Wizard", true) {
	private val createBtn: VisTextButton

	private val languageBundle: LanguageBundle by lazyInject()
	private val fileChooser: FileChooser by lazyInject("open")
	private val projectManager: ProjectManager by lazyInject()

	private lateinit var projectNameTF: MetaValidTextField
	private lateinit var folderButton: MetaTextButton
	private lateinit var folderLabel: VisLabel
	private var rootfile: FileHandle? = null
	private var namevalid = false
	private var locationValid = false
	private var checkbox: VisCheckBox? = null
	private var checkboxLabel: VisLabel? = null
	private fun createExampleCheckbox() {
		checkboxLabel = VisLabel("Include Example:")
		checkbox = VisCheckBox("", true)
		Tooltip.Builder(languageBundle["newproj.dia.tooltip.example"]).target(checkboxLabel).build()
	}

	private fun checkValid() {
		if (locationValid && namevalid) {
			createBtn.isDisabled = false
		}
	}

	private fun createFolderButton() {
		folderLabel = VisLabel(languageBundle["newproj.dia.project.root"])
		folderButton = MetaTextButton(languageBundle["newproj.dia.select.folder"])
		folderButton.addListener(object : ClickListener() {
			override fun clicked(event: InputEvent, x: Float, y: Float) {
				fileChooser.selectionMode = FileChooser.SelectionMode.DIRECTORIES
				fileChooser.fadeIn()
				fileChooser.setListener(object : FileChooserAdapter() {
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
		Tooltip.Builder(languageBundle["newproj.dia.tooltip.location"]).target(folderLabel).build()
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
		Tooltip.Builder(languageBundle["newproj.dia.tooltip.name"]).target(projectNameTF.description).build()
	}

	init {
		addButton<Button>(VisTextButton("Cancel"), Align.left, false)
		createBtn = addButton(VisTextButton("Create"), Align.right, true)
		createProjectNameTF()
		createFolderButton()
		createExampleCheckbox()
		val visTable = VisTable()
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