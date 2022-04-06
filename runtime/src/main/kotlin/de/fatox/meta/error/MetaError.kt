package de.fatox.meta.error

open class MetaError(val name: String, private val errorDescription: String) {
	open fun gotoError() {
		// NOP
	}
}