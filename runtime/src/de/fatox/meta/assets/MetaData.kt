package de.fatox.meta.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.TimeUtils
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.badlogic.gdx.utils.reflect.ReflectionException
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import java.io.File
import kotlin.reflect.KClass

/**
 * Created by Frotty on 10.03.2017.
 * Handles MetaData needs.
 * De-/serializes config classes from a .meta sub folder in the game's data folder inside user home
 * MetaData can be accessed via #.get and will be cached.
 */
class MetaData {
	internal class CacheObj<T>(var obj: T) {
		var created = TimeUtils.millis()
	}

	private val gameName: String by lazyInject("gameName")

	private val fileHandleCache = ObjectMap<String, FileHandle>()
	private val fileCache = ObjectMap<String, File>()
	private val jsonCache = ObjectMap<String, CacheObj<Any>>()
	private val dataRoot: FileHandle
	private val json = Json()
	fun save(key: String, obj: Any) {
		save(dataRoot, key, obj)
	}

	fun save(target: FileHandle, key: String, obj: Any): FileHandle {
		val jsonString = json.toJson(obj)
		val fileHandle = getCachedHandle(key, target)
		fileHandle.writeBytes(jsonString.toByteArray(), false)
		val cacheObj: CacheObj<Any>? = jsonCache.get(key)
		if (cacheObj != null) {
			cacheObj.created = TimeUtils.millis()
			cacheObj.obj = obj
		}
		return fileHandle
	}

	/** Caches and returns this object loaded from json at the specified location  */
	operator fun <T : Any> get(key: String, type: KClass<out T>): T {
		return getCachedJson(key, type, dataRoot)
	}

	private fun <T : Any> getCachedJson(key: String, type: KClass<out T>, parent: FileHandle = dataRoot): T {
		val jsonHandle: T
		if (jsonCache.containsKey(key)) {
			val cacheObj = jsonCache.get(key) as CacheObj<T>
			val lastModified = getCachedFile(key)!!.lastModified()
			if (cacheObj.created < lastModified) {
				cacheObj.obj = json.fromJson(type.java, getCachedHandle(key, parent))
				cacheObj.created = lastModified
			}
			jsonHandle = cacheObj.obj
		} else {
			val cachedHandle = getCachedHandle(key, parent)
			if (!cachedHandle.exists()) {
				try {
					cachedHandle.writeBytes(json.toJson(ClassReflection.newInstance(type.java)).toByteArray(), false)
				} catch (e: ReflectionException) {
					e.printStackTrace()
				}
			}
			jsonHandle = json.fromJson(type.java, cachedHandle)
			jsonCache.put(key, CacheObj(jsonHandle as Any))
		}
		return jsonHandle
	}

	operator fun <T> get(target: FileHandle, key: String, type: Class<T>?): T? {
		val fileHandle = getCachedHandle(key, target)
		return if (fileHandle.exists()) {
			json.fromJson(type, fileHandle.readString())
		} else null
	}

	fun getCachedHandle(key: String): FileHandle {
		return getCachedHandle(key, dataRoot)
	}

	fun getCachedFile(key: String): File? {
		return if (fileCache.containsKey(key)) {
			fileCache.get(key)
		} else null
	}

	fun getCachedHandle(key: String, parent: FileHandle = dataRoot): FileHandle {
		if (!fileHandleCache.containsKey(key)) {
			var fileHandle: FileHandle = parent.child(key)
			if (!fileHandle.exists()) {
				val fileHandle2 = Gdx.files.external(GLOBAL_DATA_FOLDER_NAME + key)
				if (fileHandle2.exists()) {
					fileHandle = fileHandle2
				}
			}
			fileHandleCache.put(key, fileHandle)
			fileCache.put(key, fileHandle.file())
		}
		return fileHandleCache.get(key)
	}

	fun has(name: String): Boolean {
		return has(dataRoot, name)
	}

	fun has(fileHandle: FileHandle, name: String): Boolean {
		return fileHandleCache.containsKey(name) || fileHandle.child(name).exists()
	}

	companion object {
		const val GLOBAL_DATA_FOLDER_NAME = ".meta"
	}

	init {
		dataRoot = Gdx.files.external(".$gameName").child(GLOBAL_DATA_FOLDER_NAME)
		dataRoot.mkdirs()
	}
}

inline operator fun <reified T : Any> MetaData.get(key: String = T::class.simpleName!!): T = get(key, T::class)