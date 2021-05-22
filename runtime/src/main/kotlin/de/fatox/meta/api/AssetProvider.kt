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
	fun <T : Any> load(name: String, type: Class<T>)

	/** Loads some asset. Loading happens async. Use #get after loading has finished. */
	fun <T : Any> load(key: AssetKey<T>, type: Class<T>): Unit = load(key.name, type)

	/** Returns an instance of the loaded asset. Index is the libgdx packed frame index.*/
	fun <T : Any> getResource(fileName: String, type: Class<T>, index: Int = -1): T

	/** Returns an instance of the loaded asset. Index is the libgdx packed frame index.*/
	fun <T : Any> getResource(key: AssetKey<T>, type: Class<T>, index: Int = -1): T =
		getResource(key.name, type, index)

	fun getDrawable(name: String): Drawable
	fun getDrawable(key: AssetKey<TextureRegion>): Drawable = getDrawable(key.name)

	/** Blocks the thread until all load tasks are finished. */
	fun finish()

	/**
	 * @param baseName name of the texture
	 * @param frames   limit frames of animations, all frames if not specified
	 * @return Cached list of TextureRegions that represent the animation of the given texture.
	 */
	fun loadAnimationFrames(baseName: String, frames: Int = -1): Array<out TextureRegion>
}

inline fun <reified T : Any> AssetProvider.load(name: String): Unit = load(name, T::class.java)
inline fun <reified T : Any> AssetProvider.load(key: AssetKey<T>): Unit = load(key, T::class.java)

inline fun <reified T : Any> AssetProvider.getResource(fileName: String, index: Int = -1): T =
	getResource(fileName, T::class.java, index)

inline fun <reified T : Any> AssetProvider.getResource(key: AssetKey<T>, index: Int = -1): T =
	getResource(key, T::class.java, index)

inline operator fun <reified T : Any> AssetProvider.get(fileName: String, index: Int = -1): T =
	getResource(fileName, T::class.java, index)

inline operator fun <reified T : Any> AssetProvider.get(key: AssetKey<T>, index: Int = -1): T =
	getResource(key, T::class.java, index)

@JvmInline
value class AssetKey<T : Any>(val name: String)
