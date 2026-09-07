package de.fatox.meta.assets

import com.badlogic.gdx.Files
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.GdxRuntimeException
import java.io.InputStream

/**
 * A libGDX [FileHandle] over one XPK entry, or over a path the archive does not contain.
 *
 * A handle whose [entryIndex] is [XPK_MISSING_ENTRY] reports `exists() == false` and refuses reads. That distinction
 * matters: the previous implementation answered a failed [child] or [parent] lookup with a handle that carried the
 * *calling* entry's name and size, so a miss returned another entry's payload with `exists()` true and `length()` 0.
 * Texture-atlas page resolution goes straight through [child], so a page-name mismatch fed atlas text to `Pixmap`
 * instead of reporting a missing file.
 */
class XPKFileHandle internal constructor(
	private val archive: XpkArchive,
	private val entryIndex: Int,
	/** Archive-relative path, always `/`-separated. */
	private val entryPath: String,
) : FileHandle(entryPath, Files.FileType.Internal) {
	private val parentPath: String = entryPath.substringBeforeLast('/', missingDelimiterValue = "")
	private val fileName: String = entryPath.substringAfterLast('/')

	private val isMissing: Boolean get() = entryIndex == XPK_MISSING_ENTRY

	/** Entry bytes, decompressed on first use, or `null` when this handle names no archive entry. */
	val array: ByteArray? get() = if (isMissing) null else archive.bytesOf(entryIndex)

	override fun exists(): Boolean = !isMissing || archive.isDirectory(entryPath)

	override fun isDirectory(): Boolean = isMissing && archive.isDirectory(entryPath)

	override fun type(): Files.FileType = Files.FileType.Internal

	override fun name(): String = fileName

	override fun path(): String = entryPath

	override fun pathWithoutExtension(): String = entryPath.substringBeforeLast('.')

	override fun extension(): String = fileName.substringAfterLast('.', missingDelimiterValue = "")

	override fun nameWithoutExtension(): String = fileName.substringBeforeLast('.')

	override fun length(): Long = if (isMissing) 0L else archive.sizeOf(entryIndex)

	override fun parent(): FileHandle = archive.resolve(parentPath)

	override fun list(): kotlin.Array<FileHandle> =
		if (isDirectory()) archive.childrenOf(entryPath) else emptyArray()

	override fun sibling(name: String?): FileHandle =
		archive.resolve(if (parentPath.isEmpty()) name.orEmpty() else "$parentPath/${name.orEmpty()}")

	override fun child(name: String): FileHandle =
		archive.resolve(if (entryPath.isEmpty()) name else "$entryPath/$name")

	override fun readBytes(): ByteArray = array ?: throw GdxRuntimeException("XPK entry not found: $entryPath")

	override fun readBytes(bytes: ByteArray, offset: Int, size: Int): Int {
		val source = readBytes()
		val copied = minOf(size, source.size)
		source.copyInto(bytes, offset, 0, copied)
		return copied
	}

	override fun read(): InputStream {
		if (isDirectory()) throw GdxRuntimeException("Cannot open a stream to a directory: $entryPath (${type()})")
		return readBytes().inputStream()
	}

	override fun toString(): String = entryPath
}
