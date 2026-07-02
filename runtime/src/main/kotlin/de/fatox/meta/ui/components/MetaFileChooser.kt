package de.fatox.meta.ui.components

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.file.FileChooser
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter
import com.kotcrab.vis.ui.widget.file.FileTypeFilter

class MetaFileChooser(mode: FileChooser.Mode) : FileChooser(mode) {
	companion object {
		val OPEN: FileChooser.Mode = FileChooser.Mode.OPEN
		val SAVE: FileChooser.Mode = FileChooser.Mode.SAVE
		val SELECT_FILES: FileChooser.SelectionMode = FileChooser.SelectionMode.FILES
		val SELECT_DIRECTORIES: FileChooser.SelectionMode = FileChooser.SelectionMode.DIRECTORIES

		fun setDefaultPrefsName(name: String) {
			FileChooser.setDefaultPrefsName(name)
		}
	}
}

open class MetaFileChooserAdapter : FileChooserAdapter() {
	override fun selected(file: Array<FileHandle>) = Unit
}

class MetaFileTypeFilter(allTypesAllowed: Boolean) : FileTypeFilter(allTypesAllowed)
