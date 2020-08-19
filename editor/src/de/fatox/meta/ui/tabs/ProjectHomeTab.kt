package de.fatox.meta.ui.tabs

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import de.fatox.meta.api.model.MetaProjectData
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
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

	private val editorUI: MetaEditorUI by lazyInject()

	override fun getTabTitle(): String {
		return "home@" + projectData.name
	}

	override fun getContentTable(): Table {
		return visTable
	}

	override fun onShow() {
		super.onShow()
		editorUI.metaToolbar.apply {
			clear()
			addAvailableWindow(AssetDiscovererWindow::class.java, null)
			addAvailableWindow(ShaderLibraryWindow::class.java, null)
			addAvailableWindow(ShaderComposerWindow::class.java, null)
		}
	}

	init {
		val visLabel = VisLabel("This is your project home tab.").apply { setAlignment(Align.center) }

		visTable.apply {
			top()
			row().height(128f)
			add(TextWidget(projectData.name))
			row().height(64f)
			add()
			row()
			add(visLabel).padBottom(128f)
		}
	}
}