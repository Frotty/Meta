package de.fatox.meta.ui.dialogs

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.Tooltip
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.file.FileChooser
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter
import com.kotcrab.vis.ui.widget.file.FileTypeFilter
import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.components.MetaClickListener
import de.fatox.meta.ui.components.MetaTextButton
import de.fatox.meta.ui.windows.MetaDialog
import de.fatox.meta.util.truncate

/**
 * Created by Frotty on 05.06.2016.
 */
class OpenProjectDialog : MetaDialog("Open Project", true) {
	private val languageBundle: LanguageBundle by lazyInject()
	private val fileChooser: FileChooser by lazyInject("open")
	private val projectManager: ProjectManager by lazyInject()

	private var folderLabel: VisLabel? = null
	private val openBtn: VisTextButton
	private var folderButton: MetaTextButton? = null
	private var rootfile: FileHandle? = null
	private fun createFolderButton() {
		folderLabel = VisLabel(languageBundle!!["newproj_dia_proj_root"])
		folderButton = MetaTextButton(languageBundle["newproj_dia_select_project"])
		folderButton!!.addListener(object : MetaClickListener() {
			override fun clicked(event: InputEvent, x: Float, y: Float) {
				fileChooser!!.selectionMode = FileChooser.SelectionMode.FILES
				val fileTypeFilter = FileTypeFilter(false)
				fileTypeFilter.addRule("Meta Project File", "json")
				fileChooser.setFileTypeFilter(fileTypeFilter)
				fileChooser.fadeIn()
				stage.keyboardFocus = fileChooser
				fileChooser.setListener(object : FileChooserAdapter() {
					override fun selected(file: Array<FileHandle>) {
						if (file.size == 1) {
							rootfile = file[0]
							folderButton!!.setText(file[0].pathWithoutExtension().truncate(30))
							if (projectManager!!.verifyProjectFile(rootfile)) {
								openBtn.isDisabled = false
							}
						}
						fileChooser.fadeOut()
					}
				})
				stage.addActor(fileChooser)
				fileChooser.fadeIn()
			}
		})
		Tooltip.Builder(languageBundle["newproj_dia_tooltip_location"]).target(folderLabel).build()
	}

	init {
		setDefaultSize(450f, 200f)
		addButton<Button>(VisTextButton("Cancel"), Align.left, false)
		openBtn = addButton(VisTextButton("Open"), Align.left, true)
		openBtn.isDisabled = true
		createFolderButton()
		val visTable = VisTable()
		visTable.defaults().pad(4f)
		visTable.add(folderLabel).growX()
		visTable.add(folderButton).growX()
		visTable.row()
		contentTable.add(visTable).top().growX()
		dialogListener = object : DialogListener {
			override fun onResult(any: Any?) {
				if (any != null && any as Boolean) {
					projectManager.loadProject(rootfile)
				}
				close()
			}
		}
	}
}