package de.fatox.meta.api.crypto

import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val XXH_PRIME64_1 = 0x9E3779B185EBCA87UL
private const val XXH_PRIME64_2 = 0xC2B2AE3D27D4EB4FUL
private const val XXH_PRIME64_3 = 0x165667B19E3779F9UL
private const val XXH_PRIME64_4 = 0x85EBCA77C2B2AE63UL
private const val XXH_PRIME64_5 = 0x27D4EB2F165667C5UL

fun XXH64(input: ByteBuffer, length: Int, seed: ULong): XX64Hash {
	input.limit(length)
	input.order(ByteOrder.LITTLE_ENDIAN)

	var hash: ULong
	if (length >= 32) {
		var v1 = seed + XXH_PRIME64_1 + XXH_PRIME64_2
		var v2 = seed + XXH_PRIME64_2
		var v3 = seed + 0UL
		var v4 = seed - XXH_PRIME64_1

		do {
			v1 = round(v1, input.getLong().toULong())
			v2 = round(v2, input.getLong().toULong())
			v3 = round(v3, input.getLong().toULong())
			v4 = round(v4, input.getLong().toULong())
		} while (input.remaining() >= 32)

		hash = v1.rotl(1) + v2.rotl(7) + v3.rotl(12) + v4.rotl(18);
		hash = mergeRound(hash, v1);
		hash = mergeRound(hash, v2);
		hash = mergeRound(hash, v3);
		hash = mergeRound(hash, v4);

	} else {
		hash = seed + XXH_PRIME64_5;
	}

	hash += length.toULong()

	return XX64Hash(finalize(hash, input).also { input.limit(input.capacity()) })
}

private fun finalize(hash: ULong, ptr: ByteBuffer): ULong {
	var hash = hash
	while (ptr.remaining() >= 8) {
		hash = hash xor round(0UL, ptr.getLong().toULong())
		hash = hash.rotl(27) * XXH_PRIME64_1 + XXH_PRIME64_4
	}
	if (ptr.remaining() >= 4) {
		hash = hash xor (ptr.getInt().toULong() * XXH_PRIME64_1)
		hash = hash.rotl(23) * XXH_PRIME64_2 + XXH_PRIME64_3;
	}
	while (ptr.remaining() > 0) {
		hash = hash xor (ptr.get().toULong() * XXH_PRIME64_5)
		hash = hash.rotl(11) * XXH_PRIME64_1;
	}
	return avalanche(hash);
}

private fun avalanche(hash: ULong): ULong {
	var hash: ULong = hash
	hash = hash xor (hash shr 33)
	hash *= XXH_PRIME64_2
	hash = hash xor (hash shr 29)
	hash *= XXH_PRIME64_3
	hash = hash xor (hash shr 32)
	return hash
}

private fun round(acc: ULong, input: ULong): ULong {
	var acc = acc
	acc += (input * XXH_PRIME64_2)
	acc = acc.rotl(31)
	acc *= XXH_PRIME64_1
	return acc
}

private fun mergeRound(acc: ULong, `val`: ULong): ULong {
	var out: ULong = acc
	out = out xor round(0UL, `val`)
	out = out * XXH_PRIME64_1 + XXH_PRIME64_4
	return out
}

private infix fun ULong.rotl(r: Int) = (((this) shl (r)) or ((this) shr (64 - (r))))
