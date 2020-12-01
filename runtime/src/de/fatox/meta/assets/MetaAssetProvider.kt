package de.fatox.meta.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.assets.loaders.ModelLoader.ModelParameters
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ObjectMap
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.ResourceKey
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.assets.XPKLoader.getList

private val log = MetaLoggerFactory.logger {}
private val defaultTexParam: TextureParameter = TextureParameter().apply {
	genMipMaps = true
	minFilter = Texture.TextureFilter.MipMapLinearLinear
}
private val defaultModelParam: ModelParameters = ModelParameters().apply { textureParameter = defaultTexParam }

class MetaAssetProvider : AssetProvider {
	private val assetManager = AssetManager(MetaFileHandleResolver())
	private val atlasCache = Array<TextureAtlas>()
	private val animCache = IntMap<Array<out TextureRegion>>()
	private val fileCache = ObjectMap<String, FileHandle>()

	override fun loadPackedAssetsFromFolder(folder: FileHandle): Boolean {
		if (folder.isDirectory) {
			for (itrHandle in folder.list()) {
				if (itrHandle.extension().equals(XPKLoader.EXTENSION, ignoreCase = true)) {
					val list = getList(itrHandle)
					list.forEach {
						fileCache.put(it.name(), it)
						fileCache.put(it.name().replace("/", "\\"), it)
						log.debug { "cache name: <${it.name()}>" }
					}
				}
			}
			return true
		}
		return false
	}

	override fun loadRawAssetsFromFolder(folder: FileHandle): Boolean {
		if (folder.isDirectory) {
			for (itrHandle in folder.list()) {
				if (itrHandle.isDirectory) {
					loadRawAssetsFromFolder(itrHandle)
				} else {
					fileCache.put(itrHandle.name(), itrHandle)
					fileCache.put(itrHandle.name().replace("/", "\\"), itrHandle)
				}
			}
			return true
		}
		return false
	}

	override fun <T: Any> load(name: String, type: Class<T>) {
		log.debug { "loading <$name>" }
		if (fileCache.containsKey(name)) {
			log.debug { "pack cache contains filename" }
			loadIntern(AssetDescriptor(fileCache[name], type))
		} else {
			loadIntern(AssetDescriptor(name, type))
		}
	}

	private fun <T: Any> loadIntern(descriptor: AssetDescriptor<T>) {
		when {
			descriptor.type == Model::class.java ->
				assetManager.load(descriptor.fileName, Model::class.java, defaultModelParam)
			descriptor.type == Texture::class.java && !descriptor.fileName.contains("ui") -> {
				log.debug { "ui load" }
				assetManager.load(descriptor.fileName, Texture::class.java, defaultTexParam)
			}
			else -> {
				log.debug { "normal load" }
				assetManager.load(descriptor)
			}
		}
		assetManager.finishLoading()
		if (descriptor.type == Model::class.java) {
			val model = assetManager.get(descriptor.fileName, Model::class.java)
			val attribute = model.materials.first()[TextureAttribute.Diffuse] as TextureAttribute
			attribute.textureDescription.texture.bind()
			Gdx.gl30.glTexParameterf(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAX_ANISOTROPY_EXT, 16f)
		}
		if (descriptor.type == Texture::class.java) {
			val texture = assetManager.get(descriptor.fileName, Texture::class.java)
			texture.bind()
			Gdx.gl30.glTexParameterf(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAX_ANISOTROPY_EXT, 16f)
		}
		if (descriptor.type == TextureAtlas::class.java) {
			atlasCache.add(assetManager.get(descriptor.fileName, TextureAtlas::class.java))
		}
	}

	override fun <T: Any> getResource(fileName: String, type: Class<T>, index: Int): T {
		return when {
			type == FileHandle::class.java -> {
				if (fileCache.containsKey(fileName))
					fileCache[fileName] as T
				else
					Gdx.files.internal(fileName) as T
			}
			assetManager.isLoaded(fileName, type) -> assetManager[fileName, type]
			type == TextureRegion::class.java -> {
				atlasCache.asSequence().map {
					if (index <= 0) it.findRegion(fileName) else it.findRegion(fileName, index)
				}.firstOrNull() as T? ?: getResource(fileName, Texture::class.java).let { TextureRegion(it) } as T?
			}
			fileCache.containsKey(fileName) -> {
				loadIntern(AssetDescriptor(fileCache[fileName], type))
				getResource(fileName, type)
			}
			else -> {
				load(fileName, type)
				getResource(fileName, type)
			}
		} ?: throw GdxRuntimeException("Resource not found: $fileName")
	}

	override fun getDrawable(name: String): Drawable {
		return TextureRegionDrawable(getResource(name, TextureRegion::class.java))
	}

	override fun finish() {
		assetManager.finishLoading()
	}

	override fun loadAnimationFrames(baseName: String, frames: Int): Array<out TextureRegion> {
		val key = 31 * baseName.hashCode() + frames
		if (!animCache.containsKey(key)) {
			// Since we use asSequence() map is lazily evaluated, thus only calling it when necessary.
			val regions: Array<AtlasRegion> = atlasCache.asSequence().map { it.findRegions(baseName) }.first()

			if (regions.size > 0) {
				if (frames > -1) regions.setSize(frames) // limit to the request number of frames
				animCache.put(key, regions)
			} else
				throw GdxRuntimeException("couldn't load $baseName")
		}
		return animCache[key]
	}

	internal inner class MetaFileHandleResolver : FileHandleResolver {
		override fun resolve(fileName: String): FileHandle {
			return fileCache[fileName] ?: Gdx.files.internal(fileName)
		}
	}
}