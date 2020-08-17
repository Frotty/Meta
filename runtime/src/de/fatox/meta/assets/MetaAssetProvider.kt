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
import de.fatox.meta.Meta
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.assets.XPKLoader.getList
import org.slf4j.LoggerFactory
import java.util.function.Consumer

class MetaAssetProvider : AssetProvider {
	internal inner class MetaFileHandleResolver : FileHandleResolver {
		override fun resolve(fileName: String): FileHandle {
			return if (fileCache.containsKey(fileName)) fileCache[fileName] else Gdx.files.internal(fileName)
		}
	}

	private val assetManager = AssetManager(MetaFileHandleResolver())
	private val atlasCache = Array<TextureAtlas>()
	private val animCache = IntMap<Array<out TextureRegion>>()
	private val fileCache = ObjectMap<String, FileHandle>()

	companion object {
		private val log = LoggerFactory.getLogger(MetaAssetProvider::class.java)
		private val defaultTexParam = TextureParameter()
		private val defaultModelParam = ModelParameters()

		init {
			defaultTexParam.genMipMaps = true
			defaultTexParam.minFilter = Texture.TextureFilter.MipMapLinearLinear
			defaultModelParam.textureParameter = defaultTexParam
		}
	}

	override fun loadPackedAssetsFromFolder(folder: FileHandle): Boolean {
		if (folder.isDirectory) {
			for (itrHandle in folder.list()) {
				if (itrHandle.extension().equals(XPKLoader.EXTENSION, ignoreCase = true)) {
					val list = getList(itrHandle)
					list.forEach(Consumer { it: XPKFileHandle ->
						fileCache.put(it.name(), it)
						fileCache.put(it.name().replace("/", "\\"), it)
						log.debug("cache name: <" + it.name() + ">")
					})
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

	override fun <T> load(name: String, type: Class<T>) {
		log.debug("loading <$name>")
		if (fileCache.containsKey(name)) {
			log.debug("pack cache contains filename")
			loadIntern(AssetDescriptor(fileCache[name], type))
		} else {
			loadIntern(AssetDescriptor(name, type))
		}
	}

	private fun <T> loadIntern(descr: AssetDescriptor<T>) {
		if (descr.type == Model::class.java) {
			assetManager.load(descr.fileName, Model::class.java, defaultModelParam)
		} else if (descr.type == Texture::class.java && !descr.fileName.contains("ui")) {
			log.debug("ui load")
			assetManager.load(descr.fileName, Texture::class.java, defaultTexParam)
		} else {
			log.debug("normal load")
			assetManager.load(descr)
		}
		assetManager.finishLoading()
		if (descr.type == Model::class.java) {
			val model = assetManager.get(descr.fileName, Model::class.java)
			val attribute = model.materials.first()[TextureAttribute.Diffuse] as TextureAttribute
			attribute.textureDescription.texture.bind()
			Gdx.gl30.glTexParameterf(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAX_ANISOTROPY_EXT, 16f)
		}
		if (descr.type == Texture::class.java) {
			val texture = assetManager.get(descr.fileName, Texture::class.java)
			texture.bind()
			Gdx.gl30.glTexParameterf(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAX_ANISOTROPY_EXT, 16f)
		}
		if (descr.type == TextureAtlas::class.java) {
			atlasCache.add(assetManager.get(descr.fileName, TextureAtlas::class.java))
		}
	}

	override fun <T> get(fileName: String, type: Class<T>, index: Int): T? {
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
				}.firstOrNull() as T? ?: get(fileName, Texture::class.java)?.let { TextureRegion(it) } as T?
			}
			fileCache.containsKey(fileName) -> {
				loadIntern(AssetDescriptor(fileCache[fileName], type))
				get(fileName, type)
			}
			else -> {
				load(fileName, type)
				get(fileName, type)
			}
		}
	}

	override fun <T> get(fileName: String, type: Class<T>): T? {
		return get(fileName, type, -1)
	}

	override fun get(fileName: String): FileHandle? {
		return null
	}

	override fun getDrawable(name: String): Drawable {
		return TextureRegionDrawable(get(name, TextureRegion::class.java))
	}

	override fun finish() {
		assetManager.finishLoading()
	}

	/**
	 * Returns a cached list of TextureRegions that represent the animation of the given texture
	 *
	 * @param baseName name of the texture
	 * @param frames   limit frames of animations, all frames if not specified
	 * @return
	 */
	fun loadAnimationFrames(baseName: String, frames: Int = -1): Array<out TextureRegion> {
		val key = baseName.hashCode() + 31 * frames
		if (!animCache.containsKey(key)) {
			// Since we use asSequence() map is lazily evaluated, thus only calling it when necessary.
			val regions: Array<AtlasRegion> = atlasCache.asSequence().map { it.findRegions(baseName) }.first()

			if (regions.size > 0) {
				if (frames > -1) regions.setSize(frames) // limit to the request number of frames
				animCache.put(key, regions)
			} else
				throw GdxRuntimeException("couldn't load " + baseName)
		}
		return animCache[key]
	}
}