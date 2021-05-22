@file:JvmName("HashUtils")

package de.fatox.meta.api.crypto

import java.io.IOException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.security.MessageDigest

/**
 * The length of the used digest algorithm in bytes.
 * @see [MessageDigest.getDigestLength]
 */
const val HASH_LENGTH: Int = 20

/** The used digest algorithm. */
private const val HASH_ALGORITHM = "SHA-1"

/** Number of bytes processed at once from [hash]. */
private const val KIBIBYTE_16: Int = 1024 * 16

private val buffer: ByteBuffer = ByteBuffer.allocate(KIBIBYTE_16)
private val mDigest: MessageDigest = MessageDigest.getInstance(HASH_ALGORITHM)

@Throws(IllegalStateException::class)
private fun MessageDigest.resetAndDigest(
	input: ByteArray,
	output: ByteArray = ByteArray(HASH_LENGTH),
	inputOffset: Int = 0,
	inputLength: Int = input.size,
	outputOffset: Int = 0,
): ByteArray {
	reset()
	update(input, inputOffset, inputLength)
	val writtenBytes = digest(output, outputOffset, HASH_LENGTH)

	// Sanity check for the current state. Since we only allow SHA-1 the hash length is predetermined and this check
	// MUST always succeed.
	check(writtenBytes == HASH_LENGTH)

	return output
}

@JvmInline
value class Hash(val value: ByteArray)
@JvmInline
value class Base64EncodedHash(val value: String)
@JvmInline
value class HexEncodedHash(val value: String)

/**
 * Converts a [byte array][ByteArray] to a hex string.
 *
 * @receiver ByteArray to convert
 * @return A hex string representing the given bytes.
 * @see <a href="https://stackoverflow.com/a/2149927">Stackoverflow</a>
 */
private fun ByteArray.toHexString(): String = "%040x".format(BigInteger(1, this))

/** Computes a hash from the data in the given [ReadableByteChannel]. */
@Throws(IOException::class)
fun ReadableByteChannel.hash(): Hash {
	buffer.rewind()
	mDigest.reset()
	while (read(buffer) != -1) {
		buffer.flip()
		mDigest.update(buffer)
		buffer.clear()
	}
	return Hash(mDigest.digest())
}

/**
 * Validates the hash at the end of [input].
 *
 * Note that it is assumed that the last [HASH_LENGTH] bytes of input are the hash and the other bytes the content where
 * the hash was computed from.
 *
 * @param input The file bytes to be verified.
 * @throws IllegalStateException if the hash is invalid.
 */
@Throws(IllegalStateException::class)
fun requireValidHash(input: ByteArray) {
	val newHash = mDigest.resetAndDigest(input, inputLength = input.size - HASH_LENGTH)
	val oldHash = input.copyOfRange(input.size - HASH_LENGTH, input.size)
	check(newHash.contentEquals(oldHash)) {
		"""
		Game files are invalid.
		${oldHash.toHexString()} (expected)
		${newHash.toHexString()} (actual)
		""".trimIndent()
	}
}

/** Computes a hash from the data in the given [String]. */
fun String.hash(): Hash = toByteArray(Charsets.UTF_8).hash()

/** Computes a hash from the data in the given [ByteArray]. */
fun ByteArray.hash(): Hash = Hash(mDigest.resetAndDigest(this))

/** Returns `true` if [this hash][this] equals the [expected hash][expected], `false` otherwise */
fun Hash.verify(expected: Hash): Boolean = value.contentEquals(expected.value)
