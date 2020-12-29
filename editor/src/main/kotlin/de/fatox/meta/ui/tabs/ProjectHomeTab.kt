package de.fatox.meta.ui.tabs

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import de.fatox.meta.api.model.MetaProjectData
import de.fatox.meta.injection.Inject
import de.fatox.meta.ui.MetaEditorUI
import de.fatox.meta.ui.components.TextWidget
import de.fatox.meta.ui.windows.AssetDiscovererWindow
import de.fatox.meta.ui.windows.ShaderComposerWindow
import de.fatox.meta.ui.windows.ShaderLibraryWindow

/**
 * Created by Frotty on 06.06.2016.
 */
class ProjectHomeTab(private val projectData: MetaProjectData) : MetaTab(true, false) {
    private val visTable = VisTable()

    @Inject
    private val editorUI: MetaEditorUI? = null
    private fun setupTable() {
        visTable.top()
        visTable.row().height(128f)
        visTable.add(TextWidget(projectData.name))
        visTable.row().height(64f)
        visTable.add()
        visTable.row()
        val visLabel = VisLabel("This is your project home tab.")
        visLabel.setAlignment(Align.center)
        visTable.add(visLabel).padBottom(128f)
    }

    override fun getTabTitle(): String {
        return "home@" + projectData.name
    }

    override fun getContentTable(): Table {
        return visTable
    }

    override fun onShow() {
        super.onShow()
        editorUI!!.metaToolbar!!.clear()
        editorUI.metaToolbar!!.addAvailableWindow(AssetDiscovererWindow::class.java, null)
        editorUI.metaToolbar!!.addAvailableWindow(ShaderLibraryWindow::class.java, null)
        editorUI.metaToolbar!!.addAvailableWindow(ShaderComposerWindow::class.java, null)
    }

    init {
        setupTable()
    }
}