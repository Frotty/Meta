package de.fatox.meta.input

import de.fatox.meta.metaUiInputProfileKey
import de.fatox.meta.assets.MetaData
import de.fatox.meta.assets.MetaDataKey
import de.fatox.meta.assets.read
import de.fatox.meta.assets.valueOrNull

fun MetaUiInputBindings.saveProfile(
	metaData: MetaData,
	key: MetaDataKey<MetaUiInputProfile> = metaUiInputProfileKey,
): MetaUiInputProfile =
	toProfile().also { metaData.save(key, it) }

/**
 * Applies the stored profile, or the defaults when there is none.
 *
 * Reads through [MetaData.StoredValue] rather than a nullable, because the two reasons for having no profile call for
 * different things. Nothing stored is just a fresh install. A profile that exists but could not be read - a scanner or
 * a sync client holding it for a moment - must be left exactly where it is: writing the defaults over it, which this
 * did while `load` conflated the two, throws away bindings the player set deliberately.
 *
 * Either way nothing is written here. The profile is persisted when it changes, by [saveProfile].
 */
fun MetaUiInputBindings.loadProfile(
	metaData: MetaData,
	key: MetaDataKey<MetaUiInputProfile> = metaUiInputProfileKey,
): MetaUiInputProfile {
	val profile = metaData.read(key).valueOrNull ?: MetaUiInputProfile.defaults()
	applyProfile(profile)
	return profile
}
