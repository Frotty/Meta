package de.fatox.meta.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.reflect.ClassReflection
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.api.extensions.error
import de.fatox.meta.api.extensions.trace
import de.fatox.meta.api.extensions.warn
import de.fatox.meta.canonicalAppStorageName
import de.fatox.meta.injection.MetaInject.Companion.inject
import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.reflect.KClass

private val log = MetaLoggerFactory.logger {}

/**
 * Stores application data as JSON beneath the game's save directory, and caches what it loads.
 *
 * This holds settings, keybindings and window layout - things a player configures once and expects to keep - so the
 * two properties that matter most are that a save either lands completely or not at all, and that a file which fails
 * to parse is never the last copy of anything.
 *
 * Prefer reusable [MetaDataKey] instances over the deprecated string-key overloads.
 */
class MetaData(root: FileHandle? = null) {
	internal class CacheObj<T : Any>(var obj: T, var created: Long)

	private val gameName: String = canonicalAppStorageName(inject("gameName"))
	private val fileHandleCache = ObjectMap<String, FileHandle>()
	private val fileCache = ObjectMap<String, File>()
	private val jsonCache = ObjectMap<String, CacheObj<Any>>()

	private val json = Json().apply {
		// Standard JSON rather than libGDX's "minimal" dialect, which omits quotes around names and is not valid
		// JSON. Reading accepts both, so this only affects what is written from here on - and it makes a save file
		// something a person or an ordinary tool can open when a player reports a problem.
		setOutputType(JsonWriter.OutputType.json)
	}

	/**
	 * Where saved data lives. Overridable so tests get a temporary directory.
	 *
	 * Without this the only root was the user's home directory, so exercising persistence at all meant writing into
	 * whoever ran the build - which is why none of this was tested.
	 */
	val dataRoot: FileHandle =
		root ?: Gdx.files.external(".$gameName").child(GLOBAL_DATA_FOLDER_NAME).also { it.mkdirs() }

	private fun cacheId(key: String, parent: FileHandle): String =
		parent.file().absolutePath + '\u0000' + key

	// ---- saving ---------------------------------------------------------------------------------------------

	@Suppress("DEPRECATION")
	fun <T : Any> save(key: MetaDataKey<T>, obj: T, target: FileHandle = dataRoot): FileHandle =
		save(key.name, obj, target)

	/**
	 * Serializes [obj] and replaces the stored value, atomically.
	 *
	 * @return the [FileHandle] the value now lives in.
	 */
	@Deprecated(
		"Use MetaData#save with MetaDataKey. " +
			"This method will be made private in a future version. " +
			"Note that it is advised to cache the MetaDataKey.",
		ReplaceWith(
			"save(MetaDataKey<T>(key),obj,target)",
			"de.fatox.meta.assets.MetaData",
			"de.fatox.meta.assets.MetaDataKey",
			"com.badlogic.gdx.files.FileHandle",
		)
	)
	fun <T : Any> save(key: String, obj: T, target: FileHandle = dataRoot): FileHandle {
		val handle = getCachedHandle(key, target)
		val newBytes = json.toJson(obj).toByteArray()

		// Unchanged values are not rewritten: it saves a disk write, and leaving the modification time alone keeps
		// every cache entry pointing at this file valid.
		if (handle.exists() && handle.readBytes().contentEquals(newBytes)) {
			log.trace { "Unchanged, not rewriting: $key" }
			return handle
		}

		log.debug { "Saving $key (${obj::class.simpleName}) to $target" }
		writeAtomically(handle, newBytes)
		jsonCache.put(cacheId(key, target), CacheObj(obj, handle.lastModified()))
		return handle
	}

