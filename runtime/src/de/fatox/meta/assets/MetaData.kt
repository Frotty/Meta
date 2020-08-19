package de.fatox.meta.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.TimeUtils
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.badlogic.gdx.utils.reflect.ReflectionException
import de.fatox.meta.Meta.Companion.inject
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import java.io.File

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
	private val jsonCache = ObjectMap<String, CacheObj<Any?>>()
	private val dataRoot: FileHandle
	private val json = Json()
	fun save(key: String, obj: Any?) {
		save(dataRoot, key, obj)
	}

	fun save(target: FileHandle?, key: String, obj: Any?): FileHandle {
		val jsonString = json.toJson(obj)
		val fileHandle = getCachedHandle(target, key)
		fileHandle.writeBytes(jsonString.toByteArray(), false)
		val cacheObj: CacheObj<Any?>? = jsonCache.get(key)
		if (cacheObj != null) {
			cacheObj.created = TimeUtils.millis()
			cacheObj.obj = obj
		}
		return fileHandle
	}

	/** Loads and caches the filehandle descripted by the path, if it exists  */
	operator fun get(key: String): FileHandle {
		return getCachedHandle(dataRoot, key)
	}

	/** Caches and returns this object loaded from json at the default location  */
	operator fun <T> get(type: Class<T>): T {
		return getCachedJson(dataRoot, type.javaClass.simpleName, type)
	}

	/** Caches and returns this object loaded from json at the specified location  */
	operator fun <T> get(key: String, type: Class<T>): T {
		return getCachedJson(dataRoot, key, type)
	}

	private fun <T> getCachedJson(parent: FileHandle, key: String, type: Class<T>): T {
		val jsonHandle: T
		if (jsonCache.containsKey(key)) {
			val cacheObj = jsonCache.get(key) as CacheObj<T>
			val lastModified = getCachedFile(key)!!.lastModified()
			if (cacheObj.created < lastModified) {
				cacheObj.obj = json.fromJson(type, getCachedHandle(parent, key))
				cacheObj.created = lastModified
			}
			jsonHandle = cacheObj.obj
		} else {
			val cachedHandle = getCachedHandle(parent, key)
			if (!cachedHandle.exists()) {
				try {
					cachedHandle.writeBytes(json.toJson(ClassReflection.newInstance(type)).toByteArray(), false)
				} catch (e: ReflectionException) {
					e.printStackTrace()
				}
			}
			jsonHandle = json.fromJson(type, cachedHandle)
			jsonCache.put(key, CacheObj(jsonHandle))
		}
		return jsonHandle
	}

	operator fun <T> get(target: FileHandle?, key: String, type: Class<T>?): T? {
		val fileHandle = getCachedHandle(target, key)
		return if (fileHandle != null && fileHandle.exists()) {
			json.fromJson(type, fileHandle.readString())
		} else null
	}

	fun getCachedHandle(key: String): FileHandle {
		return getCachedHandle(dataRoot, key)
	}

	fun getCachedFile(key: String): File? {
		return if (fileCache.containsKey(key)) {
			fileCache.get(key)
		} else null
	}

	fun getCachedHandle(parent: FileHandle?, key: String): FileHandle {
		var fileHandle: FileHandle
		if (fileHandleCache.containsKey(key)) {
			fileHandle = fileHandleCache.get(key)
		} else {
			fileHandle = parent!!.child(key)
			if (!fileHandle.exists()) {
				val fileHandle2 = Gdx.files.external(GLOBAL_DATA_FOLDER_NAME + key)
				if (fileHandle2.exists()) {
					fileHandle = fileHandle2
				}
			}
			fileHandleCache.put(key, fileHandle)
			fileCache.put(key, fileHandle.file())
		}
		return fileHandle
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
		inject(this)
		dataRoot = Gdx.files.external(".$gameName").child(GLOBAL_DATA_FOLDER_NAME)
		dataRoot.mkdirs()
	}
}