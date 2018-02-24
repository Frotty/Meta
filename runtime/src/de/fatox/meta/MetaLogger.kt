package de.fatox.meta

import de.fatox.meta.api.Logger

class MetaLogger : Logger {
    var logLevel: Logger.LogLevel = Logger.LogLevel.TRACE

    override fun trace(tag: String, text: String) {
        if (logLevel.ordinal <= Logger.LogLevel.TRACE.ordinal) {
            println("[trace] - $tag : $text")
        }
    }

    override fun info(tag: String, text: String) {
        if (logLevel.ordinal <= Logger.LogLevel.INFO.ordinal) {
            println("[info] - $tag : $text")
        }
    }

    override fun debug(tag: String, text: String) {
        if (logLevel.ordinal <= Logger.LogLevel.DEBUG.ordinal) {
            println("[debug] - $tag : $text")
        }
    }

    override fun warning(tag: String, text: String) {
        if (logLevel.ordinal <= Logger.LogLevel.WARNING.ordinal) {
            println("[warning] - $tag : $text")
        }
    }

    override fun error(tag: String, text: String) {
        if (logLevel.ordinal <= Logger.LogLevel.ERROR.ordinal) {
            System.err.println("[error] - $tag : $text")
        }
    }
}
