package de.fatox.meta.error

import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

class MetaErrorHandler {
	private val languageBundle: LanguageBundle by lazyInject()

	private val errors = Array<MetaError>()
	fun add(metaError: MetaError) {
		errors.add(metaError)
	}

	fun hasErrors(): Boolean {
		return errors.size > 0
	}

	val labelText: String
		get() = if (hasErrors()) {
			if (errors.size > 1) {
				languageBundle.format("error.found", errors.size)
			} else {
				errors[0].name
			}
		} else ""
}