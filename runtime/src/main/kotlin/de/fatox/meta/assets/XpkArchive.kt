package de.fatox.meta.assets

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import org.apache.commons.compress.archivers.sevenz.SevenZFile

/**
 * One opened XPK archive: an entry index built once, and entry payloads served by a forward-sweeping reader.
 *
 * The archive is a solid stream, so reading entry *k* necessarily decompresses entries *0..k* in the same block.
 * The previous implementation opened a fresh reader and re-swept from zero for every single entry, which made a full
 * asset load quadratic - measured at 237x slower than one sequential pass over a 512-entry archive. This class keeps
 * one reader positioned at a cursor and caches every entry the sweep passes over, so the work those entries already
 * cost is not thrown away.
 *
 * Two policies keep that linear without letting memory run away:
 *
 * - Pass-through caching stops at [PASSTHROUGH_CACHE_BUDGET], counting the *prospective* entry size so one oversized
 *   payload cannot step over the bound on its own. Past the budget the sweep still decompresses - it has no choice -
 *   but stops retaining entries nobody asked for.
 * - A rewind for an uncached entry means the budget already declined to keep something the caller then wanted, and a
 *   descending access order would repeat that for every remaining entry. The first rewind therefore lifts the budget
 *   for this archive, trading the memory bound for the linearity guarantee. Worst-case retention is then the
 *   archive's uncompressed size, which is what the old implementation reached anyway.
 *
 * [releaseCachedEntries] drops everything, and resets both policies, once a load phase drains - see
 * `docs/xpk-format-audit.md` M2.
 */
