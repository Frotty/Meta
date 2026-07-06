package de.fatox.meta.input

import de.fatox.meta.metaUiInputProfileKey
import de.fatox.meta.assets.MetaData
import de.fatox.meta.assets.MetaDataKey
import de.fatox.meta.assets.load

fun MetaUiInputBindings.saveProfile(
	metaData: MetaData,
	key: MetaDataKey<MetaUiInputProfile> = metaUiInputProfileKey,
): MetaUiInputProfile =
	toProfile().also { metaData.save(key, it) }

fun MetaUiInputBindings.loadProfile(
	metaData: MetaData,
	key: MetaDataKey<MetaUiInputProfile> = metaUiInputProfileKey,
): MetaUiInputProfile {
	val profile = metaData.load(key) ?: MetaUiInputProfile.defaults().also { metaData.save(key, it) }
	applyProfile(profile)
	return profile
}
