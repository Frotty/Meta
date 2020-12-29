package de.fatox.meta.ui.windows

import de.fatox.meta.Meta.Companion.inject
import de.fatox.meta.shader.MetaSceneHandle.sceneFile
import de.fatox.meta.input.MetaInput.addAdapterForScreen
import de.fatox.meta.ui.EditorMenuBar.clear
import de.fatox.meta.ui.EditorMenuBar.addAvailableWindow
import de.fatox.meta.input.MetaInput.removeAdapterFromScreen
import de.fatox.meta.assets.MetaData.has
import de.fatox.meta.assets.MetaData.save
import de.fatox.meta.assets.MetaData.get
import de.fatox.meta.api.model.MetaProjectData.name
import de.fatox.meta.ui.windows.MetaWindow.setDefaultSize
import de.fatox.meta.ui.windows.MetaDialog.addButton
import de.fatox.meta.ui.windows.MetaWindow.contentTable
import de.fatox.meta.ui.windows.MetaWindow.close
import de.fatox.meta.api.lang.LanguageBundle.get
import de.fatox.meta.ui.components.MetaTextButton.setText
import de.fatox.meta.util.truncate
import de.fatox.meta.ui.components.MetaValidTextField.addValidator
import kotlin.text.isBlank
import de.fatox.meta.error.MetaErrorHandler.add
import de.fatox.meta.ui.components.MetaValidTextField.description
import de.fatox.meta.ui.components.MetaValidTextField.textField
import de.fatox.meta.ui.components.AssetSelectButton.file
import de.fatox.meta.shader.MetaShaderLibrary.newShader
import de.fatox.meta.ui.windows.MetaWindow.uiManager
import de.fatox.meta.api.ui.UIManager.getWindow
import de.fatox.meta.ui.components.AssetSelectButton.hasFile
import de.fatox.meta.ui.components.AssetSelectButton.setSelectListener
import de.fatox.meta.ui.components.AssetSelectButton.table
import de.fatox.meta.util.isValidFolderName
import de.fatox.meta.shader.MetaShaderComposer.newShaderComposition
import de.fatox.meta.ui.windows.MetaWindow.draw
import de.fatox.meta.shader.MetaSceneHandle.shaderComposition
import de.fatox.meta.shader.MetaSceneHandle.data
import de.fatox.meta.shader.MetaShaderComposer.compositions
import de.fatox.meta.shader.MetaShaderLibrary.getLoadedShaders
import de.fatox.meta.api.graphics.GLShaderHandle.data
import de.fatox.meta.api.model.GLShaderData.name
import de.fatox.meta.api.graphics.GLShaderHandle.vertexHandle
import de.fatox.meta.api.graphics.GLShaderHandle.fragmentHandle
import de.fatox.meta.api.graphics.GLShaderHandle.targets
import de.fatox.meta.api.AssetProvider.getDrawable
import de.fatox.meta.api.ui.UIManager.showDialog
import de.fatox.meta.api.ui.UIManager.metaHas
import de.fatox.meta.api.ui.UIManager.metaGet
import de.fatox.meta.api.model.AssetDiscovererData.lastFolder
import de.fatox.meta.api.ui.UIManager.setMainMenuBar
import de.fatox.meta.ui.EditorMenuBar.menuBar
import de.fatox.meta.api.ui.UIManager.changeScreen
import de.fatox.meta.api.ui.UIManager.bringWindowsToFront
import de.fatox.meta.api.ui.UIManager.addTable
import de.fatox.meta.shader.MetaShaderComposer.currentComposition
import de.fatox.meta.shader.ShaderComposition.compositionHandle
import de.fatox.meta.shader.MetaShaderComposer.getComposition
import de.fatox.meta.api.model.MetaSceneData.compositionPath
import de.fatox.meta.api.model.MetaProjectData.isValid
import de.fatox.meta.api.AssetProvider.get
import de.fatox.meta.api.ui.UIRenderer.update
import de.fatox.meta.api.ui.UIRenderer.draw
import de.fatox.meta.api.ui.UIManager.resize
import de.fatox.meta.api.model.MetaAudioVideoData.width
import de.fatox.meta.api.model.MetaAudioVideoData.height
import de.fatox.meta.api.model.MetaAudioVideoData.x
import de.fatox.meta.api.ui.UIManager.posModifier
import de.fatox.meta.api.PosModifier.getX
import de.fatox.meta.api.model.MetaAudioVideoData.y
import de.fatox.meta.api.PosModifier.getY
import de.fatox.meta.Meta
import de.fatox.meta.shader.MetaSceneHandle
import de.fatox.meta.ui.tabs.MetaTab
import de.fatox.meta.injection.Inject
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.input.MetaInput
import de.fatox.meta.ui.MetaEditorUI
import de.fatox.meta.camera.ArcCamControl
import com.badlogic.gdx.scenes.scene2d.ui.Table
import de.fatox.meta.ui.windows.AssetDiscovererWindow
import de.fatox.meta.ui.windows.ShaderLibraryWindow
import de.fatox.meta.ui.windows.ShaderComposerWindow
import de.fatox.meta.ui.windows.SceneOptionsWindow
import de.fatox.meta.ui.windows.PrimitivesWindow
import de.fatox.meta.ui.components.SceneWidget
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.ui.components.TextWidget
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.LinkLabel.LinkLabelListener
import com.badlogic.gdx.Gdx
import de.fatox.meta.api.model.MetaProjectData
import de.fatox.meta.ui.windows.MetaDialog
import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.ui.components.MetaTextButton
import com.badlogic.gdx.files.FileHandle
import de.fatox.meta.ui.components.MetaClickListener
import com.kotcrab.vis.ui.widget.file.FileTypeFilter
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter
import de.fatox.meta.injection.Singleton
import de.fatox.meta.ide.SceneManager
import de.fatox.meta.ui.components.MetaValidTextField
import de.fatox.meta.ui.components.MetaInputValidator
import de.fatox.meta.error.MetaErrorHandler
import de.fatox.meta.error.MetaError
import de.fatox.meta.shader.MetaShaderLibrary
import de.fatox.meta.ui.components.AssetSelectButton
import de.fatox.meta.ui.windows.AssetDiscovererWindow.SelectListener
import de.fatox.meta.api.model.GLShaderData
import de.fatox.meta.api.graphics.GLShaderHandle
import de.fatox.meta.ui.dialogs.ProjectWizardDialog
import de.fatox.meta.shader.MetaShaderComposer
import de.fatox.meta.ui.windows.MetaWindow
import com.badlogic.gdx.graphics.g2d.Batch
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.shader.ShaderComposition
import com.badlogic.gdx.scenes.scene2d.Actor
import de.fatox.meta.ui.tabs.SceneTab
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.ui.dialogs.ShaderWizardDialog
import com.badlogic.gdx.utils.Scaling
import de.fatox.meta.ide.AssetDiscoverer
import de.fatox.meta.ui.FolderListAdapter
import de.fatox.meta.ui.windows.AssetDiscovererWindow.FolderModel
import de.fatox.meta.api.model.AssetDiscovererData
import com.kotcrab.vis.ui.widget.ListView.ItemClickListener
import com.kotcrab.vis.ui.widget.ListView.ListViewTable
import de.fatox.meta.ui.components.MetaIconTextButton
import de.fatox.meta.ui.EditorMenuBar
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter
import de.fatox.meta.ui.tabs.WelcomeTab
import com.kotcrab.vis.ui.util.adapter.ArrayAdapter
import com.kotcrab.vis.ui.util.adapter.SimpleListAdapter.SimpleListAdapterStyle
import com.kotcrab.vis.ui.VisUI
import de.fatox.meta.api.model.MetaSceneData
import com.badlogic.gdx.utils.ObjectMap
import de.fatox.meta.ide.AssetOpenListener
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.math.Vector3
import de.fatox.meta.ide.MetaSceneManager
import de.fatox.meta.ui.tabs.ProjectHomeTab
import com.kotcrab.vis.ui.widget.toast.ToastTable
import java.lang.Exception
import java.nio.file.Paths
import com.badlogic.gdx.utils.I18NBundle
import de.fatox.meta.api.lang.AvailableLanguages
import java.util.Locale
import de.fatox.meta.lang.MetaLanguageBundle
import de.fatox.meta.graphics.renderer.FullscreenShader
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import de.fatox.meta.api.graphics.FontProvider
import com.badlogic.gdx.graphics.FPSLogger
import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.Shaders
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.*

