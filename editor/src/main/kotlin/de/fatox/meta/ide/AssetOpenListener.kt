package de.fatox.meta.ide

import com.badlogic.gdx.files.FileHandle

fun interface AssetOpenListener {
	fun onOpen(fileHandle: FileHandle)
}