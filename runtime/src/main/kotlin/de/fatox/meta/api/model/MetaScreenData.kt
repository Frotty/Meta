package de.fatox.meta.api.model

import com.badlogic.gdx.utils.Array

data class MetaScreenData(var name: String) {
	var windowData: Array<MetaWindowData> = Array<MetaWindowData>(4)
}
