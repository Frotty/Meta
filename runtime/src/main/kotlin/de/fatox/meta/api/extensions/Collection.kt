@file:Suppress("NOTHING_TO_INLINE", "unused")

package de.fatox.meta.api.extensions

import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.LongMap
import com.badlogic.gdx.utils.ObjectMap

// Taken from kotlin source
inline fun <K, V> ObjectMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
	val value = get(key)
	return if (value == null) {
		val answer = defaultValue()
		put(key, answer)
		answer
	} else {
		value
	}
}

// Taken from kotlin source
inline fun <V> LongMap<V>.getOrPut(key: Long, defaultValue: () -> V): V {
	val value = get(key)
	return if (value == null) {
		val answer = defaultValue()
		put(key, answer)
		answer
	} else {
		value
	}
}

// Taken from kotlin source
inline fun <V> IntMap<V>.getOrPut(key: Int, defaultValue: () -> V): V {
	val value = get(key)
	return if (value == null) {
		val answer = defaultValue()
		put(key, answer)
		answer
	} else {
		value
	}
}

inline operator fun <K, V> ObjectMap<K, V>.set(key: K, value: V): V = put(key, value)
inline operator fun <V> LongMap<V>.set(key: Long, value: V): V = put(key, value)
inline operator fun <V> IntMap<V>.set(key: Int, value: V): V = put(key, value)