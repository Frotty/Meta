package de.fatox.meta.api

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.utils.Drawable

interface AssetProvider {
    fun addAssetFolder(folder: FileHandle): Boolean

    fun <T> load(name: String, type: Class<T>)

    operator fun <T> get(fileName: String, type: Class<T>, index: Int): T

    operator fun <T> get(fileName: String, type: Class<T>): T

    operator fun get(fileName: String): FileHandle

    fun getDrawable(name: String): Drawable

    fun finish()
}
