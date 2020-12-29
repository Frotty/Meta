package de.fatox.meta.api.model

import com.badlogic.gdx.utils.Array

/**
 * Created by Frotty on 28.06.2016.
 */
data class MetaScreenData(var name: String) {
	var windowData: Array<MetaWindowData> = Array<MetaWindowData>(4)
}
