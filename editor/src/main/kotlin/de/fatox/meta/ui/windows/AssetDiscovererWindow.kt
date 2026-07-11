package de.fatox.meta.ui.windows

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.extensions.onClick
import de.fatox.meta.api.model.AssetDiscovererData
import de.fatox.meta.api.ui.metaGet
import de.fatox.meta.ide.AssetDiscoverer
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.FolderListAdapter
import de.fatox.meta.ui.components.MetaIcon
import de.fatox.meta.ui.components.MetaImageButton
import de.fatox.meta.ui.components.MetaIconTextButton
import de.fatox.meta.ui.components.MetaListView
import de.fatox.meta.ui.components.MetaScrollPane
import de.fatox.meta.ui.components.MetaSeparator
import de.fatox.meta.ui.components.MetaTable
import de.fatox.meta.ui.components.MetaTextField

private const val TAG = "adwSettings"

/**
 * Created by Frotty on 07.06.2016.
 */
class AssetDiscovererWindow : MetaWindow("Asset Discoverer", true, true) {

	private val assetDiscoverer: AssetDiscoverer by lazyInject()

	private var adapter: FolderListAdapter<FolderModel>? = null
	private var filePane: ScrollPane? = null
	private lateinit var view: MetaListView<FolderModel>
	private val toolbarTable = MetaTable()
	private val fileViewTable = MetaTable()
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
			data = uiManager.metaGet(TAG)
			assetDiscoverer.openChild(data!!.lastFolder)
		}
	}

	private fun setup() {
		adapter = FolderListAdapter(Array())
		view = MetaListView(adapter!!).apply {
			mainTable.defaults().pad(2f)
			setItemClickListener {
				assetDiscoverer.openFolder((it as FolderModel).fileHandle)
			}
		}
		contentTable.apply {
			top().left()
			row().left().top().height(24f)
			add(toolbarTable).growX()
			row().height(1f)
			add(MetaSeparator()).growX()
			row()
			add(fileViewTable).left().grow()
		}
		createToolbarBar()
		createFileView()
	}

	private fun createToolbarBar() {
		toolbarTable.left().top()
		toolbarTable.row().height(24f)
		val newFileButton = MetaImageButton("ri-file-add-line")
		toolbarTable.add(newFileButton).size(24f).left()
		toolbarTable.add(MetaIcon("ri-search-line", 22)).size(24f).left()
		val searchTF = MetaTextField()
		toolbarTable.add(searchTF).height(24f).growX()
	}

	private fun createFileView() {
		fileViewTable.left()
		fileViewTable.add(view.mainTable).growY().pad(2f).minWidth(128f)
		fileViewTable.add(MetaSeparator()).width(2f).growY()
		createFilePane()
	}

	private fun createFilePane() {
		top()
		if (assetDiscoverer.currentChildFiles == null) {
			return
		}
		val visTable2 = MetaTable()
		visTable2.defaults().pad(2f)
		visTable2.top()
		visTable2.setSize(((width - 128).toInt()).toFloat(), height - 64)
		visTable2.row().height(78f)
		var counter = 0f
		for (file in assetDiscoverer.currentChildFiles!!) {
			val fileButton =
				MetaIconTextButton(file.name(), "ri-file-text-line", maxWidth = 78, vertical = true)
			fileButton.onClick {
				if (selectionMode) {
					listener!!.onSelect(file)
					listener = null
					selectionMode = false
				} else {
					assetDiscoverer.openFile(file)
				}
			}
			visTable2.add(fileButton).top().size(78f, 78f)
			counter += 78f
			if (counter > width - 128) {
				visTable2.row().height(78f)
			}
		}
		if (filePane == null) {
			filePane = MetaScrollPane(visTable2)
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
		view.rebuildView()
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
