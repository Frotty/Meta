package de.fatox.meta.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GLTexture
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.TextureData
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.GdxRuntimeException
import java.nio.ByteBuffer

/**
 * Decodes large ordinary pixmap textures on AssetManager's worker, builds their mip chain there, and leaves only an
 * empty texture allocation for loadSync. [StagedTextureUploads] then transfers bounded row batches on later frames.
 */
internal class MetaTextureLoader(
	resolver: FileHandleResolver,
	private val stagedUploads: StagedTextureUploads,
) : AsynchronousAssetLoader<Texture, TextureParameter>(resolver) {
	private var prepared: PreparedTexture? = null
	private var fallbackData: TextureData? = null
	private var existingTexture: Texture? = null

	override fun loadAsync(
		manager: AssetManager,
		fileName: String,
		file: FileHandle,
		parameter: TextureParameter?,
	) {
		prepared = null
		existingTexture = parameter?.texture
		val suppliedData = parameter?.textureData
		val data = suppliedData ?: TextureData.Factory.loadFromFile(
			file,
			parameter?.format,
			parameter?.genMipMaps ?: false,
		)
		if (!data.isPrepared) data.prepare()

		if (existingTexture != null || suppliedData != null || data.type != TextureData.TextureDataType.Pixmap ||
			!StagedTextureUploadPolicy.shouldStage(data.width, data.height)
		) {
			fallbackData = data
			return
		}

		fallbackData = null
		var source: Pixmap? = null
		try {
			source = data.consumePixmap()
			val sourceMustBeDisposed = data.disposePixmap()
			val requestedFormat = data.format
			val base = if (source.format != requestedFormat || !sourceMustBeDisposed) {
				Pixmap(source.width, source.height, requestedFormat).also { converted ->
					converted.setBlending(Pixmap.Blending.None)
					converted.drawPixmap(source, 0, 0)
					if (sourceMustBeDisposed) source.dispose()
					source = null
				}
			} else {
				source.also { source = null }
			}
			val levels = buildMipLevels(base, data.useMipMaps())
			prepared = PreparedTexture(file, levels, data.useMipMaps())
		} finally {
			source?.dispose()
		}
	}

	override fun loadSync(
		manager: AssetManager,
		fileName: String,
		file: FileHandle,
		parameter: TextureParameter?,
	): Texture {
		val staged = prepared
		prepared = null
		val texture = if (staged != null) {
			val base = staged.levels[0]!!
			Texture(
				StagedTextureData(
					staged.file,
					base.width,
					base.height,
					base.format,
					staged.useMipMaps,
				),
			).also { stagedUploads.add(it, staged.levels) }
		} else {
			val data = fallbackData ?: throw GdxRuntimeException("Texture data was not prepared for $fileName")
			fallbackData = null
			existingTexture?.also { it.load(data) } ?: Texture(data)
		}
		existingTexture = null

		if (parameter != null) {
			texture.setFilter(parameter.minFilter, parameter.magFilter)
			texture.setWrap(parameter.wrapU, parameter.wrapV)
		}
		return texture
	}

	override fun getDependencies(
		fileName: String,
		file: FileHandle,
		parameter: TextureParameter?,
	) = null

	override fun unloadAsync(
		manager: AssetManager,
		fileName: String,
		file: FileHandle,
		parameter: TextureParameter?,
	) {
		prepared?.dispose()
		prepared = null
		fallbackData = null
		existingTexture = null
	}

	private fun buildMipLevels(base: Pixmap, useMipMaps: Boolean): Array<Pixmap?> {
		val levels = Array<Pixmap?>()
		levels.add(base)
		if (!useMipMaps) return levels

		var previous = base
		while (previous.width > 1 || previous.height > 1) {
			val width = (previous.width / 2).coerceAtLeast(1)
			val height = (previous.height / 2).coerceAtLeast(1)
			val next = Pixmap(width, height, base.format)
			next.setBlending(Pixmap.Blending.None)
			next.setFilter(Pixmap.Filter.BiLinear)
			next.drawPixmap(previous, 0, 0, previous.width, previous.height, 0, 0, width, height)
			levels.add(next)
			previous = next
		}
		return levels
	}

	private class PreparedTexture(
		val file: FileHandle,
		val levels: Array<Pixmap?>,
		val useMipMaps: Boolean,
	) {
		fun dispose() {
			for (index in 0 until levels.size) {
				levels[index]?.dispose()
				levels[index] = null
			}
		}
	}
}

/**
 * Managed texture data which allocates empty level zero during initial loading. On a context reload, the same object
 * falls back to libGDX's ordinary file upload; context changes are rare and already outside animated startup.
 */
