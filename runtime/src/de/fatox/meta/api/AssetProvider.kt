package de.fatox.meta.api

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Array

interface AssetProvider {
	/** Loads all assets from XPK archives in the given folder. */
	fun loadPackedAssetsFromFolder(folder: FileHandle): Boolean

	/** Loads all assets from the given folder. */
	fun loadRawAssetsFromFolder(folder: FileHandle): Boolean

	/** Loads some asset. Loading happens async. Use #get after loading has finished. */
	fun <T> load(name: String, type: Class<T>)

	/** Returns an instance of the loaded asset. Index is the libgdx packed frame index.*/
	fun <T> getResource(fileName: String, type: Class<T>, index: Int = -1): T?

	// For java interop
	fun <T> getResource(fileName: String, type: Class<T>): T? = getResource(fileName, type, -1)

	fun getDrawable(name: String): Drawable

	/** Blocks the thread until all load tasks are finished. */
	fun finish()

	/**
	 * Returns a cached list of TextureRegions that represent the animation of the given texture
	 *
	 * @param baseName name of the texture
	 * @param frames   limit frames of animations, all frames if not specified
	 * @return
	 */
	fun loadAnimationFrames(baseName: String, frames: Int = -1): Array<out TextureRegion>
}

inline fun <reified T : Any> AssetProvider.load(name: String) {
	load(name, T::class.java)
}

inline operator fun <reified T : Any> AssetProvider.get(fileName: String, index: Int = -1): T? =
	getResource(fileName, T::class.java, index)