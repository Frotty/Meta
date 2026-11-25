@file:JvmName("HashUtils")

package de.fatox.meta.api.crypto

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

/**
 * Computes a hash over this channel's entire content, from offset 0, and restores the caller's position.
 *
 * Streams in [HASH_IO_CHUNK_SIZE] slices, keeping memory use bounded and continuing after short
 * [ReadableByteChannel.read] calls.
 */
@Throws(IOException::class)
fun SeekableByteChannel.hash(): XX64Hash {
	val callerPosition = position()
	try {
		position(0)
		val streamingHash = StreamingXXH64()
		val chunk = ByteArray(HASH_IO_CHUNK_SIZE)
		val buffer = ByteBuffer.wrap(chunk)
		while (true) {
			buffer.clear()
			// A blocking channel returns -1 at the end and never 0 for a buffer with room; treat both as the end.
			val read = read(buffer)
			if (read <= 0) break
			streamingHash.update(chunk, 0, read)
		}
		return streamingHash.digest()
	} finally {
		position(callerPosition)
	}
}

private const val HASH_IO_CHUNK_SIZE = 64 * 1024

/**
 * Validates the hash at the end of [input].
 *
 * Note that it is assumed that the last [HASH_LENGTH] bytes of input are the hash and the other bytes the content where
 * the hash was computed from.
 *
 * @param input The file bytes to be verified.
 * @throws IllegalArgumentException if [input] is shorter than the hash trailer.
 * @throws IllegalStateException if the computed hash differs from the trailer.
 */
@OptIn(ExperimentalStdlibApi::class)
@Throws(IllegalStateException::class)
fun checkHash(input: ByteArray) {
	require(input.size >= HASH_LENGTH) { "Hashed data is shorter than its hash trailer." }
	val inputBuffer = ByteBuffer.wrap(input)
	val newHash = XXH64(inputBuffer, length = input.size - HASH_LENGTH, 0UL).value
	checkHash(input, XX64Hash(newHash))
}

@OptIn(ExperimentalStdlibApi::class)
internal fun checkHash(input: ByteArray, newHash: XX64Hash) {
	require(input.size >= HASH_LENGTH) { "Hashed data is shorter than its hash trailer." }
	val oldHash = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN).getLong(input.size - HASH_LENGTH).toULong()
	check(newHash.value == oldHash) {
		"""
		Game files are invalid.
		${oldHash.toHexString()} (expected)
		${newHash.value.toHexString()} (actual)
		""".trimIndent()
	}
}

/** Computes a hash from the data in the given [String]. */
fun String.hash(): XX64Hash = encodeToByteArray().hash()

/** Computes a hash from the data in the given [ByteArray]. */
fun ByteArray.hash(): XX64Hash = XXH64(ByteBuffer.wrap(this), length = this.size, 0UL)

fun XX64Hash.verify(other: XX64Hash): Boolean = value == other.value