private class StagedTextureData(
	private val file: FileHandle,
	private val width: Int,
	private val height: Int,
	private val format: Pixmap.Format,
	private val useMipMaps: Boolean,
) : TextureData {
	private var prepared = false
	private var initialAllocation = true
	private var reloadData: TextureData? = null

	override fun getType(): TextureData.TextureDataType = TextureData.TextureDataType.Custom

	override fun isPrepared(): Boolean = prepared

	override fun prepare() {
		check(!prepared) { "Already prepared" }
		if (!initialAllocation) {
			reloadData = TextureData.Factory.loadFromFile(file, format, useMipMaps).also {
				if (!it.isPrepared) it.prepare()
			}
		}
		prepared = true
	}

	override fun consumePixmap(): Pixmap =
		throw GdxRuntimeException("Staged texture data uploads itself")

	override fun disposePixmap(): Boolean = false

	override fun consumeCustomData(target: Int) {
		check(prepared) { "Call prepare() before consuming staged texture data" }
		prepared = false
		if (initialAllocation) {
			Gdx.gl.glTexImage2D(
				target,
				0,
				Pixmap.Format.toGlFormat(format),
				width,
				height,
				0,
				Pixmap.Format.toGlFormat(format),
				Pixmap.Format.toGlType(format),
				null,
			)
			initialAllocation = false
			return
		}

		val data = checkNotNull(reloadData) { "Reload texture data was not prepared" }
		reloadData = null
		GLTexture.uploadImageData(target, data, 0)
	}

	override fun getWidth(): Int = width

	override fun getHeight(): Int = height

	override fun getFormat(): Pixmap.Format = format

	override fun useMipMaps(): Boolean = useMipMaps

	override fun isManaged(): Boolean = true
}

/** Render-thread queue of decoded texture mip levels waiting for bounded transfer to OpenGL. */
internal class StagedTextureUploads {
	private val uploads = Array<StagedTextureUpload>()

	val isEmpty: Boolean get() = uploads.size == 0
	val size: Int get() = uploads.size

	fun add(texture: Texture, levels: Array<Pixmap?>) {
		uploads.add(StagedTextureUpload(texture, levels))
	}

	fun update(maxBytes: Int = StagedTextureUploadPolicy.MAX_BYTES_PER_UPDATE): Boolean {
		if (uploads.size == 0) return true
		val upload = uploads[0]
		if (upload.update(maxBytes)) uploads.removeIndex(0)
		return uploads.size == 0
	}

	fun finish() {
		while (uploads.size > 0) update(Int.MAX_VALUE)
	}

	fun dispose() {
		for (index in 0 until uploads.size) uploads[index].dispose()
		uploads.clear()
	}
}

private class StagedTextureUpload(
	private val texture: Texture,
	private val levels: Array<Pixmap?>,
) {
	private var level = 0
	private var row = 0
	private var currentPixels: ByteBuffer? = null

	fun update(maxBytes: Int): Boolean {
		var remainingBudget = maxBytes.coerceAtLeast(1)
		texture.bind()
		Gdx.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1)
		while (remainingBudget > 0 && level < levels.size) {
			val pixmap = levels[level]!!
			if (row == 0 && level > 0) {
				Gdx.gl.glTexImage2D(
					GL20.GL_TEXTURE_2D,
					level,
					pixmap.glInternalFormat,
					pixmap.width,
					pixmap.height,
					0,
					pixmap.glFormat,
					pixmap.glType,
					null,
				)
			}

			val source = currentPixels ?: pixmap.pixels.duplicate().also { currentPixels = it }
			val rowBytes = source.capacity() / pixmap.height
			val rows = StagedTextureUploadPolicy.rowsForBudget(
				rowBytes,
				pixmap.height - row,
				remainingBudget,
			)
			val start = row * rowBytes
			val byteCount = rows * rowBytes
			source.limit(source.capacity())
			source.position(start)
			source.limit(start + byteCount)
			Gdx.gl.glTexSubImage2D(
				GL20.GL_TEXTURE_2D,
				level,
				0,
				row,
				pixmap.width,
				rows,
				pixmap.glFormat,
				pixmap.glType,
				source,
			)
			row += rows
			remainingBudget -= byteCount
			if (row >= pixmap.height) {
				pixmap.dispose()
				levels[level] = null
				level++
				row = 0
				currentPixels = null
			}
		}
		Gdx.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 4)
		Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0)
		return level >= levels.size
	}

	fun dispose() {
		for (index in level until levels.size) {
			levels[index]?.dispose()
			levels[index] = null
		}
	}
}

internal object StagedTextureUploadPolicy {
	const val MAX_BYTES_PER_UPDATE = 512 * 1024
	private const val MIN_PIXELS_TO_STAGE = 512 * 512

	fun shouldStage(width: Int, height: Int): Boolean =
		width > 0 && height > 0 && width.toLong() * height >= MIN_PIXELS_TO_STAGE

	fun rowsForBudget(rowBytes: Int, remainingRows: Int, budgetBytes: Int): Int {
		require(rowBytes > 0)
		require(remainingRows > 0)
		val affordableRows = budgetBytes.coerceAtLeast(1) / rowBytes
		return affordableRows.coerceAtLeast(1).coerceAtMost(remainingRows)
	}
}