/**
 * Created by Frotty on 07.06.2016.
 */
@Singleton
class AssetDiscovererWindow : MetaWindow("Asset Discoverer", true, true) {
    @Inject
    private override val assetProvider: AssetProvider? = null

    @Inject
    private val assetDiscoverer: AssetDiscoverer? = null
    private var adapter: FolderListAdapter<FolderModel?>? = null
    private var filePane: ScrollPane? = null
    private var view: ListView<*>? = null
    private val toolbarTable = VisTable()
    private val fileViewTable = VisTable()
    private var selectionMode = false
    private var listener: SelectListener? = null
    private var data: AssetDiscovererData? = null

    private open inner class FolderModel(val fileHandle: FileHandle?) {
        override fun toString(): String {
            return fileHandle!!.nameWithoutExtension() + "/"
        }
    }

    private fun loadLastFolder() {
        assetDiscoverer!!.setRoot("")
        if (uiManager.metaHas(TAG)) {
            data = uiManager.metaGet(TAG, AssetDiscovererData::class.java)
            assetDiscoverer.openChild(data!!.lastFolder)
        }
    }

    override fun addAction(action: Action) {
        super.addAction(action)
    }

    private fun setup() {
        adapter = FolderListAdapter(Array())
        view = ListView(adapter)
        view.getMainTable().defaults().pad(2f)
        view.setItemClickListener(ItemClickListener { item: Any -> assetDiscoverer!!.openFolder((item as FolderModel).fileHandle) })
        contentTable.top().left()
        contentTable.row().left().top().height(24f)
        contentTable.add(toolbarTable).growX()
        contentTable.row().height(1f)
        contentTable.add(Separator()).growX()
        contentTable.row()
        contentTable.add(fileViewTable).left().grow()
        createToolbarBar()
        createFileView()
    }

