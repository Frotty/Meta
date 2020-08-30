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

inline fun Logger.info(msg: () -> String) {
	if (isInfoEnabled) info(msg())
}

inline fun Logger.warn(msg: () -> String) {
	if (isWarnEnabled) warn(msg())
}

inline fun Logger.error(msg: () -> String) {
	if (isErrorEnabled) error(msg())
}

inline fun Logger.error(t: Throwable, msg: () -> String) {
	if (isErrorEnabled) error(msg(), t)
}

inline fun Logger.debug(msg: () -> String) {
	if (isDebugEnabled) debug(msg())
}

inline fun Logger.trace(msg: () -> String) {
	if (isTraceEnabled) trace(msg())
}
