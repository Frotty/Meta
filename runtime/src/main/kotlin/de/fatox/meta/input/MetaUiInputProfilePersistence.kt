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

/**
 * Applies the stored profile, or the defaults when there is none.
 *
 * Loading does not save. Writing the defaults here looked harmless - it only ran when nothing was stored - but
 * `MetaData.load` answers `null` both for "nothing is stored" and for "this could not be read right now", and a file
 * held briefly by a scanner or a sync client is the second. Treating that as the first replaced a perfectly good set
 * of bindings with the defaults, which is the loss this store is meant to prevent. The profile is persisted when it
 * is changed, by [saveProfile].
 */
fun MetaUiInputBindings.loadProfile(
	metaData: MetaData,
	key: MetaDataKey<MetaUiInputProfile> = metaUiInputProfileKey,
): MetaUiInputProfile {
	val profile = metaData.load(key) ?: MetaUiInputProfile.defaults()
	applyProfile(profile)
	return profile
}
