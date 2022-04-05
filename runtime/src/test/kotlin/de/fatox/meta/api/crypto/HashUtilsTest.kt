package de.fatox.meta.api.crypto

import de.fatox.meta.api.encoding.decode
import de.fatox.meta.api.encoding.toHex
import org.junit.jupiter.api.Assertions.assertArrayEquals
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

// Messages and encoded hashes taken from:
// https://csrc.nist.gov/projects/cryptographic-standards-and-guidelines/example-values
// and
// https://www.di-mgt.com.au/sha_testvectors.html
@Suppress("SpellCheckingInspection")
private val testVectors = sequenceOf(
	"" to "DA39A3EE5E6B4B0D3255BFEF95601890AFD80709",
	"abc" to "A9993E364706816ABA3E25717850C26C9CD0D89D",
	"abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq" to "84983E441C3BD26EBAAE4AA1F95129E5E54670F1",
	"abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu" to "A49B2446A02C645BF419F995B67091253A04A259",
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
			assertTrue(message.hash().verify(message.hash()))
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

		@ParameterizedTest
		@MethodSource("testVectors")
		fun `hash message and compare hex strings`(message: String, expected: String) {
			assertEquals(expected, message.hash().toHex().value)
		}

		@ParameterizedTest
		@MethodSource("testVectors")
		fun `hash message and verify`(message: String, expected: String) {
			assertTrue(message.hash().verify(HexEncodedHash(expected).decode()))
		}

		@ParameterizedTest
		@MethodSource("testVectors")
		fun `hash message and compare byte arrays`(message: String, expected: String) {
			assertArrayEquals(HexEncodedHash(expected).decode().value, message.hash().value)
		}
	}
}