class XpkArchive internal constructor(
	/** Whole archive, signature already restored. Released on [dispose]. */
	private var fileBytes: ByteArray?,
	/** Archive path, for diagnostics only. */
	private val archivePath: String,
	private val entryNames: kotlin.Array<String>,
	private val entrySizes: LongArray,
	private val entryIsDirectory: BooleanArray,
	/** Overridable so tests can exercise budget exhaustion without building a 64 MB archive. */
	private val passthroughCacheBudget: Long = PASSTHROUGH_CACHE_BUDGET,
) : Disposable {
	private val lock = Any()
	private val cache = arrayOfNulls<ByteArray>(entryNames.size)

	/** Index of the entry the open reader will yield next. */
	private var cursor = 0
	private var reader: SevenZFile? = null
	private var passthroughBytes = 0L
	private var retainedBytes = 0L

	/** Set once a rewind proves the budget is costing more sweeps than it saves memory. */
	private var unboundedCaching = false
	private var sweeps = 0
	private var disposed = false

	/** Bytes of entry payload currently retained. Test observability for the release and budget contracts. */
	internal val retainedEntryBytes: Long get() = synchronized(lock) { retainedBytes }

	/** How many times the solid stream has been rewound. Test observability for the linear-access contract. */
	internal val sweepCount: Int get() = synchronized(lock) { sweeps }

	/** File entries only, in archive order. Directory entries are indexed but never handed out. */
	val entries: Array<XPKFileHandle> = Array(entryNames.size)

	private val byPath = ObjectMap<String, XPKFileHandle>(entryNames.size)
	private val directories = ObjectSet<String>()

	/** The same keys as [directories], in insertion order, so listings can iterate without a reusable iterator. */
	private val directoryPaths = Array<String>()

	init {
		// The archive root is always a directory, even though no entry path names it. Without this, parent() on a
		// top-level entry reports a handle that does not exist and lists nothing.
		addDirectory("")
		for (index in entryNames.indices) {
			val path = normalisedPath(entryNames[index])
			if (entryIsDirectory[index]) {
				addDirectory(assetPathKey(path))
				// A stored directory's own ancestors need registering too: an archive may record `a/b/` without ever
				// recording `a`, which would otherwise leave `a` unlistable and absent from the root.
				addAncestorDirectories(path)
				continue
			}
			val handle = XPKFileHandle(this, index, path)
			entries.add(handle)
			byPath.put(assetPathKey(path), handle)
			addAncestorDirectories(path)
		}
	}

	/** Registers every ancestor of [path] as a directory, whether or not the archive stored it explicitly. */
	private fun addAncestorDirectories(path: String) {
		var separator = path.lastIndexOf('/')
		while (separator > 0) {
			addDirectory(assetPathKey(path.substring(0, separator)))
			separator = path.lastIndexOf('/', separator - 1)
		}
	}

	private fun addDirectory(key: String) {
		if (directories.add(key)) directoryPaths.add(key)
	}

	/** Resolves an archive-relative path case-insensitively, or a non-existent handle when there is no such entry. */
	internal fun resolve(path: String): XPKFileHandle {
		val normalised = normalisedPath(path)
		return byPath[assetPathKey(normalised)] ?: XPKFileHandle(this, XPK_MISSING_ENTRY, normalised)
	}

	internal fun isDirectory(path: String): Boolean = directories.contains(assetPathKey(normalisedPath(path)))

	/**
	 * Immediate children of a directory path: file entries directly under it, then its direct subdirectories.
	 *
	 * Both sources are needed. Walking only the file entries misses a directory the archive stored explicitly with
	 * nothing under it, which would then satisfy `exists()` and `isDirectory()` while never appearing in its parent's
	 * listing. Walking only the directory set misses the directories implied by file paths, which archives commonly
	 * do not store at all.
	 *
	 * Scans both tables, which is fine because directory listing is a tooling and diagnostics path, never a loading
	 * one. Inheriting libGDX's default [FileHandle.list] instead returned an empty array for every archive directory,
	 * because it consults a `File` that does not exist on disk.
	 */
	internal fun childrenOf(path: String): kotlin.Array<FileHandle> {
		val prefix = if (path.isEmpty()) "" else "${assetPathKey(normalisedPath(path))}/"
		val children = Array<FileHandle>()
		val seenDirectories = ObjectSet<String>()

		for (index in 0 until entries.size) {
			val candidate = entries[index]
			val key = assetPathKey(candidate.path())
			if (!key.startsWith(prefix)) continue
			val remainder = key.substring(prefix.length)
			val separator = remainder.indexOf('/')
			if (separator < 0) {
				children.add(candidate)
			} else if (seenDirectories.add(remainder.substring(0, separator))) {
				children.add(resolve(candidate.path().substring(0, prefix.length + separator)))
			}
		}

		// Directories the archive recorded explicitly, including ones holding no files at all. Indexed rather than
		// iterated: libGDX's reusable iterators are not nesting-safe, and this runs inside resolve().
		for (index in 0 until directoryPaths.size) {
			val directory = directoryPaths[index]
			if (directory.isEmpty() || !directory.startsWith(prefix)) continue
			val remainder = directory.substring(prefix.length)
			if (remainder.isEmpty() || remainder.indexOf('/') >= 0) continue
			if (seenDirectories.add(remainder)) children.add(resolve(directory))
		}

		return kotlin.Array(children.size) { children[it] }
	}

	internal fun sizeOf(entryIndex: Int): Long = entrySizes[entryIndex]

	/** Decompresses one entry, sweeping the solid stream forward and retaining what it passes. */
	internal fun bytesOf(entryIndex: Int): ByteArray = synchronized(lock) {
		check(!disposed) { "XPK archive $archivePath was disposed" }
		cache[entryIndex]?.let { return it }

		if (reader == null) {
			restart()
		} else if (cursor > entryIndex) {
			// Reaching here means an uncached entry sits behind the cursor, which only happens once the pass-through
			// budget has declined to keep something. That is proof the budget cost more than it saved: a descending
			// access order would otherwise re-decompress the prefix for every remaining entry, reinstating exactly the
			// quadratic behaviour this class exists to remove. Trade the bound for linearity from here on;
			// releaseCachedEntries reclaims the memory when the load phase drains.
			unboundedCaching = true
			restart()
		}
		val active = reader ?: throw GdxRuntimeException("Could not open XPK archive $archivePath")

		while (cursor <= entryIndex) {
			val entry = active.nextEntry ?: break
			val at = cursor++
			if (entryIsDirectory[at]) continue
			val requested = at == entryIndex
			// Skipping still decompresses, so retaining a pass-through entry costs only the copy - until the budget
			// is spent, after which unrequested entries are decompressed and dropped. The prospective size is part of
			// the test: checking only what is already cached would admit one entry of any size, so a single large
			// video or model could overshoot the bound by its whole payload.
			if (!requested && (cache[at] != null || !fitsInPassthroughBudget(entrySizes[at]))) continue
			val bytes = readEntry(active, entry.name, entrySizes[at])
			cache[at] = bytes
			retainedBytes += bytes.size
			if (!requested) passthroughBytes += bytes.size
		}

		return cache[entryIndex] ?: throw GdxRuntimeException(
			"XPK entry ${entryNames[entryIndex]} not found in $archivePath",
		)
	}

	private fun fitsInPassthroughBudget(size: Long): Boolean =
		unboundedCaching || passthroughBytes + size <= passthroughCacheBudget

	/**
	 * Drops every cached entry payload and closes the open reader, keeping the archive usable.
	 *
	 * A game loads, then plays. Entry bytes are consumed once - decoded into a texture, a sound or a model - and then
	 * only the decoded form is needed, so retaining them for the process lifetime means a second full copy of the
	 * asset data sits in heap next to the GPU and OpenAL copies. Closing the reader also releases its LZMA2
	 * dictionary, which is tens of megabytes at high presets.
	 *
	 * Cheap and idempotent when nothing is cached, so it is safe to call from a per-frame completion check. A later
	 * read simply re-opens and sweeps again.
	 */
	fun releaseCachedEntries() {
		synchronized(lock) {
			if (disposed) return
			reader?.close()
			reader = null
			cursor = 0
			cache.fill(null)
			passthroughBytes = 0
			retainedBytes = 0
			unboundedCaching = false
		}
	}

	/**
	 * True when nothing is retained, so callers can skip a redundant [releaseCachedEntries].
	 *
	 * Counted rather than scanned: this is polled from the per-frame completion check, and scanning the entry table
	 * every frame would make the check itself scale with archive size.
	 */
	internal val isFullyReleased: Boolean
		get() = synchronized(lock) { reader == null && retainedBytes == 0L }

	/**
	 * Rewinds the sweep. Each reader gets its own [XPKByteChannel] view, because closing a `SevenZFile` closes the
	 * channel it was given - so a shared channel could be used exactly once.
	 */
	private fun restart() {
		val bytes = checkNotNull(fileBytes) { "XPK archive $archivePath was disposed" }
		reader?.close()
		reader = SevenZFile.Builder().setSeekableByteChannel(XpkReadOnlyChannel(bytes)).get()
		cursor = 0
		sweeps++
	}

	private fun readEntry(file: SevenZFile, name: String, size: Long): ByteArray {
		require(size in 0..Int.MAX_VALUE.toLong()) { "Invalid XPK entry size: $name ($size bytes)" }
		val content = ByteArray(size.toInt())
		var offset = 0
		while (offset < content.size) {
			val read = file.read(content, offset, content.size - offset)
			if (read < 0) break
			offset += read
		}
		check(offset == content.size) { "Unexpected end of XPK entry $name at $offset/${content.size}" }
		return content
	}

	override fun dispose() {
		synchronized(lock) {
			if (disposed) return
			disposed = true
			reader?.close()
			reader = null
			cache.fill(null)
			passthroughBytes = 0
			retainedBytes = 0
			fileBytes = null
		}
	}
}

