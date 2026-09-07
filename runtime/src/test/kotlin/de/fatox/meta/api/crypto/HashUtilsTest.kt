package de.fatox.meta.api.crypto

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import java.nio.ByteBuffer
import kotlin.streams.asStream
import kotlin.test.assertEquals

// Hashes created with: https://asecuritysite.com/encryption/xxhash
@Suppress("SpellCheckingInspection")
private val testVectors = sequenceOf(
	"" to "ef46db3751d8e999",
	"abc" to "44bc2cf5ad770999",
	"test" to "4fdcca5ddb678139",
	"123456789ABCDEF12" to "880a293145b975a0",
	"abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq" to "f06103773e8585df",
	"abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu" to "bafc02122ded1d21",
)

internal class HashUtilsTest {
	/** XXH64 used to set the caller's limit and byte order and restore only the limit. */
	@Test
	fun `one-shot hash does not disturb the caller's buffer`() {
		val input = ByteArray(200) { index -> (index * 7).toByte() }
		val buffer = ByteBuffer.wrap(input).order(java.nio.ByteOrder.BIG_ENDIAN)
		buffer.position(64)
		buffer.limit(128)

		val hash = XXH64(buffer, input.size, 0UL)

		assertEquals(64, buffer.position(), "position must be untouched")
		assertEquals(128, buffer.limit(), "limit must be untouched")
		assertEquals(java.nio.ByteOrder.BIG_ENDIAN, buffer.order(), "byte order must be untouched")
		// Hashing is over [0, length) regardless of the caller's window, so a fresh wrap agrees.
		assertEquals(XXH64(ByteBuffer.wrap(input), input.size, 0UL), hash)
	}

	/**
	 * The channel overload allocated a buffer the size of the channel, cast that size to Int, and honoured a single
	 * read - so a short read or a non-zero starting position hashed trailing zeroes.
	 */
	@Test
	fun `channel hash streams the whole channel and restores position`() {
		for (length in intArrayOf(0, 1, 31, 32, 33, 65_536, 200_000)) {
			val content = ByteArray(length) { index -> (index * 29 + length).toByte() }
			// Hash trailers are not part of this contract, so pad past the channel's HASH_LENGTH accounting.
			val backing = content + ByteArray(HASH_LENGTH)
			val channel = ShortReadChannel(backing, contentLength = length)
			channel.position(7L.coerceAtMost(length.toLong()))

			val hashed = channel.hash()

			assertEquals(content.hash(), hashed, "channel hash of $length bytes must match the array hash")
			assertEquals(7L.coerceAtMost(length.toLong()), channel.position(), "caller position must be restored")
		}
	}

	/** Returns at most 13 bytes per read, so a single-read implementation cannot pass. */
	private class ShortReadChannel(
		private val data: ByteArray,
		private val contentLength: Int,
	) : java.nio.channels.SeekableByteChannel {
		private var pos = 0
		private var open = true

		override fun read(dst: ByteBuffer): Int {
			val available = contentLength - pos
			if (available <= 0) return -1
			val count = minOf(dst.remaining(), available, 13)
			dst.put(data, pos, count)
			pos += count
			return count
		}

		override fun write(src: ByteBuffer): Int = throw UnsupportedOperationException()
		override fun position(): Long = pos.toLong()
		override fun position(newPosition: Long): java.nio.channels.SeekableByteChannel {
			pos = newPosition.toInt()
			return this
		}

		override fun size(): Long = contentLength.toLong()
		override fun truncate(size: Long): java.nio.channels.SeekableByteChannel = throw UnsupportedOperationException()
		override fun isOpen(): Boolean = open
		override fun close() {
			open = false
		}
	}

	@Test
	fun `streaming hash matches one-shot hash across chunk boundaries`() {
		val inputs = ArrayList<ByteArray>()
		for (length in 0..257) {
			inputs.add(ByteArray(length) { index -> (index * 131 + length * 17).toByte() })
		}
		val chunkSizes = intArrayOf(1, 3, 7, 31, 32, 33, 64, 127)
		for (input in inputs) {
			val expected = XXH64(ByteBuffer.wrap(input), input.size, 0UL)
			for (chunkSize in chunkSizes) {
				val streaming = StreamingXXH64()
				var offset = 0
				while (offset < input.size) {
					val length = minOf(chunkSize, input.size - offset)
					streaming.update(input, offset, length)
					offset += length
				}
				assertEquals(expected, streaming.digest(), "length=${input.size}, chunkSize=$chunkSize")
			}
		}
	}

	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@Nested
	internal inner class SelfTest {
		private fun strings(): Stream<Arguments> = testVectors.map { arguments(it.first) }.asStream()
		private fun byteArrays(): Stream<Arguments> = testVectors.map { arguments(it.first.toByteArray()) }.asStream()
		private fun both(): Stream<Arguments> =
			testVectors.map { arguments(it.first, it.first.toByteArray()) }.asStream()

		@ParameterizedTest
		@MethodSource("strings")
		fun `self test with String`(message: String) {
			assertEquals(message.hash(), message.hash())
		}

		@ParameterizedTest
		@MethodSource("byteArrays")
		fun `self test with ByteArray`(message: ByteArray) {
			assertTrue(message.hash().verify(message.hash()))
		}

		@ParameterizedTest
		@MethodSource("both")
		fun `self test with String - ByteArray`(message: String, expected: ByteArray) {
			assertTrue(message.hash().verify(expected.hash()))
		}

		@ParameterizedTest
		@MethodSource("both")
		fun `self test with ByteArray - String`(expected: String, message: ByteArray) {
			assertTrue(message.hash().verify(expected.hash()))
		}
	}

	@Nested
	internal inner class HashTest {
		private fun testVectors(): Stream<Arguments> = testVectors.map { arguments(it.first, it.second) }.asStream()

		@OptIn(ExperimentalStdlibApi::class)
		@ParameterizedTest
		@MethodSource("testVectors")
		fun `hash message and compare hex strings`(message: String, expected: String) {
			assertEquals(expected, message.hash().value.toHexString())
		}
	}
}
