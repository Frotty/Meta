package de.fatox.meta.assets

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader.TextureAtlasParameter
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData
import com.badlogic.gdx.utils.Array

/**
 * Texture-atlas dependency discovery parses the atlas file and may decompress it from an XPK. libGDX's stock
 * TextureAtlasLoader is synchronous, so that work happens inside AssetManager.update on the render thread.
 *
 * Declaring the same loader protocol as asynchronous moves dependency discovery onto AssetManager's worker. Texture
 * uploads remain on the GL thread through the ordinary Texture dependencies; the final atlas assembly is lightweight.
 */
internal class MetaTextureAtlasLoader(
	resolver: FileHandleResolver,
) : AsynchronousAssetLoader<TextureAtlas, TextureAtlasParameter>(resolver) {
	private var data: TextureAtlasData? = null

	override fun getDependencies(
		fileName: String,
		atlasFile: FileHandle,
		parameter: TextureAtlasParameter?,
	): Array<AssetDescriptor<*>> {
		val atlasData = TextureAtlasData(atlasFile, atlasFile.parent(), parameter?.flip ?: false)
		data = atlasData
		val dependencies = Array<AssetDescriptor<*>>(atlasData.pages.size)
		val pages = atlasData.pages
		for (index in 0 until pages.size) {
			val page = pages[index]
			val textureParameter = TextureParameter().apply {
				format = page.format
				genMipMaps = page.useMipMaps
				minFilter = page.minFilter
				magFilter = page.magFilter
			}
			dependencies.add(AssetDescriptor(page.textureFile, Texture::class.java, textureParameter))
		}
		return dependencies
	}

	override fun loadAsync(
		manager: AssetManager,
		fileName: String,
		file: FileHandle,
		parameter: TextureAtlasParameter?,
	) = Unit

	override fun loadSync(
		manager: AssetManager,
		fileName: String,
		file: FileHandle,
		parameter: TextureAtlasParameter?,
	): TextureAtlas {
		val atlasData = checkNotNull(data) { "Texture atlas data was not prepared for $fileName" }
		val pages = atlasData.pages
		for (index in 0 until pages.size) {
			val page = pages[index]
			page.texture = manager.get(page.textureFile.path().replace('\\', '/'), Texture::class.java)
		}
		data = null
		return TextureAtlas(atlasData)
	}
}
