@file:Suppress("NOTHING_TO_INLINE", "unused")

package de.fatox.meta.api.extensions

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.IntSet
import com.badlogic.gdx.utils.LongMap
import com.badlogic.gdx.utils.ObjectMap

/**
 * Iterates by index instead of using libGDX's cached [Array] iterator.
 *
 * This is allocation-free and safe to nest over the same array. The size is captured before the first callback,
 * matching `for (index in 0 until array.size)` semantics.
 */
inline fun <T> Array<T>.forEachValue(action: (T) -> Unit) {
	val end = size
	var index = 0
	while (index < end) {
		action(get(index))
		index++
	}
}

/**
 * Indexed variant of [forEachValue]. Prefer this or an explicit index range over Kotlin's iterator-based helpers.
 */
inline fun <T> Array<T>.forEachIndexedValue(action: (index: Int, value: T) -> Unit) {
	val end = size
	var index = 0
	while (index < end) {
		action(index, get(index))
		index++
	}
}

/**
 * Uses a fresh iterator rather than libGDX's two cached map iterators, so nesting over the same map is safe.
 *
 * The iterator allocation makes this unsuitable for per-frame paths. Hot maps should keep an indexed [Array] view
 * or use another data layout with direct indexed access.
 */
inline fun <K, V> ObjectMap<K, V>.forEachEntryReentrant(action: (key: K, value: V) -> Unit) {
	val iterator = ObjectMap.Entries(this)
	while (iterator.hasNext()) {
		val entry = iterator.next()
		action(entry.key, entry.value)
	}
}

/** Reentrant map iteration for lifecycle/setup paths; see [forEachEntryReentrant] for the allocation contract. */
inline fun <V> IntMap<V>.forEachEntryReentrant(action: (key: Int, value: V) -> Unit) {
	val iterator = IntMap.Entries<V>(this)
	while (iterator.hasNext()) {
		val entry = iterator.next()
		action(entry.key, entry.value)
	}
}

/** Reentrant map iteration for lifecycle/setup paths; see [forEachEntryReentrant] for the allocation contract. */
inline fun <V> LongMap<V>.forEachEntryReentrant(action: (key: Long, value: V) -> Unit) {
	val iterator = LongMap.Entries<V>(this)
	while (iterator.hasNext()) {
		val entry = iterator.next()
		action(entry.key, entry.value)
	}
}

/** Uses a fresh iterator so nested traversal of the same set cannot invalidate the outer traversal. */
inline fun IntSet.forEachIntReentrant(action: (value: Int) -> Unit) {
	val iterator = IntSet.IntSetIterator(this)
	while (iterator.hasNext) action(iterator.next())
}

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
