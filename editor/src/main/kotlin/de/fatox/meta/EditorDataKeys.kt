package de.fatox.meta

import com.badlogic.gdx.utils.Array
import de.fatox.meta.assets.MetaDataKey

// The module-specific filename keeps this Kotlin file facade distinct from the runtime's MetaDataKeysKt.
val lastProjectsKey = MetaDataKey<Array<String>>("lastProjects")
