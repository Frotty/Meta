package de.fatox.meta.assets

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.GdxRuntimeException
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import java.io.InputStream

class XPKFileHandle(
	private val siblings: Array<XPKFileHandle>,
	var length: Int,
	private val sevenZFile: XPKByteChannel,
	private val entry: SevenZArchiveEntry,
	val name: String,
) : FileHandle() {
	val path: String = name.substringBeforeLast('\\')
	private val fileName: String = name.substringAfterLast('\\')
	val array: ByteArray? by lazy(LazyThreadSafetyMode.NONE) { XPKLoader.loadEntry(sevenZFile, entry) }

	override fun exists(): Boolean = true

	override fun parent(): FileHandle {
		val name = if (path != this.name) path else ""

		return XPKFileHandle(siblings, 0, sevenZFile, entry, name)
	}

	override fun sibling(name: String?): FileHandle? =
		siblings.firstOrNull { it.path == path && it.fileName == name }

	override fun read(): InputStream {
		try {
			return readBytes()!!.inputStream()
		} catch (ex: Throwable) {
			if (file() != null && file().isDirectory)
				throw GdxRuntimeException("Cannot open a stream to a directory: $file ($type)", ex)
			throw GdxRuntimeException("Error reading file: $file ($type)", ex)
		}
	}

	override fun name(): String = name

	override fun extension(): String = fileName.substringAfterLast('.')

	override fun nameWithoutExtension(): String = fileName.substringBeforeLast('.')

	override fun path(): String = name

	override fun pathWithoutExtension(): String = name.substringBeforeLast('.')

	override fun length(): Long = length.toLong()

	override fun child(name: String): FileHandle? {
		val search = if (path != name && path.isNotBlank()) "$path\\$name" else name

		return siblings.firstOrNull {
			it.name.replace('/', '\\') == search
		} ?: XPKFileHandle(siblings, 0, sevenZFile, entry, search)
	}

	override fun toString(): String = name.replace('\\', '/')

	override fun readBytes(): ByteArray? = array
}