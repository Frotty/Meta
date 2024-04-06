@file:JvmName("HashUtils")

package de.fatox.meta.api.crypto

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.channels.SeekableByteChannel
import java.security.MessageDigest

/**
 * The length of the used digest algorithm in bytes.
 * @see [MessageDigest.getDigestLength]
 */
const val HASH_LENGTH: Int = 8

@JvmInline
value class XX64Hash(val value: ULong)

@JvmInline
value class Base64EncodedHash(val value: String)

/** Computes a hash from the data in the given [ReadableByteChannel]. */
@Throws(IOException::class)
fun SeekableByteChannel.hash(): XX64Hash {
	return XXH64(ByteBuffer.allocate(size().toInt()).also { read(it); it.flip() }, size().toInt(), 0UL)
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
@OptIn(ExperimentalStdlibApi::class)
@Throws(IllegalStateException::class)
fun checkHash(input: ByteArray) {
	val inputBuffer = ByteBuffer.wrap(input)
	val newHash = XXH64(inputBuffer, length = input.size - HASH_LENGTH, 0UL).value
	val oldHash = inputBuffer.getLong().toULong()
	check(newHash == oldHash) {
		"""
		Game files are invalid.
		${oldHash.toHexString()} (expected)
		${newHash.toHexString()} (actual)
		""".trimIndent()
	}
}

/** Computes a hash from the data in the given [String]. */
fun String.hash(): XX64Hash = encodeToByteArray().hash()

/** Computes a hash from the data in the given [ByteArray]. */
fun ByteArray.hash(): XX64Hash = XXH64(ByteBuffer.wrap(this), length = this.size, 0UL)

fun XX64Hash.verify(other: XX64Hash): Boolean = value == other.value