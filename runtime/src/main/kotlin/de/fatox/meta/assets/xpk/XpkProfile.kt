package de.fatox.meta.assets.xpk

import java.security.PublicKey
import javax.crypto.spec.SecretKeySpec

/**
 * The per-game parameters an XPK v2 archive is built and read with.
 *
 * Meta ships the mechanism; the game ships the parameters. Nothing here has a default, and Meta binds no profile, so
 * this repository being public gives away the container's *shape* and nothing about any particular game's instance of
 * it. A generic extractor written from this source still needs the constants below, which live in the consuming
 * game's repository.
 *
 * This is cost-raising, not security: the key is materialised in the process, so a debugger or a `javax.crypto` hook
 * recovers it. The one exception is [tocSigningKey] - see `docs/xpk-format-audit.md` §8 for the honest limits.
 *
 * Instances are immutable and safe to share. Build one with [XpkProfile.of], which copies the key material so a
 * caller cannot mutate a live profile's arrays.
 */
class XpkProfile private constructor(
	rootKey: ByteArray,
	nameHashKey: ByteArray,
	/** Distinguishes archives built with different parameter sets; recorded in the footer. */
	val profileId: Int,
	/** Mixed into the footer obfuscation so its layout is not the same in two games. */
	val footerMask: Long,
	/** Verifies the table of contents. Absent means unsigned archives are accepted - development only. */
	val tocSigningKey: PublicKey?,
) {
	/**
	 * Key objects built once.
	 *
	 * Every block read and every name lookup needs one of these; handing out `ByteArray` copies instead would
	 * allocate a key per operation on the asset-loading path for no benefit, since neither the reader nor the writer
	 * ever wants the raw bytes. `SecretKeySpec` copies what it is given, so the caller's arrays are not retained.
	 */
	internal val aesKey: SecretKeySpec = SecretKeySpec(rootKey, "AES")
	internal val macKey: SecretKeySpec = SecretKeySpec(rootKey, "HmacSHA256")
	internal val nameHashMacKey: SecretKeySpec = SecretKeySpec(nameHashKey, "HmacSHA256")

	override fun toString(): String = "XpkProfile(profileId=$profileId, signed=${tocSigningKey != null})"

	companion object {
		/** AES-256. */
		const val ROOT_KEY_LENGTH: Int = 32

		/** Anything shorter weakens the keyed name hash for no saving. */
		const val NAME_HASH_KEY_LENGTH: Int = 32

		fun of(
			rootKey: ByteArray,
			nameHashKey: ByteArray,
			profileId: Int,
			footerMask: Long,
			tocSigningKey: PublicKey? = null,
		): XpkProfile {
			require(rootKey.size == ROOT_KEY_LENGTH) {
				"Root key must be $ROOT_KEY_LENGTH bytes, was ${rootKey.size}"
			}
			require(nameHashKey.size == NAME_HASH_KEY_LENGTH) {
				"Name hash key must be $NAME_HASH_KEY_LENGTH bytes, was ${nameHashKey.size}"
			}
			require(profileId in 0..0xFFFF) { "Profile id must fit in 16 bits, was $profileId" }
			return XpkProfile(rootKey, nameHashKey, profileId, footerMask, tocSigningKey)
		}
	}
}
