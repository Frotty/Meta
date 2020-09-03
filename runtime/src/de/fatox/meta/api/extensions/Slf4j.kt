package de.fatox.meta.api.extensions

import org.slf4j.Logger
import org.slf4j.LoggerFactory

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

@PublishedApi
@Suppress("NOTHING_TO_INLINE")
internal inline fun (() -> Any?).safeToString(): String {
	return try {
		invoke().toString()
	} catch (t: Throwable) {
		"Log message invocation failed: $t"
	}
}

inline fun Logger.info(msg: () -> Any?) {
	if (isInfoEnabled) info(msg.safeToString())
}

inline fun Logger.warn(msg: () -> Any?) {
	if (isWarnEnabled) warn(msg.safeToString())
}

inline fun Logger.error(msg: () -> Any?) {
	if (isErrorEnabled) error(msg.safeToString())
}

inline fun Logger.error(t: Throwable, msg: () -> Any?) {
	if (isErrorEnabled) error(msg.safeToString(), t)
}

inline fun Logger.debug(msg: () -> Any?) {
	if (isDebugEnabled) debug(msg.safeToString())
}

inline fun Logger.trace(msg: () -> Any?) {
	if (isTraceEnabled) trace(msg.safeToString())
}
