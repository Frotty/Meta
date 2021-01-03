@file:JvmName("HexUtils")

package de.fatox.meta.api.encoding

import com.badlogic.gdx.utils.StringBuilder
import de.fatox.meta.api.crypto.Hash
import de.fatox.meta.api.crypto.HexEncodedHash

// Taken from javax.xml.bind.DatatypeConverter

fun HexEncodedHash.decode(): Hash {
	// "111" is not a valid hex encoding.
	require(value.length % 2 == 0) { "hexBinary needs to be even-length: $this" }

	val out = ByteArray(value.length / 2)
	var i = 0
	while (i < value.length) {
		val h = value[i].hexToBin()
		val l = value[i + 1].hexToBin()
		require(!(h == -1 || l == -1)) { "contains illegal character for hexBinary: $this" }
		out[i / 2] = (h * 16 + l).toByte()
		i += 2
	}
	return Hash(out)
}

@Suppress("SpellCheckingInspection")
private val hexCode = "0123456789ABCDEF".toCharArray()

fun Hash.toHex(): HexEncodedHash {
	val r = StringBuilder(value.size * 2)
	value.forEach {
		val b = it.toInt()
		r.append(hexCode[b shr 4 and 0xF])
		r.append(hexCode[b and 0xF])
	}
	return HexEncodedHash(r.toString())
}

private fun Char.hexToBin(): Int {
	if (this in '0'..'9') return this - '0'
	if (this in 'A'..'F') return this - 'A' + 10
	return if (this in 'a'..'f') this - 'a' + 10 else -1
}