/** How many bytes of never-requested entries a sweep may retain before it stops keeping them. */
internal const val PASSTHROUGH_CACHE_BUDGET: Long = 64L * 1024 * 1024

/** Sentinel entry index for a handle that names a path its archive does not contain. */
internal const val XPK_MISSING_ENTRY: Int = -1

/**
 * Canonical form of an entry path: `/`-separated, no leading or trailing separator, no empty or `.` segments.
 *
 * Archive metadata is not ours to trust. 7z stores whatever the packer wrote, and a directory is flagged by an
 * attribute rather than by its spelling - a Commons Compress packer emitting `empty/` round-trips that trailing slash
 * verbatim, which as a raw key made `child("empty")` miss and hid the directory from its parent's listing. Normalising
 * once here, at the single point where archive names enter, is what keeps every lookup and listing agreeing, rather
 * than each of them stripping separators defensively.
 *
 * The previous implementation rewrote paths to `\` on the way out, which every consumer then had to undo - see the
 * `replace('\\', '/')` that [MetaTextureAtlasLoader] carried to recover AssetManager keys.
 */
internal fun normalisedPath(path: String): String {
	val forwardSlashed = path.replace('\\', '/')
	if (isCanonicalPath(forwardSlashed)) return forwardSlashed

	val builder = StringBuilder(forwardSlashed.length)
	var start = 0
	while (start <= forwardSlashed.length) {
		var end = forwardSlashed.indexOf('/', start)
		if (end < 0) end = forwardSlashed.length
		val length = end - start
		val skip = length == 0 || (length == 1 && forwardSlashed[start] == '.')
		if (!skip) {
			if (builder.isNotEmpty()) builder.append('/')
			builder.append(forwardSlashed, start, end)
		}
		start = end + 1
	}
	return builder.toString()
}

/**
 * Whether [value] is already in the form [normalisedPath] produces, so the rewrite and its allocation can be skipped.
 *
 * Derived from the same rule the parser applies - reject a leading or trailing separator, an empty segment, or a `.`
 * segment - rather than from a list of substrings to look for. That list was the bug: it enumerated `./`, `/./` and
 * a bare `.`, and missed a terminal `/.`, so `empty/.` slipped through unchanged and appeared in listings beside the
 * `empty` it should have been. A guard written from the property cannot disagree with the parser about a spelling
 * neither of us thought of.
 *
 * `..` is deliberately left alone. Entries are resolved through a map and never touch the filesystem, so it carries
 * no traversal risk here, and collapsing it would alias two entries an archive kept distinct.
 */
private fun isCanonicalPath(value: String): Boolean {
	if (value.isEmpty()) return true
	if (value[0] == '/' || value[value.length - 1] == '/') return false
	var start = 0
	while (start < value.length) {
		var end = value.indexOf('/', start)
		if (end < 0) end = value.length
		val length = end - start
		if (length == 0) return false
		if (length == 1 && value[start] == '.') return false
		start = end + 1
	}
	return true
}
