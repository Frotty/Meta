package de.fatox.meta.api

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.utils.Drawable

interface AssetProvider {
	/** Loads all assets from XPK archives in the given folder. */
    fun loadAssetsFromFolder(folder: FileHandle): Boolean

	/** Loads some asset. Loading happens async. Use #get after loading has finished. */
    fun <T> load(name: String, type: Class<T>)

	/** Returns an instance of the loaded asset. Index is the libgdx packed frame index.*/
    operator fun <T> get(fileName: String, type: Class<T>, index: Int): T?

	/** Returns an instance of the loaded asset. */
    operator fun <T> get(fileName: String, type: Class<T>): T?

    operator fun get(fileName: String): FileHandle?

    fun getDrawable(name: String): Drawable

	/** Blocks the thread until all load tasks are finished. */
    fun finish()
}
