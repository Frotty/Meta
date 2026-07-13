package de.fatox.meta.ui.components

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.extensions.onClick
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType
import de.fatox.meta.ui.windows.MetaWindow

/** Meta-owned file chooser with no platform toolkit or VisUI dependency. */
class MetaFileChooser(val mode: Mode) : MetaWindow(
	if (mode == Mode.OPEN) "Open" else "Save",
	resizable = true,
	closeButton = true,
) {
	enum class Mode { OPEN, SAVE }
	enum class SelectionMode { FILES, DIRECTORIES }

	var selectionMode: SelectionMode = SelectionMode.FILES
	private var listener: MetaFileChooserAdapter? = null
	private var fileTypeFilter: MetaFileTypeFilter? = null
	private var directory: FileHandle = defaultDirectory()
	private var selected: FileHandle? = null
	private val pathLabel = MetaLabel("", MetaType.CAPTION, MetaColor.TEXT_MUTED)
	private val entries = MetaTable()
	private val nameField = MetaTextField("", MetaType.BODY, placeholder = "File name")

	init {
		setDefaultSize(DEFAULT_WIDTH, DEFAULT_HEIGHT)
		contentTable.defaults().growX()
		val top = MetaTable().apply {
			add(MetaImageButton("ri-arrow-up-line", 16).onClick { navigate(directory.parent()) }).size(30f)
			add(pathLabel).left().growX().padLeft(MetaSpacing.SM)
		}
		contentTable.add(top).padBottom(MetaSpacing.SM).row()
		contentTable.add(MetaScrollPane(entries)).grow().row()
		if (mode == Mode.SAVE) contentTable.add(nameField).height(34f).padTop(MetaSpacing.SM).row()
		val actions = MetaTable().apply {
			add(MetaTextButton("Cancel").onClick { listener?.canceled(); fadeOut() }).padRight(MetaSpacing.SM)
			add(MetaTextButton(if (mode == Mode.OPEN) "Open" else "Save").onClick { confirmSelection() })
		}
		contentTable.add(actions).right().padTop(MetaSpacing.SM)
		refreshEntries()
	}

	fun setDirectory(directory: FileHandle) {
		navigate(directory)
	}

	fun setFileTypeFilter(filter: MetaFileTypeFilter?) {
		fileTypeFilter = filter
		refreshEntries()
	}

	fun setListener(listener: MetaFileChooserAdapter?) {
		this.listener = listener
	}

	fun fadeIn(): MetaFileChooser {
		clearActions()
		color.a = 0f
		addAction(Actions.fadeIn(0.15f))
		return this
	}

	fun fadeOut(): MetaFileChooser {
		clearActions()
		addAction(Actions.sequence(Actions.fadeOut(0.12f), Actions.removeActor()))
		return this
	}

	override fun close() {
		listener?.canceled()
		fadeOut()
	}

	override fun setStage(stage: Stage?) {
		super.setStage(stage)
		if (stage != null) centerWindow()
	}

	private fun navigate(next: FileHandle) {
		if (!next.exists() || !next.isDirectory) return
		directory = next
		selected = null
		refreshEntries()
	}

	private fun refreshEntries() {
		pathLabel.setText(directory.path())
		entries.clearChildren()
		entries.top().left()
		val listed = directory.list().sortedWith(compareBy<FileHandle>({ !it.isDirectory }, { it.name().lowercase() }))
		for (entry in listed) {
			if (!entry.isDirectory && fileTypeFilter?.accepts(entry) == false) continue
			val row = MetaIconTextButton(entry.name(), if (entry.isDirectory) "ri-folder-line" else "ri-file-line",
				size = MetaType.BODY, iconSize = 18).apply { left() }
			row.addListener(object : ClickListener() {
				override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent, x: Float, y: Float) {
					if (entry.isDirectory && tapCount >= 2) {
						navigate(entry)
						return
					}
					selected = entry
					if (!entry.isDirectory) nameField.setText(entry.name())
				}
			})
			entries.add(row).growX().height(34f).row()
		}
		entries.invalidateHierarchy()
	}

	private fun confirmSelection() {
		val choice = when {
			selectionMode == SelectionMode.DIRECTORIES -> selected?.takeIf { it.isDirectory } ?: directory
			mode == Mode.SAVE && nameField.text.isNotBlank() -> directory.child(nameField.text.trim())
			else -> selected?.takeIf { !it.isDirectory }
		} ?: return
		listener?.selected(Array.with(choice))
		fadeOut()
	}

	companion object {
		val OPEN = Mode.OPEN
		val SAVE = Mode.SAVE
		val SELECT_FILES = SelectionMode.FILES
		val SELECT_DIRECTORIES = SelectionMode.DIRECTORIES
		private var defaultPrefsName = "de.fatox.meta"

		fun setDefaultPrefsName(name: String) {
			defaultPrefsName = name
		}

		private fun defaultDirectory(): FileHandle = Gdx.files.absolute(System.getProperty("user.home", "."))
		private const val DEFAULT_WIDTH = 720f
		private const val DEFAULT_HEIGHT = 520f
	}
}

open class MetaFileChooserAdapter {
	open fun selected(file: Array<FileHandle>) = Unit
	open fun canceled() = Unit
}

class MetaFileTypeFilter(private val allTypesAllowed: Boolean) {
	private val extensions = com.badlogic.gdx.utils.ObjectSet<String>()

	fun addRule(description: String, vararg extensions: String) {
		for (extension in extensions) this.extensions.add(extension.lowercase().trimStart('.'))
	}

	internal fun accepts(file: FileHandle): Boolean = allTypesAllowed || extensions.size == 0 ||
		extensions.contains(file.extension().lowercase())
}
