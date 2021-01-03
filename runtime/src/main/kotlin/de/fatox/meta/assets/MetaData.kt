package de.fatox.meta.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.SerializationException
import com.badlogic.gdx.utils.TimeUtils
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.badlogic.gdx.utils.reflect.ReflectionException
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.api.extensions.error
import de.fatox.meta.api.extensions.trace
import de.fatox.meta.injection.MetaInject.Companion.inject
import java.io.File
import kotlin.reflect.KClass

private val log = MetaLoggerFactory.logger {}

/**
 * Created by Frotty on 10.03.2017.
 * Handles MetaData needs.
 * De-/serializes config classes from a .meta sub folder in the game's data folder inside user home
 * MetaData can be accessed via #.get and will be cached.
 */
class MetaData {
	internal class CacheObj<T : Any>(var obj: T, var created: Long = TimeUtils.millis())

	private val gameName: String = inject("gameName")
	private val fileHandleCache = ObjectMap<String, FileHandle>()
	private val fileCache = ObjectMap<String, File>()
	private val jsonCache = ObjectMap<String, CacheObj<Any>>()
	private val json = Json()

	private val dataRoot: FileHandle =
		Gdx.files.external(".$gameName").child(GLOBAL_DATA_FOLDER_NAME).also { it.mkdirs() }

	private val emptyByteArray = ByteArray(0)

	/**
	 * @param key String
	 * @param obj T
	 * @param target FileHandle
	 * @return The cached [FileHandle] of the serialized [obj].
	 */
	fun <T : Any> save(key: String, obj: T, target: FileHandle = dataRoot): FileHandle {
		// Get the file handle and (over) write the serialized json object to it
		return getCachedHandle(key, target).also { fileHandle: FileHandle ->
			val newBytes = json.toJson(obj).toByteArray()
			val oldBytes = if (fileHandle.exists()) fileHandle.readBytes() else emptyByteArray

			log.trace {
				"""
					New bytes (max 100):
						${newBytes.joinToString(limit = 100)}
					Old bytes (max 100):
						${oldBytes.joinToString(limit = 100)}
				""".trimIndent()
			}

			// Only save if obj is different
			if (oldBytes.contentEquals(newBytes)) return@also

			log.debug {
				"""
				Save the following:
					key:    $key
					type:   ${obj::class.simpleName}
					target: $target
				""".trimIndent()
			}

			fileHandle.writeBytes(newBytes, false)

			// Update object in json cache, if it exists
			jsonCache.get(key)?.let {
				log.debug { "Update json cache!" }

				it.obj = obj
				it.created = fileHandle.lastModified()
			}
		}
	}

	fun <T : Any> save(key: MetaDataKey<T>, obj: T): FileHandle = save(key.name, obj)

	/**
	 * Caches and returns this object loaded from json at the specified location.
	 *
	 * @param key String
	 * @param type KClass<out T>
	 * @param parent FileHandle
	 * @return T
	 */
	operator fun <T : Any> get(key: String, type: KClass<out T>, parent: FileHandle = dataRoot): T {
		return try {
			log.trace {
				"""
				Try to load the following from the json cache:
					key:    $key
					type:   ${type.simpleName}
					parent: $parent
				""".trimIndent()
			}

			if (jsonCache.containsKey(key)) { // Data exists in cache
				log.trace { "Found key in json cache: $key" }
				@Suppress("UNCHECKED_CAST")
				(jsonCache.get(key) as CacheObj<T>).let {
					// Update cache when file is newer than the cached data
					val lastModified = fileCache.get(key)?.lastModified() ?: 0L
					if (it.created < lastModified) {
						log.debug { "File is newer than the cached data, updating cache!" }
						it.obj = json.fromJson(type.java, getCachedHandle(key, parent))
						it.created = lastModified
					}
					it.obj
				}
			} else { // Data does not exists in cache
				log.debug { "Did not find key in json cache: $key" }
				val cachedHandle = getCachedHandle(key, parent)
				if (!cachedHandle.exists()) {
					try {
						cachedHandle.writeBytes(
							json.toJson(ClassReflection.newInstance(type.java)).toByteArray(),
							false
						)
					} catch (e: ReflectionException) {
						log.error("Failed to create class from type: ${type.simpleName}", e)
					}
				}
				json.fromJson(type.java, cachedHandle).also { jsonCache.put(key, CacheObj(it)) }
			}
		} catch (e: SerializationException) {
			log.error { "Failed to load key: $key" }
			log.debug { "Fallback to new instance creation!" }
			// Overwrite corrupted file with new instance
			ClassReflection.newInstance(type.java).also { save(key, it) }
		}
	}

	fun <T : Any> load(key: String, type: KClass<out T>, target: FileHandle = dataRoot): T? {
		return getCachedHandle(key, target).let { if (it.exists()) json.fromJson(type.java, it.readString()) else null }
	}

	fun getCachedHandle(key: String, parent: FileHandle = dataRoot): FileHandle {
		if (!fileHandleCache.containsKey(key)) {
			var child: FileHandle = parent.child(key)
			if (!child.exists()) {
				val fileHandle2 = Gdx.files.external(GLOBAL_DATA_FOLDER_NAME + key)
				if (fileHandle2.exists()) {
					child = fileHandle2
				}
			}
			fileHandleCache.put(key, child)
			fileCache.put(key, child.file())
		}
		return fileHandleCache.get(key)
	}

	fun getCachedHandle(key: MetaDataKey<*>, parent: FileHandle = dataRoot): FileHandle =
		getCachedHandle(key.name, parent)

	fun has(name: String, fileHandle: FileHandle = dataRoot): Boolean {
		return fileHandleCache.containsKey(name) || fileHandle.child(name).exists()
	}

	fun has(key: MetaDataKey<*>, fileHandle: FileHandle = dataRoot): Boolean = has(key.name, fileHandle)

	companion object {
		const val GLOBAL_DATA_FOLDER_NAME: String = ".meta"
	}
}

inline class MetaDataKey<T : Any>(val name: String)

inline operator fun <reified T : Any> MetaData.get(key: String): T = get(key, T::class)
inline operator fun <reified T : Any> MetaData.get(key: MetaDataKey<T>): T = get(key.name)

inline fun <reified T : Any> MetaData.load(key: MetaDataKey<T>): T? = load(key.name, T::class)