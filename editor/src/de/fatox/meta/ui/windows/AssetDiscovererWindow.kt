package de.fatox.meta.ui.windows

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Scaling
import com.kotcrab.vis.ui.widget.*
import de.fatox.meta.api.model.AssetDiscovererData
import de.fatox.meta.ide.AssetDiscoverer
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.FolderListAdapter
import de.fatox.meta.ui.components.MetaClickListener
import de.fatox.meta.ui.components.MetaIconTextButton

/**
 * Created by Frotty on 07.06.2016.
 */
object AssetDiscovererWindow : MetaWindow("Asset Discoverer", true, true) {
	private const val TAG = "adwSettings"

	private val assetDiscoverer: AssetDiscoverer by lazyInject()

	private var adapter: FolderListAdapter<FolderModel>? = null
	private var filePane: ScrollPane? = null
	private var view: ListView<FolderModel>? = null
	private val toolbarTable = VisTable()
	private val fileViewTable = VisTable()
	private var selectionMode = false
	private var listener: SelectListener? = null
	private var data: AssetDiscovererData? = null

	private open class FolderModel(val fileHandle: FileHandle) {
		override fun toString(): String {
			return fileHandle.nameWithoutExtension() + "/"
		}
	}

	private fun loadLastFolder() {
		assetDiscoverer.setRoot("")
		if (uiManager.metaHas(TAG)) {
			data = uiManager.metaGet(TAG, AssetDiscovererData::class.java)
			assetDiscoverer.openChild(data!!.lastFolder)
		}
	}

	private fun setup() {
		adapter = FolderListAdapter(Array())
		view = ListView(adapter)
		view!!.mainTable.defaults().pad(2f)
		view!!.setItemClickListener {
			assetDiscoverer.openFolder((it as FolderModel).fileHandle)
		}
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
		val newFileButton = VisImageButton(assetProvider.getDrawable("ui/appbar.page.add.png"))
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
		if (assetDiscoverer.currentChildFiles == null) {
			return
		}
		val visTable2 = VisTable()
		visTable2.defaults().pad(2f)
		visTable2.top()
		visTable2.setSize(((width - 128).toInt()).toFloat(), height - 64)
		visTable2.row().height(78f)
		var counter = 0f
		for (file in assetDiscoverer.currentChildFiles!!) {
			val fileButton = MetaIconTextButton(file.name(), assetProvider.getDrawable("ui/appbar.page.text.png"), 78)
			fileButton.addListener(object : MetaClickListener() {
				override fun clicked(event: InputEvent, x: Float, y: Float) {
					if (selectionMode) {
						listener!!.onSelect(file)
						listener = null
						selectionMode = false
					} else {
						assetDiscoverer.openFile(file)
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
			filePane!!.actor = visTable2
		}
	}

	private fun refreshFolderView() {
		adapter!!.clear()
		if (assetDiscoverer.currentFolder!!.path() != assetDiscoverer.root!!.path()) {
			adapter!!.add(object : FolderModel(assetDiscoverer.currentFolder!!.parent()) {
				override fun toString(): String {
					return "../"
				}
			})
		}
		for (child in assetDiscoverer.currentChildFolders!!) {
			adapter!!.add(FolderModel(child))
		}
		view!!.rebuildView()
	}

	fun refresh() {
		createFilePane()
		refreshFolderView()
	}

	fun enableSelectionMode(selectListener: SelectListener) {
		toFront()
		stage.keyboardFocus = this
		listener = selectListener
		selectionMode = true
	}

	fun interface SelectListener {
		fun onSelect(fileHandle: FileHandle?)
	}

	init {
		setSize(500f, 200f)
		loadLastFolder()
		setup()
		refresh()
	}
}