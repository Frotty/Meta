package de.fatox.meta.injection

class MetastasisException : RuntimeException {
    internal constructor(message: String?) : super(message) {}
    internal constructor(message: String?, cause: Throwable?) : super(message, cause) {}
}