package de.fatox.meta.ui.dialogs

import com.badlogic.gdx.utils.Align
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.shader.MetaShaderComposer
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.bindDisabled
import de.fatox.meta.ui.components.MetaInputValidator
import de.fatox.meta.ui.components.MetaTable
import de.fatox.meta.ui.components.MetaTextButton
import de.fatox.meta.ui.components.MetaValidTextField
import de.fatox.meta.ui.windows.MetaDialog

class ShaderCompositionWizard : MetaDialog("Composition Wizard", true) {
	private val metaShaderComposer: MetaShaderComposer by lazyInject()
	private val projectManager: ProjectManager by lazyInject()

	private val createBtn: MetaTextButton
	private val compNameTF: MetaValidTextField = MetaValidTextField("Composition name:", statusLabel)

	private fun setupTable() {
		val visTable = MetaTable()
		visTable.defaults().pad(MetaSpacing.XS)
		visTable.add(compNameTF.description).growX()
		visTable.add(compNameTF.textField).growX()
		visTable.row()
		contentTable.add(visTable).top().growX()
		dialogListener = DialogListener { any ->
			if (any == true) {
				metaShaderComposer.newShaderComposition(compNameTF.textField.text)
			}
			close()
		}
	}

	init {
		addButton<MetaTextButton>(MetaTextButton("Cancel"), Align.left, false)
		createBtn = addButton(MetaTextButton("Create"), Align.right, true)
		compNameTF.addValidator(MetaInputValidator.required("Invalid composition name"))
		setDefaultSize(400f, 120f)
		setupTable()
	}

	override fun onShown() {
		super.onShown()
		reactiveScope.bindDisabled(createBtn) { !compNameTF.textField.inputValidValue() }
	}
}
