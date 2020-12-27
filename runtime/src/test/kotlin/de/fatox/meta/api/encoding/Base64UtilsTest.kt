package de.fatox.meta.api.encoding

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test

internal class Base64UtilsTest {
	private val a = ByteArray(100) { (it * 31).toByte() }.compress()

	@Test
	fun `self test`() {
		assertArrayEquals(a.byteArray, a.toBase64().decode().byteArray)
	}
}