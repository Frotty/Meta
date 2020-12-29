package de.fatox.meta.assets

import java.io.IOException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object HashUtils {
    private val buffer = ByteBuffer.allocate(1024 * 4 * 4)
    fun computeSha1(channel: ReadableByteChannel): ByteArray {
        return try {
            buffer.rewind()
            val digest = MessageDigest.getInstance("SHA-1")
            while (channel.read(buffer) != -1) {
                buffer.flip()
                digest.update(buffer)
                buffer.clear()
            }
            digest.digest()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    //  http://stackoverflow.com/a/3940857/314015
    fun hex(data: ByteArray?): String {
        return String.format("%040x", BigInteger(1, data))
    }
}