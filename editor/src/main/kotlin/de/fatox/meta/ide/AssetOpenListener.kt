package de.fatox.meta.ide

import com.badlogic.gdx.files.FileHandle

/**
 * Created by Frotty on 18.03.2017.
 */
fun interface AssetOpenListener {
	fun onOpen(fileHandle: FileHandle)
}