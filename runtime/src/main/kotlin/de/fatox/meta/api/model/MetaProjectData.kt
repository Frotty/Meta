package de.fatox.meta.api.model

import com.badlogic.gdx.utils.Array

/**
 * Created by Frotty on 02.06.2016.
 */
data class MetaProjectData(var name: String = "unknown") {

    var openTabs: Array<MetaTabData> = Array()

    val isValid: Boolean get() = !name.isBlank()

    companion object {
        const val PROJECT_FILE_NAME = "metaproject.json"
    }
}
