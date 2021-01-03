package de.fatox.meta.ui.tabs

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.LinkLabel
import com.kotcrab.vis.ui.widget.LinkLabel.LinkLabelListener
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import de.fatox.meta.assets.MetaData
import de.fatox.meta.assets.get
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.components.TextWidget

/**
 * Created by Frotty on 05.06.2016.
 */
class WelcomeTab : MetaTab(false, false) {
	private val visTable = VisTable()

	private val metaData: MetaData by lazyInject()
	private val projectManager: ProjectManager by lazyInject()

	override fun getTabTitle(): String {
		return "Home"
	}

	override fun getContentTable(): Table {
		return visTable
	}

	init {
		visTable.top()
		visTable.row().height(128f)
		visTable.add(TextWidget("Meta"))
		visTable.row().height(64f)
		visTable.add()
		visTable.row()
		val visLabel = VisLabel("Welcome to the Meta Engine\nCreate or load a project\n\nRecent projects:")
		visLabel.setAlignment(Align.center)
		visTable.add(visLabel).pad(16f)
		if (!metaData.has("lastProjects")) {
			metaData.save("lastProjects", Array<String>())
		}
		val lastProjects: Array<String> = metaData["lastProjects"]
		for (lastProj in lastProjects) {
			visTable.row()
			val linkLabel = LinkLabel(lastProj.substring(0, lastProj.lastIndexOf("/")))
			linkLabel.listener = LinkLabelListener { projectManager.loadProject(Gdx.files.absolute(lastProj)) }
			visTable.add(linkLabel).center().pad(2f)
		}
	}
}