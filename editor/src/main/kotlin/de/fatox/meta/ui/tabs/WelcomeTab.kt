package de.fatox.meta.ui.tabs

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.extensions.onClick
import de.fatox.meta.assets.MetaData
import de.fatox.meta.assets.get
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.lastProjectsKey
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaTable
import de.fatox.meta.ui.components.MetaTextButton
import de.fatox.meta.ui.components.TextWidget

/**
 * Created by Frotty on 05.06.2016.
 */
class WelcomeTab : MetaTab(false, false) {
	private val visTable = MetaTable()

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
		val visLabel = MetaLabel("Welcome to the Meta Engine\nCreate or load a project\n\nRecent projects:", 16)
		visLabel.setAlignment(Align.center)
		visTable.add(visLabel).pad(16f)
		if (!metaData.has(lastProjectsKey)) metaData.save(lastProjectsKey, Array())
		val lastProjects = metaData[lastProjectsKey]
		for (lastProj in lastProjects) {
			visTable.row()
			val linkLabel = MetaTextButton(lastProj.substring(0, lastProj.lastIndexOf("/")), 14)
				.onClick { projectManager.loadProject(Gdx.files.absolute(lastProj)) }
			visTable.add(linkLabel).center().pad(2f)
		}
	}
}
