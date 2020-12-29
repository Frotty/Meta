package de.fatox.meta.ui.dialogs

import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import de.fatox.meta.error.MetaError
import de.fatox.meta.error.MetaErrorHandler
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.Inject
import de.fatox.meta.injection.Singleton
import de.fatox.meta.shader.MetaShaderComposer
import de.fatox.meta.ui.components.MetaInputValidator
import de.fatox.meta.ui.components.MetaValidTextField
import de.fatox.meta.ui.windows.MetaDialog

/**
 * Created by Frotty on 29.06.2016.
 */
@Singleton
class ShaderCompositionWizard : MetaDialog("Composition Wizard", true) {
    @Inject
    private val metaShaderComposer: MetaShaderComposer? = null

    @Inject
    private val projectManager: ProjectManager? = null
    private val cancelBtn: VisTextButton
    private val createBtn: VisTextButton
    private val compNameTF: MetaValidTextField
    private fun checkButton() {
        createBtn.isDisabled = compNameTF.textField.text.isBlank()
    }

    private fun setupTable() {
        val visTable = VisTable()
        visTable.defaults().pad(4f)
        visTable.add(compNameTF.description).growX()
        visTable.add(compNameTF.textField).growX()
        visTable.row()
        contentTable.add(visTable).top().growX()
        setDialogListener({ `object`: Any? ->
            if (`object` != null) {
                if (`object` as Boolean) {
                    metaShaderComposer!!.newShaderComposition(compNameTF.textField.text)
                    //                    ShaderComposerWindow window = uiManager.getWindow(ShaderComposerWindow.class);
                    //                    if(window != null) {
                    //                        window.addComposition(shaderComposition);
                    //                    }
                }
            }
            close()
        })
    }

    init {
        cancelBtn = addButton(VisTextButton("Cancel"), Align.left, false)
        createBtn = addButton(VisTextButton("Create"), Align.right, true)
        compNameTF = MetaValidTextField("Composition name:", statusLabel)
        compNameTF.addValidator(object : MetaInputValidator() {
            override fun validateInput(input: String?, errors: MetaErrorHandler?) {
                if (input!!.isBlank()) {
                    errors!!.add(object : MetaError("Invalid composition name", "") {
                        override fun gotoError() {}
                    })
                } else {
                    checkButton()
                }
            }
        })
        createBtn.isDisabled = true
        setDefaultSize(400f, 120f)
        setupTable()
    }
}