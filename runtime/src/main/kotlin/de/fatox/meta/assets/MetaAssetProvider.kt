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
import de.fatox.meta.assets.xpk.XpkFormat
import de.fatox.meta.assets.xpk.XpkProfile

import de.fatox.meta.assets.xpk.XpkV2Archive
import de.fatox.meta.injection.MetaInject

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
	private val openArchives = Array<XpkArchive>()
	private val v2Archives = Array<XpkV2Archive>()

	/**
	 * The game supplies the parameters an XPK v2 archive is built and read with; Meta binds none.
	 *
	 * Absent, v2 archives are simply not recognised - which is the point: this repository is public, so the format
	 * lives here and the constants that open any particular game's archives do not. See [XpkProfile].
	 */
	private val xpkProfile: XpkProfile? = MetaInject.injectOrNull()
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
				if (!itrHandle.extension().equals(XPKLoader.EXTENSION, ignoreCase = true)) continue

				val v2 = xpkProfile?.let { XpkV2Archive.openOrNull(it, itrHandle) }
				if (v2 != null) {
					// A v2 archive stores keyed name hashes, not paths, so there is nothing to enumerate into
					// fileCache. It answers lookups instead - see resolvePacked.
					v2Archives.add(v2)
					log.debug { "Registered ${v2.entryCount} packed assets from <${itrHandle.name()}>" }
					continue
				}

				// The provider owns the archive. Buffers are kept while a load phase is running, and a read
				// outside one - a lazily constructed sound, say - cleans up after itself, because nothing pumps
				// update() once the splash has finished.
				val archive = XPKLoader.open(itrHandle, retainAfterRead = ::isLoadPhaseActive)
				openArchives.add(archive)
				val list = archive.entries
				for (index in 0 until list.size) {
					val file = list[index]
					// path(), not name(): name() is the last path element, and packed assets must be keyed by
					// the same archive-relative path that loadRawAssetsFromFolder uses for loose files.
					cacheFile(file.path(), file, itrHandle.path())
				}
				log.debug { "Indexed ${list.size} assets from <${itrHandle.name()}>" }
			}
			return true
		}
		return false
	}

	/**
	 * The one place a name is turned into a handle, across every source the provider knows.
	 *
	 * Written as one function rather than an `?:` at each call site on purpose. A short-circuit would let a loose
	 * file or a v1 entry quietly shadow a v2 one, which is the same overlap `cacheFile` already refuses between
	 * loose files and v1 archives - and being silent about it in one case and loud in the other is how a stale pack
	 * ends up serving different bytes depending on which lookup path reached it.
	 */
	private fun resolveAsset(fileName: String): FileHandle? {
		val indexed = fileCache[assetPathKey(fileName)]
		val packed = resolvePacked(fileName)
		if (indexed != null && packed != null) {
			throw GdxRuntimeException(
				"Asset '$fileName' is present both as an indexed file (${indexed.path()}) and in a packed archive",
			)
		}
		return indexed ?: packed
	}

	/**
	 * Resolves a name against the registered v2 archives.
	 *
	 * v2 keeps keyed hashes rather than paths, so it cannot be indexed up front the way v1 and loose files are; the
	 * archive is asked instead. Lookup is a binary search over a sorted `long[]`, so this costs one hash and a few
	 * comparisons per archive.
	 */
	private fun resolvePacked(fileName: String): FileHandle? {
		val profile = xpkProfile ?: return null
		if (v2Archives.size == 0) return null

		// Hashed once, not once per archive: every archive under this profile hashes a name the same way.
		val canonical = assetPathKey(fileName)
		if (canonical.isEmpty()) return null
		val nameHash = XpkFormat.nameHash(profile, canonical)

		var found: FileHandle? = null
		for (index in 0 until v2Archives.size) {
			val candidate = v2Archives[index].findByNameHash(nameHash, canonical) ?: continue
			// Every archive is searched, not just up to the first hit. Returning the first would make registration
			// order decide which bytes an asset resolves to, and a stale or split pack would then serve silently
			// wrong content. Loose files and v1 entries already fail loudly on a collision, via cacheFile.
			if (found != null) {
				throw GdxRuntimeException("Asset '$fileName' is present in more than one packed archive")
			}
			found = candidate
		}
		return found
	}

	override fun loadRawAssetsFromFolder(folder: FileHandle): Boolean {
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
		val cachedFile = resolveAsset(name)
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
			// The zero-budget poll can be the call that observes completion: SplashScreen's loading budget drops to 0
			// after a slow frame, and its return value is what advances the phase. Releasing only on the budgeted path
			// would let the splash move on with entry buffers and the open 7z decoder still retained.
			val polled = assetManager.queuedAssets == 0 &&
				pendingFinalization.size == 0 &&
				stagedTextureUploads.isEmpty
			if (polled) releaseArchiveCaches()
			return polled
		}

		val startedAt = TimeUtils.nanoTime()
		do {
			val complete = if (!stagedTextureUploads.isEmpty) {
				stagedTextureUploads.update(StagedTextureUploadPolicy.bytesForBudget(millis))
				assetManager.queuedAssets == 0
			} else {
				assetManager.update()
			}
			finalizeLoadedAssets(MAX_FINALIZATIONS_PER_STEP)

			val drained = complete && pendingFinalization.size == 0 && stagedTextureUploads.isEmpty
			if (drained) {
				releaseArchiveCaches()
				warnIfSlowStep("Asset loading update", millis, startedAt)
				return true
			}
		} while (AssetUpdateBudget.hasTimeRemaining(startedAt, TimeUtils.nanoTime(), millis))

		warnIfSlowStep("Asset loading update", millis, startedAt)
		return false
	}

	/**
	 * Frees archive entry buffers once a load phase has drained.
	 *
	 * Decompressed entry bytes are only needed while a loader is turning them into a texture, sound or model. Held
	 * past that they are a second full copy of the asset data in heap, alongside the GPU and OpenAL copies. Also
	 * closes each archive's 7z reader, releasing its LZMA2 dictionary.
	 */
	/**
	 * Whether asset loading is in flight, so archive reads are part of a burst worth caching for.
	 *
	 * Read from AssetManager's worker as well as the GL thread. Both fields are plain int reads and this is only a
	 * retention hint - a stale answer costs one extra sweep or one late release, never wrong bytes - so it is
	 * deliberately unsynchronised rather than adding a lock to the read path.
	 */
	private fun isLoadPhaseActive(): Boolean =
		assetManager.queuedAssets > 0 || pendingFinalization.size > 0

	private fun releaseArchiveCaches() {
		for (index in 0 until openArchives.size) {
			val archive = openArchives[index]
			if (!archive.isFullyReleased) archive.releaseCachedEntries()
		}
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
		val cachedFile = resolveAsset(fileName)
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
		releaseArchiveCaches()
	}

	override fun dispose() {
		stagedTextureUploads.dispose()
		pendingFinalization.clear()
		atlasCache.clear()
		animCache.clear()
		fileCache.clear()
		fileOrigins.clear()
		// Releases each archive's open 7z reader (and its decoder dictionary) plus its cached entry buffers.
		for (index in 0 until openArchives.size) openArchives[index].dispose()
		openArchives.clear()
		for (index in 0 until v2Archives.size) v2Archives[index].dispose()
		v2Archives.clear()
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
			return resolveAsset(fileName) ?: Gdx.files.internal(fileName)
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
		const val MAX_FINALIZATIONS_PER_STEP = 1
		const val NANOS_PER_MILLI = 1_000_000L
		const val SLOW_UPDATE_WARNING_MS = 8L
	}
}

internal object AssetUpdateBudget {
	private const val NANOS_PER_MILLI = 1_000_000L

	fun hasTimeRemaining(startedAtNanos: Long, currentNanos: Long, millis: Int): Boolean =
		millis > 0 && currentNanos - startedAtNanos < millis.toLong() * NANOS_PER_MILLI
}
