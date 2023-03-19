@file:Suppress("unused") // public utility class

package de.fatox.meta.api.extensions

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@PublishedApi
internal inline fun safeToString(msg: () -> Any?): String {
	return try {
		msg.invoke().toString()
	} catch (@SuppressWarnings("TooGenericExceptionCaught") e: Exception) { // We need to return a string in any case
		"Log message invocation failed: $e"
	}
}

inline fun Logger.info(msg: () -> Any?) {
	if (isInfoEnabled) info(safeToString(msg))
}

inline fun Logger.warn( msg: () -> Any?) {
	if (isWarnEnabled) warn(safeToString(msg))
}

inline fun Logger.error(msg: () -> Any?) {
	if (isErrorEnabled) error(safeToString(msg))
}

inline fun Logger.error(t: Throwable, msg: () -> Any?) {
	if (isErrorEnabled) error(safeToString(msg), t)
}

inline fun Logger.debug(msg: () -> Any?) {
	if (isDebugEnabled) debug(safeToString(msg))
}

inline fun Logger.trace(msg: () -> Any?) {
	if (isTraceEnabled) trace(safeToString(msg))
}

object MetaLoggerFactory {
	@Suppress("NOTHING_TO_INLINE")
	inline fun logger(noinline context: () -> Unit): Logger {
		val name = context.javaClass.name
		val className = when {
			name.contains("Kt$") -> name.substringBefore("Kt$")
			name.contains("$") -> name.substringBefore("$")
			else -> name
		}

		return LoggerFactory.getLogger(className)
	}
}
