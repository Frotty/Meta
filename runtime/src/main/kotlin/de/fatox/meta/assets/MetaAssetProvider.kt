package de.fatox.meta.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.assets.loaders.ModelLoader.ModelParameters
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.TimeUtils
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.api.extensions.trace
import de.fatox.meta.api.extensions.warn
import de.fatox.meta.assets.XPKLoader.getList

private val log = MetaLoggerFactory.logger {}
private val defaultTexParam: TextureParameter = TextureParameter().apply {
	genMipMaps = true
	minFilter = Texture.TextureFilter.MipMapLinearLinear
}
private val defaultModelParam: ModelParameters = ModelParameters().apply { textureParameter = defaultTexParam }

class MetaAssetProvider : AssetProvider {
	/**
	 * Maximum supported anisotropy, queried once on first use (GL thread). 0 when the
	 * GL_EXT_texture_filter_anisotropic extension is unavailable.
	 */
	private val maxAnisotropy: Float by lazy {
		if (Gdx.graphics.supportsExtension("GL_EXT_texture_filter_anisotropic")) {
			val buffer = BufferUtils.newFloatBuffer(16)
			Gdx.gl.glGetFloatv(GL20.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, buffer)
			buffer.get(0)
		} else {
			0f
		}
	}

	/** Applies anisotropic filtering to the currently bound texture, if supported. */
	private fun applyAnisotropy() {
		if (maxAnisotropy > 0f) {
			Gdx.gl.glTexParameterf(
				GL20.GL_TEXTURE_2D,
				GL20.GL_TEXTURE_MAX_ANISOTROPY_EXT,
				minOf(16f, maxAnisotropy)
			)
		}
	}

	private val atlasCache = Array<TextureAtlas>()
	private val animCache = IntMap<Array<out TextureRegion>>()
	private val fileCache = ObjectMap<String, FileHandle>()
	private val fileOrigins = ObjectMap<String, String>()
	private val pendingFinalization = Array<AssetDescriptor<*>>()
	private val stagedTextureUploads = StagedTextureUploads()
	private val resolver = MetaFileHandleResolver()
	private val assetManager = AssetManager(resolver).apply {
		setLoader(Texture::class.java, MetaTextureLoader(resolver, stagedTextureUploads))
		setLoader(TextureAtlas::class.java, MetaTextureAtlasLoader(resolver))
	}
	private var finalizationCursor = 0

	override val progress: Float get() = assetManager.progress

	override fun loadPackedAssetsFromFolder(folder: FileHandle): Boolean {
		if (folder.isDirectory) {
			val children = folder.list()
			for (childIndex in children.indices) {
				val itrHandle = children[childIndex]
				if (itrHandle.extension().equals(XPKLoader.EXTENSION, ignoreCase = true)) {
					val list = getList(itrHandle)
					for (index in 0 until list.size) {
						val file = list[index]
						cacheFile(file.name(), file, itrHandle.path())
					}
					log.debug { "Indexed ${list.size} assets from <${itrHandle.name()}>" }
				}
			}
			return true
		}
		return false
	}

	override fun loadRawAssetsFromFolder(folder: FileHandle): Boolean {
		var filesSinceYield = 0
		// This helper function does all the recursion,
		// always stripping out `rootFolderName` from the path.
		fun loadFolderRecursively(currentFolder: FileHandle, rootFolderName: String) {
			// List everything in currentFolder
			val children = currentFolder.list()
			for (childIndex in children.indices) {
				val child = children[childIndex]
				if (child.isDirectory) {
					// Recurse into subdirectories
					loadFolderRecursively(child, rootFolderName)
				} else {
					// Build the full path (relative to internal root, but includes folder name)
					val fullPath = child.path()  // e.g. "assets/subfolder/img.png"

					// Remove the top folder name + "/" from the front (e.g. remove "assets/" -> "subfolder/img.png")
					val relativePath = if (fullPath.startsWith("$rootFolderName/")) {
						fullPath.substring(rootFolderName.length + 1)
					} else {
						fullPath
					}

					// Store one portable lookup key; keep the actual source path on the handle.
					cacheFile(relativePath, child, folder.path())
					if (++filesSinceYield >= FILES_PER_YIELD) {
						filesSinceYield = 0
						Thread.yield()
					}
				}
			}
		}
		// If the given handle is not a folder, do nothing
		if (!folder.isDirectory) return false

		// Kick off recursion, remembering the name of the top-level folder (e.g. "assets")
		loadFolderRecursively(folder, folder.path())

		return true
	}

