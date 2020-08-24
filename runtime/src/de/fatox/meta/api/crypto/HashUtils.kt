@file:Suppress("MemberVisibilityCanBePrivate") // public utility class

package de.fatox.meta.api.crypto

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.security.MessageDigest

/**
 * Basic hash utils for computing and verifying hashes for game files.
 */
object HashUtils {
	/**
	 * The length of the used digest algorithm in bytes.
	 * @see [MessageDigest.getDigestLength]
	 */
	const val HASH_LENGTH = 20

	/** The used digest algorithm. */
	private const val HASH_ALGORITHM = "SHA-1"

	/** Number of bytes processed at once from [computeHash]. */
	private const val KIBIBYTE_16: Int = 1024 * 16 // TODO why this number?

	private val buffer: ByteBuffer = ByteBuffer.allocate(KIBIBYTE_16)
	private val digest: MessageDigest = MessageDigest.getInstance(HASH_ALGORITHM)

	/**
	 * Computes a hash from the data in the given [channel].
	 *
	 * @param channel ReadableByteChannel
	 * @return The hash from the given [channel].
	 */
	fun computeHash(channel: ReadableByteChannel): ByteArray {
		buffer.rewind()
		digest.reset()
		while (channel.read(buffer) != -1) {
			buffer.flip()
			digest.update(buffer)
			buffer.clear()
		}
		return digest.digest()
	}

	/**
	 * Validates the hash at the end of [input].
	 *
	 * Note that it is assumed that the last [HASH_LENGTH] bytes of input are the hash and the other bytes the content
	 * where the hash was computed from.
	 *
	 * @param input The file bytes to be verified.
	 * @throws IllegalStateException if the hash is invalid.
	 */
	fun requireValidHash(input: ByteArray) {
		digest.reset()
		digest.update(input, 0, input.size - HASH_LENGTH)
		val newHash = digest.digest()
		val oldHash = input.copyOfRange(input.size - HASH_LENGTH, input.size)
		check(newHash.contentEquals(oldHash))
		{
			"""
			Game files are invalid.
			${oldHash.toHexString()} (expected)
			${newHash.toHexString()} (actual)""".trimIndent() // TODO do we need to keep this in "production"?
		}
	}

	/**
	 * Converts a [byte array][ByteArray] to a hex string.
	 *
	 * @receiver ByteArray to convert
	 * @return A hex string representing the given bytes.
	 * @see <a href="https://stackoverflow.com/a/2149927">Stackoverflow</a>
	 */
	private fun ByteArray.toHexString(): String = "%040x".format(BigInteger(1, this))
}
