package de.fatox.meta.assets

import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest



object HashUtils {
	const val HASH_LENGTH = 20

	private val buffer = ByteBuffer.allocate(1024 * 4 * 4)
	private val digest: MessageDigest = MessageDigest.getInstance("SHA-1")

	fun requireValidHash(input: ByteArray) {
		digest.reset()
		digest.update(input, 0, input.size - HASH_LENGTH)
		val newHash = digest.digest()
		val oldHash = input.copyOfRange(input.size - HASH_LENGTH, input.lastIndex)
		check(newHash.contentEquals(oldHash))
		{ "game files invalid. expected: ${hex(oldHash)} actual: ${hex(newHash)}" }
	}

	//  http://stackoverflow.com/a/3940857/314015
	private fun hex(data: ByteArray?): String {
		return String.format("%040x", BigInteger(1, data))
	}
}