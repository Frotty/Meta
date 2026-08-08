package de.fatox.meta.ui.dialogs

import com.badlogic.gdx.utils.Align
import de.fatox.meta.ide.SceneManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.bindDisabled
import de.fatox.meta.ui.components.MetaInputValidator
import de.fatox.meta.ui.components.MetaTable
import de.fatox.meta.ui.components.MetaTextButton
import de.fatox.meta.ui.components.MetaValidTextField
import de.fatox.meta.ui.windows.MetaDialog
import de.fatox.meta.ui.windows.MetaDialog.DialogListener

class SceneWizardDialog : MetaDialog("Scene Wizard", true) {
	private val createBtn: MetaTextButton

	private val sceneManager: SceneManager by lazyInject()

	private val sceneNameTF: MetaValidTextField

	init {
		addButton<MetaTextButton>(MetaTextButton("Cancel"), Align.left, false)
		createBtn = addButton(MetaTextButton("Create"), Align.right, true)
		sceneNameTF = MetaValidTextField("Scene name:", statusLabel)
		sceneNameTF.addValidator(MetaInputValidator.required("Scene name required"))
		val visTable = MetaTable()
		visTable.defaults().pad(MetaSpacing.XS)
		visTable.add(sceneNameTF.description).growX()
		visTable.add(sceneNameTF.textField).growX()
		visTable.row()
		contentTable.add(visTable).top().growX()
		dialogListener = DialogListener { any ->
			close()
			if (any == true) {
				sceneManager.createNew(sceneNameTF.textField.text)
			}
		}
		pack()
	}

	override fun onShown() {
		super.onShown()
		reactiveScope.bindDisabled(createBtn) { !sceneNameTF.textField.inputValidValue() }
	}
}
