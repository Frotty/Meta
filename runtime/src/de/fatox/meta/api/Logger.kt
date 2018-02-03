package de.fatox.meta.api

interface Logger {
    enum class LogLevel {
        TRACE, DEBUG, INFO, WARNING, ERROR
    }

    fun trace(tag: String, text: String)

    fun info(tag: String, text: String)

    fun debug(tag: String, text: String)

    fun warning(tag: String, text: String)

    fun error(tag: String, text: String)
}
