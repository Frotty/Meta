package de.fatox.meta.ui.dialogs

import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import de.fatox.meta.error.MetaError
import de.fatox.meta.error.MetaErrorHandler
import de.fatox.meta.ide.SceneManager
import de.fatox.meta.injection.Inject
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.injection.Singleton
import de.fatox.meta.ui.components.MetaInputValidator
import de.fatox.meta.ui.components.MetaValidTextField
import de.fatox.meta.ui.windows.MetaDialog

/**
 * Created by Frotty on 13.06.2016.
 */
@Singleton
class SceneWizardDialog : MetaDialog("Scene Wizard", true) {
	private val cancelBtn: VisTextButton
	private val createBtn: VisTextButton

	private val sceneManager: SceneManager by lazyInject()

	private val sceneNameTF: MetaValidTextField

	init {
		cancelBtn = addButton(VisTextButton("Cancel"), Align.left, false)
		createBtn = addButton(VisTextButton("Create"), Align.right, true)
		sceneNameTF = MetaValidTextField("Scene name:", statusLabel)
		sceneNameTF.addValidator(object : MetaInputValidator() {
			override fun validateInput(input: String?, errors: MetaErrorHandler) {
				if (input.isNullOrBlank()) {
					errors!!.add(object : MetaError("Scene name required", "") {
						override fun gotoError() {}
					})
				} else {
					createBtn.isDisabled = false
				}
			}
		})
		val visTable = VisTable()
		visTable.defaults().pad(4f)
		visTable.add(sceneNameTF.description).growX()
		visTable.add(sceneNameTF.textField).growX()
		visTable.row()
		contentTable.add(visTable).top().growX()
		createBtn.isDisabled = true
		dialogListener = object : DialogListener {
			override fun onResult(any: Any?) {
				close()
				if (any as Boolean) {
					sceneManager.createNew(sceneNameTF.textField.text)
				}
			}
		}
		setDefaultSize(200f, 400f)
	}
}