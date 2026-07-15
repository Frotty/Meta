package de.fatox.meta.api

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Array

interface AssetProvider {
	/** Indexes assets from XPK archives in the given folder so they can subsequently be queued or retrieved by name. */
	fun loadPackedAssetsFromFolder(folder: FileHandle): Boolean

	/** Indexes raw assets in the given folder so they can subsequently be queued or retrieved by name. */
	fun loadRawAssetsFromFolder(folder: FileHandle): Boolean

	/** Queues an asset for asynchronous loading. Advance it with [update], then retrieve it with [getResource]. */
	fun <T : Any> load(name: String, type: Class<T>)

	/** Queues an asset for asynchronous loading. Advance it with [update], then retrieve it with [getResource]. */
	fun <T : Any> load(key: AssetKey<T>, type: Class<T>): Unit = load(key.name, type)

	/**
	 * Advances queued asynchronous loads for at most [millis] milliseconds. Call from the GL/render thread.
	 * Returns true when the queue is empty. The default keeps custom providers source-compatible.
	 */
	fun update(millis: Int = 16): Boolean = true

	/** Progress of the current loading queue in the range 0..1. */
	val progress: Float get() = 1f

	/** Returns an instance of the loaded asset. Index is the libgdx packed frame index.*/
	fun <T : Any> getResource(fileName: String, type: Class<T>, index: Int = -1): T

	/** Returns an instance of the loaded asset. Index is the libgdx packed frame index.*/
	fun <T : Any> getResource(key: AssetKey<T>, type: Class<T>, index: Int = -1): T =
		getResource(key.name, type, index)

	fun getDrawable(name: String): Drawable
	fun getDrawable(key: AssetKey<TextureRegion>): Drawable = getDrawable(key.name)

	/** Blocks the thread until all load tasks are finished. */
	fun finish()

	/** Releases loaded assets and loader worker threads. */
	fun dispose(): Unit = Unit

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
