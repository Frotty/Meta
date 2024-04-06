package de.fatox.meta.api.crypto

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
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
