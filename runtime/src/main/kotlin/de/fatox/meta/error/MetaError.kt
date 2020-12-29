package de.fatox.meta.error

abstract class MetaError(val name: String, private val errorDescription: String) {
	abstract fun gotoError()
}