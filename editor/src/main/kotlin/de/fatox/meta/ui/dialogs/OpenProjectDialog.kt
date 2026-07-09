package de.fatox.meta.ui.dialogs

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.bindDisabled
import de.fatox.meta.ui.components.MetaFileChooser
import de.fatox.meta.ui.components.MetaFileChooserAdapter
import de.fatox.meta.ui.components.MetaFileTypeFilter
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaTable
import de.fatox.meta.ui.components.MetaTextButton
import de.fatox.meta.ui.components.MetaTooltip
import de.fatox.meta.ui.windows.MetaDialog
import de.fatox.meta.ui.windows.MetaDialog.DialogListener
import de.fatox.meta.util.truncate

/**
 * Created by Frotty on 05.06.2016.
 */
class OpenProjectDialog : MetaDialog("Open Project", true) {
	private val languageBundle: LanguageBundle by lazyInject()
	private val fileChooser: MetaFileChooser by lazyInject("open")
	private val projectManager: ProjectManager by lazyInject()

	private var folderLabel: MetaLabel? = null
	private val openBtn: MetaTextButton
	private var folderButton: MetaTextButton? = null
	private val rootFileValue = signal<FileHandle?>(null)
	private fun createFolderButton() {
		folderLabel = MetaLabel(languageBundle["newproj.dia.project.root"], 14)
		folderButton = MetaTextButton(languageBundle["newproj.dia.select.project"])
		folderButton!!.addListener(object : ClickListener() {
			override fun clicked(event: InputEvent, x: Float, y: Float) {
				fileChooser.selectionMode = MetaFileChooser.SELECT_FILES
				val fileTypeFilter = MetaFileTypeFilter(false)
				fileTypeFilter.addRule("Meta Project File", "json")
				fileChooser.setFileTypeFilter(fileTypeFilter)
				fileChooser.fadeIn()
				stage.keyboardFocus = fileChooser
				fileChooser.setListener(object : MetaFileChooserAdapter() {
					override fun selected(file: Array<FileHandle>) {
						if (file.size == 1) {
							rootFileValue.value = file[0].takeIf { projectManager.verifyProjectFile(it) }
							folderButton!!.setText(file[0].pathWithoutExtension().truncate(30))
						} else {
							rootFileValue.value = null
						}
						fileChooser.fadeOut()
					}
				})
				stage.addActor(fileChooser)
				fileChooser.fadeIn()
			}
		})
		MetaTooltip.attach(folderLabel, languageBundle["newproj.dia.tooltip.location"])
	}

	init {
		setDefaultSize(450f, 200f)
		addButton<Button>(MetaTextButton("Cancel"), Align.left, false)
		openBtn = addButton(MetaTextButton("Open"), Align.left, true)
		createFolderButton()
		val visTable = MetaTable()
		visTable.defaults().pad(4f)
		visTable.add(folderLabel).growX()
		visTable.add(folderButton).growX()
		visTable.row()
		contentTable.add(visTable).top().growX()
		dialogListener = DialogListener { any ->
			if (any == true) {
				projectManager.loadProject(rootFileValue.peek()!!)
			}
			close()
		}
	}

	override fun onShown() {
		super.onShown()
		reactiveScope.bindDisabled(openBtn) { rootFileValue() == null }
	}
}