	/**
	 * Writes to a scratch file, flushes it to the device, then renames it over the target.
	 *
	 * A direct write leaves a window in which the file on disk is neither the old value nor the new one, and killing
	 * a game while it saves is ordinary behaviour rather than an edge case. A rename is atomic, so a reader sees one
	 * complete version or the other, and the flush is what makes that true after a power loss rather than only after
	 * a process kill.
	 */
	private fun writeAtomically(handle: FileHandle, bytes: ByteArray) {
		val target = handle.file()
		val directory = target.parentFile
		directory?.mkdirs()

		val scratch = File.createTempFile(target.name, SCRATCH_SUFFIX, directory)
		try {
			FileOutputStream(scratch).use { output ->
				output.write(bytes)
				output.flush()
				output.fd.sync()
			}
			try {
				Files.move(
					scratch.toPath(),
					target.toPath(),
					StandardCopyOption.ATOMIC_MOVE,
					StandardCopyOption.REPLACE_EXISTING,
				)
			} catch (_: AtomicMoveNotSupportedException) {
				// Some network and virtual filesystems refuse an atomic replace. A plain one still beats writing
				// through the live file, and the flush above means the source was complete before either happened.
				log.debug { "Atomic replace unavailable for ${target.name}; falling back to a plain move" }
				Files.move(scratch.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
			}
		} finally {
			// A no-op once the move succeeded, and the reason a failed save leaves no half-written file behind for
			// the next launch to read as corrupt.
			scratch.delete()
		}
	}

	// ---- loading --------------------------------------------------------------------------------------------

	@Suppress("DEPRECATION")
	fun <T : Any> get(key: MetaDataKey<T>, type: KClass<out T>, parent: FileHandle = dataRoot): T =
		get(key.name, type, parent)

	/**
	 * Returns the stored value, or a fresh instance when there is none.
	 *
	 * Reading never writes. A missing key used to be answered by serializing a default instance to disk, which turned
	 * merely asking for settings into creating them.
	 */
	@Deprecated(
		"Use MetaData#get with MetaDataKey. " +
			"This method will be made private in a future version. " +
			"Note that it is advised to cache the MetaDataKey.",
		ReplaceWith(
			"get(MetaDataKey<T>(key),parent)",
			"de.fatox.meta.assets.MetaData",
			"de.fatox.meta.assets.get",
			"de.fatox.meta.assets.MetaDataKey",
			"com.badlogic.gdx.files.FileHandle",
		)
	)
	operator fun <T : Any> get(key: String, type: KClass<out T>, parent: FileHandle = dataRoot): T =
		read(key, type, parent) ?: newInstance(type)

	@Suppress("DEPRECATION")
	fun <T : Any> load(key: MetaDataKey<T>, type: KClass<out T>, target: FileHandle = dataRoot): T? =
		load(key.name, type, target)

	/** Returns the stored value, or `null` when there is none or it could not be read. */
	@Deprecated(
		"Use MetaData#load with MetaDataKey. " +
			"This method will be made private in a future version. " +
			"Note that it is advised to cache the MetaDataKey.",
		ReplaceWith(
			"load(MetaDataKey<T>(key),target)",
			"de.fatox.meta.assets.MetaData",
			"de.fatox.meta.assets.get",
			"de.fatox.meta.assets.MetaDataKey",
			"com.badlogic.gdx.files.FileHandle",
		)
	)
	fun <T : Any> load(key: String, type: KClass<out T>, target: FileHandle = dataRoot): T? =
		read(key, type, target)

	private fun <T : Any> read(key: String, type: KClass<out T>, parent: FileHandle): T? {
		val handle = getCachedHandle(key, parent)
		val cacheId = cacheId(key, parent)
		val lastModified = if (handle.exists()) handle.lastModified() else 0L

		jsonCache.get(cacheId)?.let { cached ->
			// Both sides of this comparison are now the file's own modification time. Comparing it against a
			// wall-clock reading taken when the entry was created made the answer depend on two different clocks.
			if (cached.created >= lastModified) {
				log.trace { "Cache hit: $key" }
				@Suppress("UNCHECKED_CAST")
				return cached.obj as T
			}
			log.debug { "File is newer than the cached value, reloading: $key" }
		}

		if (!handle.exists()) return null

		return try {
			val loaded = json.fromJson(type.java, handle)
				?: throw IllegalStateException("Deserialized to null")
			jsonCache.put(cacheId, CacheObj(loaded, lastModified))
			loaded
		} catch (failure: RuntimeException) {
			// Anything the reader throws means these bytes are not a value of this type: truncated by a kill during
			// a save, damaged on disk, or written by a build whose class shape no longer matches.
			val kept = quarantine(handle)
			log.error(
				"Could not read $key as ${type.simpleName}" +
					if (kept != null) "; kept the file as ${kept.name()}" else "; the file could not be set aside",
				failure,
			)
			jsonCache.remove(cacheId)
			null
		}
	}

	/**
	 * Moves an unreadable file aside instead of replacing it.
	 *
	 * Recovering by writing a fresh instance over it destroyed the only copy of whatever the player had configured,
	 * and left nothing to diagnose from. Keeping it costs a few kilobytes and means a bad save is recoverable by
	 * hand.
	 */
	private fun quarantine(handle: FileHandle): FileHandle? {
		val file = handle.file()
		if (!file.exists()) return null

		var candidate = File(file.parentFile, file.name + CORRUPT_SUFFIX)
		var attempt = 1
		while (candidate.exists() && attempt < MAX_QUARANTINE_ATTEMPTS) {
			candidate = File(file.parentFile, "${file.name}$CORRUPT_SUFFIX.$attempt")
			attempt++
		}
		if (candidate.exists()) {
			log.warn { "Too many quarantined copies of ${file.name}; leaving the unreadable file in place" }
			return null
		}
		return if (file.renameTo(candidate)) FileHandle(candidate) else null
	}

	private fun <T : Any> newInstance(type: KClass<out T>): T =
		@Suppress("UNCHECKED_CAST")
		(ClassReflection.newInstance(type.java) as T)

	// ---- handles --------------------------------------------------------------------------------------------

	@Suppress("DEPRECATION")
	fun getCachedHandle(key: MetaDataKey<*>, parent: FileHandle = dataRoot): FileHandle =
		getCachedHandle(key.name, parent)

	@Deprecated(
		"Use MetaData#getCachedHandle with MetaDataKey. " +
			"This method will be made private in a future version. " +
			"Note that it is advised to cache the MetaDataKey.",
		ReplaceWith(
			"getCachedHandle(MetaDataKey<Any>(key),parent)",
			"de.fatox.meta.assets.MetaData",
			"de.fatox.meta.assets.MetaDataKey",
			"com.badlogic.gdx.files.FileHandle",
		)
	)
	fun getCachedHandle(key: String, parent: FileHandle = dataRoot): FileHandle {
		val cacheId = cacheId(key, parent)
		if (!fileHandleCache.containsKey(cacheId)) {
			var child: FileHandle = parent.child(key)
			if (!child.exists() && parent.path() == dataRoot.path()) {
				val legacy = Gdx.files.external(GLOBAL_DATA_FOLDER_NAME + key)
				if (legacy.exists()) child = legacy
			}
			fileHandleCache.put(cacheId, child)
			fileCache.put(cacheId, child.file())
		}
		return fileHandleCache.get(cacheId)
	}

	@Suppress("DEPRECATION")
	fun has(key: MetaDataKey<*>, fileHandle: FileHandle = dataRoot): Boolean = has(key.name, fileHandle)

	@Deprecated(
		"Use MetaData#has with MetaDataKey. " +
			"This method will be made private in a future version. " +
			"Note that it is advised to cache the MetaDataKey.",
		ReplaceWith(
			"has(MetaDataKey<Any>(name),fileHandle)",
			"de.fatox.meta.assets.MetaData",
			"de.fatox.meta.assets.MetaDataKey",
			"com.badlogic.gdx.files.FileHandle",
		)
	)
	fun has(name: String, fileHandle: FileHandle = dataRoot): Boolean =
		fileHandleCache.containsKey(cacheId(name, fileHandle)) || fileHandle.child(name).exists()

	companion object {
		const val GLOBAL_DATA_FOLDER_NAME: String = ".meta"

		/** Suffix for a file set aside because it could not be read. Never loaded; kept for recovery. */
		const val CORRUPT_SUFFIX: String = ".corrupt"

		private const val SCRATCH_SUFFIX = ".tmp"
		private const val MAX_QUARANTINE_ATTEMPTS = 32
	}
}

@Suppress("unused")
@JvmInline
value class MetaDataKey<T : Any>(val name: String)

@Suppress("DEPRECATION")
@Deprecated(
	"Use MetaData#get with MetaDataKey. " +
		"This method will be made private in a future version. " +
		"Note that it is advised to cache the MetaDataKey.",
	ReplaceWith(
		"get(MetaDataKey<T>(key),parent)",
		"de.fatox.meta.MetaData",
		"de.fatox.meta.assets.MetaData.get",
		"de.fatox.meta.assets.MetaDataKey",
		"com.badlogic.gdx.files.FileHandle",
	)
)
inline operator fun <reified T : Any> MetaData.get(key: String, parent: FileHandle = dataRoot): T =
	get(key, T::class, parent)

@Suppress("DEPRECATION")
inline operator fun <reified T : Any> MetaData.get(key: MetaDataKey<T>, parent: FileHandle = dataRoot): T =
	get(key.name, T::class, parent)

@Suppress("DEPRECATION")
inline fun <reified T : Any> MetaData.load(key: MetaDataKey<T>, target: FileHandle = dataRoot): T? =
	load(key.name, T::class, target)
