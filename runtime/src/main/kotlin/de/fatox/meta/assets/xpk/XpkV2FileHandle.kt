package de.fatox.meta.assets.xpk

import com.badlogic.gdx.Files
import com.badlogic.gdx.files.FileHandle
import de.fatox.meta.assets.normalisedPath
import java.io.InputStream

/**
 * A libGDX [FileHandle] over one XPK v2 entry.
 *
 * Handles are produced by [XpkV2Archive.find] and only ever name an entry that exists - the archive stores keyed
 * hashes rather than paths, so there is nothing to enumerate and no such thing as a handle for a missing entry. The
 * path here is the one the caller asked for, kept so libGDX has something to report.
 *
 * Reads are self-contained: [XpkV2Archive.bytesOf] decodes only the block holding this entry and hands back a fresh
 * array, so nothing accumulates and nothing needs releasing on a schedule.
 */
class XpkV2FileHandle internal constructor(
	private val archive: XpkV2Archive,
	private val entryIndex: Int,
	private val entryPath: String,
) : FileHandle(entryPath, Files.FileType.Internal) {
	private val fileName: String = entryPath.substringAfterLast('/')

	override fun exists(): Boolean = true

	override fun isDirectory(): Boolean = false

	override fun type(): Files.FileType = Files.FileType.Internal

	override fun name(): String = fileName

	override fun path(): String = entryPath

	override fun pathWithoutExtension(): String = entryPath.substringBeforeLast('.')

	override fun extension(): String = fileName.substringAfterLast('.', missingDelimiterValue = "")

	override fun nameWithoutExtension(): String = fileName.substringBeforeLast('.')

	override fun length(): Long = archive.sizeOf(entryIndex)

	override fun readBytes(): ByteArray = archive.bytesOf(entryIndex)

	override fun readBytes(bytes: ByteArray, offset: Int, size: Int): Int {
		val source = readBytes()
		val copied = minOf(size, source.size)
		source.copyInto(bytes, offset, 0, copied)
		return copied
	}

	override fun read(): InputStream = readBytes().inputStream()

	/** Sibling and child lookups go back through the archive, which is the only thing that can resolve a name. */
	override fun sibling(name: String?): FileHandle = resolveRelative(parentPath(), name.orEmpty())

	override fun child(name: String): FileHandle = resolveRelative(entryPath, name)

	override fun parent(): FileHandle = XpkV2DirectoryHandle(archive, parentPath())

	override fun list(): kotlin.Array<FileHandle> = emptyArray()

	override fun toString(): String = entryPath

	private fun parentPath(): String = entryPath.substringBeforeLast('/', missingDelimiterValue = "")

	private fun resolveRelative(base: String, name: String): FileHandle {
		val combined = if (base.isEmpty()) name else "$base/$name"
		return archive.find(combined) ?: XpkV2DirectoryHandle(archive, normalisedPath(combined))
	}
}

/**
 * A handle for a path the archive cannot confirm: a parent, or a lookup that missed.
 *
 * Reports `exists() == false` rather than standing in for something else. v1 answered a failed lookup with a handle
 * carrying the *calling* entry's payload, which fed one asset's bytes to another's loader; nothing here can alias.
 * Directory structure is not recoverable from name hashes, so it cannot claim to be a directory either.
 */
class XpkV2DirectoryHandle internal constructor(
	private val archive: XpkV2Archive,
	private val directoryPath: String,
) : FileHandle(directoryPath, Files.FileType.Internal) {
	override fun exists(): Boolean = false

	override fun isDirectory(): Boolean = false

	override fun type(): Files.FileType = Files.FileType.Internal

	override fun path(): String = directoryPath

	override fun name(): String = directoryPath.substringAfterLast('/')

	override fun length(): Long = 0L

	override fun list(): kotlin.Array<FileHandle> = emptyArray()

	override fun child(name: String): FileHandle {
		val combined = if (directoryPath.isEmpty()) name else "$directoryPath/$name"
		return archive.find(combined) ?: XpkV2DirectoryHandle(archive, normalisedPath(combined))
	}

	override fun parent(): FileHandle =
		XpkV2DirectoryHandle(archive, directoryPath.substringBeforeLast('/', missingDelimiterValue = ""))

	override fun toString(): String = directoryPath
}
