package de.fatox.meta.api.model

import com.badlogic.gdx.utils.Array

data class MetaProjectData(var name: String = "unknown") {
	var openTabs: Array<MetaTabData> = Array()

	val isValid: Boolean get() = name.isNotBlank()

	companion object {
		const val PROJECT_FILE_NAME: String = "metaproject.json"
	}
}