    private fun createToolbarBar() {
        toolbarTable.left().top()
        toolbarTable.row().height(24f)
        val newFileButton = VisImageButton(assetProvider!!.getDrawable("ui/appbar.page.add.png"))
        newFileButton.image.setScaling(Scaling.fill)
        toolbarTable.add(newFileButton).size(24f).left()
        val searchIcon = VisImage(assetProvider.getDrawable("ui/appbar.page.search.png"))
        searchIcon.setScaling(Scaling.fill)
        toolbarTable.add(searchIcon).size(24f).left()
        val searchTF = VisTextField()
        toolbarTable.add(searchTF).height(24f).growX()
    }

    private fun createFileView() {
        fileViewTable.left()
        fileViewTable.add(view!!.mainTable).growY().pad(2f).minWidth(128f)
        fileViewTable.add(Separator()).width(2f).growY()
        createFilePane()
    }

    private fun createFilePane() {
        top()
        if (assetDiscoverer.getCurrentChildFiles() == null) {
            return
        }
        val visTable2 = VisTable()
        visTable2.defaults().pad(2f)
        visTable2.top()
        visTable2.setSize((width - 128) as Int.toFloat(), height - 64)
        visTable2.row().height(78f)
        var counter = 0f
        for (file in assetDiscoverer.getCurrentChildFiles()) {
            val fileButton =
                MetaIconTextButton(file!!.name(), assetProvider!!.getDrawable("ui/appbar.page.text.png"), 78)
            fileButton.addListener(object : MetaClickListener() {
                override fun clicked(event: InputEvent, x: Float, y: Float) {
                    if (selectionMode) {
                        listener!!.onSelect(file)
                        listener = null
                        selectionMode = false
                    } else {
                        assetDiscoverer!!.openFile(file)
                    }
                }
            })
            visTable2.add(fileButton).top().size(78f, 78f)
            counter += 78f
            if (counter > width - 128) {
                visTable2.row().height(78f)
            }
        }
        if (filePane == null) {
            filePane = VisScrollPane(visTable2)
            fileViewTable.add(filePane).growY().top().pad(2f)
        } else {
            filePane!!.clear()
            filePane!!.widget = visTable2
        }
    }

    private fun refreshFolderView() {
        adapter!!.clear()
        if (assetDiscoverer.getCurrentFolder().path() != assetDiscoverer.getRoot().path()) {
            adapter!!.add(object : FolderModel(assetDiscoverer.getCurrentFolder().parent()) {
                override fun toString(): String {
                    return "../"
                }
            })
        }
        for (child in assetDiscoverer.getCurrentChildFolders()) {
            adapter!!.add(FolderModel(child))
        }
        view!!.rebuildView()
    }

    fun refresh() {
        createFilePane()
        refreshFolderView()
    }

    fun enableSelectionMode(selectListener: SelectListener?) {
        toFront()
        stage.keyboardFocus = this
        listener = selectListener
        selectionMode = true
    }

    interface SelectListener {
        fun onSelect(fileHandle: FileHandle?)
    }

    companion object {
        private const val TAG = "adwSettings"
    }

    init {
        setSize(500f, 200f)
        loadLastFolder()
        setup()
        refresh()
    }
}