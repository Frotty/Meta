package de.fatox.meta.ui.dialogs

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisCheckBox
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import de.fatox.meta.api.model.GLShaderData
import de.fatox.meta.error.MetaError
import de.fatox.meta.error.MetaErrorHandler
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.Inject
import de.fatox.meta.injection.Singleton
import de.fatox.meta.shader.MetaShaderLibrary
import de.fatox.meta.ui.components.AssetSelectButton
import de.fatox.meta.ui.components.MetaInputValidator
import de.fatox.meta.ui.components.MetaValidTextField
import de.fatox.meta.ui.windows.MetaDialog
import de.fatox.meta.ui.windows.ShaderLibraryWindow

/**
 * Created by Frotty on 29.06.2016.
 */
@Singleton
class ShaderWizardDialog : MetaDialog("Shader Wizard", true) {
    @Inject
    private val shaderLibrary: MetaShaderLibrary? = null

    @Inject
    private val projectManager: ProjectManager? = null
    private val cancelBtn: VisTextButton
    private val createBtn: VisTextButton
    private val shaderNameTF: MetaValidTextField
    private val renderTargetGroup = ButtonGroup<VisCheckBox>()
    private var vertexSelect: AssetSelectButton? = null
    private var fragmentSelect: AssetSelectButton? = null
    private fun checkButton() {
        createBtn.isDisabled = (shaderNameTF.textField.text.isBlank()
                || !vertexSelect!!.hasFile()
                || !fragmentSelect!!.hasFile())
    }

    private fun setupTable() {
        val visTable = VisTable()
        visTable.defaults().pad(4f)
        visTable.add(shaderNameTF.description).growX()
        visTable.add(shaderNameTF.textField).growX()
        visTable.row()
        val visLabel = VisLabel("Render Target:")
        visLabel.setAlignment(Align.center)
        visTable.add(visLabel).colspan(2).pad(4f)
        visTable.row()
        val geometryButton = VisCheckBox("Geometry", true)
        val fullscreenButton = VisCheckBox("Fullscreen", false)
        visTable.add(geometryButton)
        visTable.add(fullscreenButton)
        visTable.row()
        val visLabel2 = VisLabel("Shader Files:")
        visLabel2.setAlignment(Align.center)
        visTable.add(visLabel2).colspan(2).pad(4f)
        visTable.row()
        vertexSelect = AssetSelectButton("Vertex Shader")
        vertexSelect!!.setSelectListener { file: FileHandle? -> checkButton() }
        visTable.add(vertexSelect!!.table).colspan(2).growX()
        visTable.row()
        fragmentSelect = AssetSelectButton("Fragment Shader")
        fragmentSelect!!.setSelectListener { file: FileHandle? -> checkButton() }
        visTable.add(fragmentSelect!!.table).colspan(2).growX()
        visTable.row()
        renderTargetGroup.add(geometryButton)
        renderTargetGroup.add(fullscreenButton)
        contentTable.add(visTable).top().growX()
    }

    init {
        cancelBtn = addButton(VisTextButton("Cancel"), Align.left, false)
        createBtn = addButton(VisTextButton("Create"), Align.right, true)
        shaderNameTF = MetaValidTextField("Shader name:", statusLabel)
        shaderNameTF.addValidator(object : MetaInputValidator() {
            override fun validateInput(input: String?, errors: MetaErrorHandler?) {
                if (input!!.isBlank()) {
                    errors!!.add(object : MetaError("Invalid Shader name", "") {
                        override fun gotoError() {}
                    })
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
        setDialogListener({ `object`: Any ->
            if (`object` as Boolean) {
                val vertFile = projectManager!!.relativize(vertexSelect!!.file)
                val fragFile = projectManager.relativize(fragmentSelect!!.file)
                val shaderData = GLShaderData(shaderNameTF.textField.text, vertFile!!, fragFile!!)
                val glShaderHandle = shaderLibrary!!.newShader(shaderData)
                val window = uiManager.getWindow(ShaderLibraryWindow::class.java)
                if (window != null) {
                    window.addShader(glShaderHandle)
                }
            }
            close()
        })
    }
}