	override fun <T: Any> load(name: String, type: Class<T>) {
		log.trace { "queueing <$name>" }
		val cachedFile = fileCache[assetPathKey(name)]
		if (cachedFile != null) {
			log.trace { "pack cache contains filename" }
			queueIntern(AssetDescriptor(cachedFile, type))
		} else {
			queueIntern(AssetDescriptor(name, type))
		}
	}

	private fun <T: Any> queueIntern(descriptor: AssetDescriptor<T>) {
		if (assetManager.contains(descriptor.fileName)) return
		when {
			descriptor.type == Model::class.java ->
				assetManager.load(descriptor.fileName, Model::class.java, defaultModelParam)
			descriptor.type == Texture::class.java && !descriptor.fileName.contains("ui") -> {
				log.trace { "non-ui texture load (mipmapped)" }
				assetManager.load(descriptor.fileName, Texture::class.java, defaultTexParam)
			}
			else -> {
				log.trace { "normal load" }
				assetManager.load(descriptor)
			}
		}
		pendingFinalization.add(descriptor)
	}

	private fun finalizeLoadedAssets(maxChecks: Int) {
		var checks = 0
		while (pendingFinalization.size > 0 && checks < maxChecks) {
			if (finalizationCursor >= pendingFinalization.size) finalizationCursor = 0
			val descriptor = pendingFinalization[finalizationCursor]
			checks++
			if (!assetManager.isLoaded(descriptor.fileName)) {
				finalizationCursor++
				continue
			}
			finalizeLoadedAsset(descriptor)
			pendingFinalization.removeIndex(finalizationCursor)
		}
	}

	private fun finalizeLoadedAsset(fileName: String) {
		for (index in 0 until pendingFinalization.size) {
			val descriptor = pendingFinalization[index]
			if (descriptor.fileName != fileName) continue
			finalizeLoadedAsset(descriptor)
			pendingFinalization.removeIndex(index)
			if (finalizationCursor > index) finalizationCursor--
			return
		}
	}

	private fun finalizeLoadedAsset(descriptor: AssetDescriptor<*>) {
		if (descriptor.type == Model::class.java) {
			val model = assetManager.get(descriptor.fileName, Model::class.java)
			val attribute = model.materials.first()[TextureAttribute.Diffuse] as TextureAttribute
			attribute.textureDescription.texture.bind()
			applyAnisotropy()
		}
		if (descriptor.type == Texture::class.java) {
			val texture = assetManager.get(descriptor.fileName, Texture::class.java)
			texture.bind()
			applyAnisotropy()
		}
		if (descriptor.type == TextureAtlas::class.java) {
			atlasCache.add(assetManager.get(descriptor.fileName, TextureAtlas::class.java))
		}
	}

	override fun update(millis: Int): Boolean {
		if (millis <= 0) {
			return assetManager.queuedAssets == 0 &&
				pendingFinalization.size == 0 &&
				stagedTextureUploads.isEmpty
		}

		if (!stagedTextureUploads.isEmpty) {
			val startedAt = TimeUtils.nanoTime()
			stagedTextureUploads.update()
			warnIfSlowStep("Staged texture upload", millis, startedAt)
			return false
		}

		// AssetManager.update(millis) always executes at least one task and may execute many more before checking its
		// soft deadline. One task can itself contain an unbounded texture upload. Advancing exactly one task gives the
		// splash scheduler a predictable recovery frame between expensive GL operations.
		val startedAt = TimeUtils.nanoTime()
		val complete = assetManager.update()
		finalizeLoadedAssets(MAX_FINALIZATIONS_PER_UPDATE)
		warnIfSlowStep("Asset loading step", millis, startedAt)
		if (!complete) Thread.yield()
		return complete && pendingFinalization.size == 0 && stagedTextureUploads.isEmpty
	}

	private fun warnIfSlowStep(label: String, requestedMillis: Int, startedAt: Long) {
		val elapsedMillis = (TimeUtils.nanoTime() - startedAt) / NANOS_PER_MILLI
		if (elapsedMillis < SLOW_UPDATE_WARNING_MS) return
		log.warn {
			"$label took ${elapsedMillis}ms (requested budget ${requestedMillis}ms, " +
				"${assetManager.queuedAssets} queued, ${stagedTextureUploads.size} texture uploads). " +
				assetManager.diagnostics
		}
	}

	override fun <T : Any> getResource(fileName: String, type: Class<T>, index: Int): T {
		val cachedFile = fileCache[assetPathKey(fileName)]
		return when {
			type == FileHandle::class.java -> {
				type.cast(cachedFile ?: Gdx.files.internal(fileName))
			}
			assetManager.isLoaded(fileName, type) -> assetManager[fileName, type]
			cachedFile != null && assetManager.isLoaded(cachedFile.path(), type) -> assetManager[cachedFile.path(), type]
			type == TextureRegion::class.java -> {
				var region: TextureRegion? = null
				for (atlasIndex in 0 until atlasCache.size) {
					region = if (index <= 0) {
						atlasCache[atlasIndex].findRegion(fileName)
					} else {
						atlasCache[atlasIndex].findRegion(fileName, index)
					}
					if (region != null) break
				}
				type.cast(region ?: TextureRegion(getResource(fileName, Texture::class.java)))
			}
			cachedFile != null -> {
				load(fileName, type)
				val resolvedName = cachedFile.path()
				assetManager.finishLoadingAsset<Any>(resolvedName)
				stagedTextureUploads.finish()
				finalizeLoadedAsset(resolvedName)
				getResource(fileName, type)
			}
			else -> {
				load(fileName, type)
				assetManager.finishLoadingAsset<Any>(fileName)
				stagedTextureUploads.finish()
				finalizeLoadedAsset(fileName)
				getResource(fileName, type)
			}
		} ?: throw GdxRuntimeException("Resource not found: $fileName")
	}

	override fun getDrawable(name: String): Drawable {
		return TextureRegionDrawable(getResource(name, TextureRegion::class.java))
	}

	override fun finish() {
		assetManager.finishLoading()
		stagedTextureUploads.finish()
		finalizeLoadedAssets(Int.MAX_VALUE)
	}

	override fun dispose() {
		stagedTextureUploads.dispose()
		pendingFinalization.clear()
		atlasCache.clear()
		animCache.clear()
		fileCache.clear()
		fileOrigins.clear()
		assetManager.dispose()
	}

	override fun loadAnimationFrames(baseName: String, frames: Int): Array<out TextureRegion> {
		val key = 31 * baseName.hashCode() + frames
		if (!animCache.containsKey(key)) {
			var regions: Array<AtlasRegion>? = null
			for (atlasIndex in 0 until atlasCache.size) {
				val candidate = atlasCache[atlasIndex].findRegions(baseName)
				if (candidate.size > 0) {
					regions = candidate
					break
				}
			}

			if (regions != null) {
				if (frames > -1) regions.setSize(frames) // limit to the request number of frames
				animCache.put(key, regions)
			} else
				throw GdxRuntimeException("couldn't load $baseName")
		}
		return animCache[key]
	}

	internal inner class MetaFileHandleResolver : FileHandleResolver {
		override fun resolve(fileName: String): FileHandle {
			return fileCache[assetPathKey(fileName)] ?: Gdx.files.internal(fileName)
		}
	}

	private fun cacheFile(assetPath: String, file: FileHandle, origin: String) {
		val key = assetPathKey(assetPath)
		val existing = fileCache[key]
		val existingOrigin = fileOrigins[key]
		if (existing != null && (existing.path() != file.path() || existingOrigin != origin)) {
			throw GdxRuntimeException(
				"Case-insensitive asset path collision for '$assetPath': '" +
					existing.path() + "' from '" + existingOrigin + "' and '" + file.path() + "' from '" + origin + "'",
			)
		}
		fileCache.put(key, file)
		fileOrigins.put(key, origin)
	}

	private companion object {
		const val MAX_FINALIZATIONS_PER_UPDATE = 1
		const val FILES_PER_YIELD = 64
		const val NANOS_PER_MILLI = 1_000_000L
		const val SLOW_UPDATE_WARNING_MS = 8L
	}
}
