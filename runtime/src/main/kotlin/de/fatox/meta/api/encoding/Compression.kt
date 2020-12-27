@file:JvmName("CompressionUtils")
@file:Suppress("NOTHING_TO_INLINE")

package de.fatox.meta.api.encoding

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

@PublishedApi
internal val deflater: Deflater = Deflater()

@PublishedApi
internal val inflater: Inflater = Inflater()

@PublishedApi
internal const val BUFFER_LENGTH: Int = 1024

inline class CompressedByteArray(val byteArray: ByteArray)
inline class EncodedByteArray(val byteArray: ByteArray)

@PublishedApi
internal inline fun ByteArrayOutputStream.toCompressedByteArray(): CompressedByteArray =
	CompressedByteArray(toByteArray())

@PublishedApi
internal inline fun Inflater.with(data: CompressedByteArray) {
	reset()
	setInput(data.byteArray)
}

@PublishedApi
internal inline fun CompressedByteArray.decompress(): ByteArray = ByteArrayOutputStream(this.byteArray.size).use {
	inflater.with(this)
	val buffer = ByteArray(BUFFER_LENGTH)
	while (!inflater.finished()) it.write(buffer, 0, inflater.inflate(buffer))
	it.toByteArray()
}

@PublishedApi
internal inline fun Deflater.with(data: ByteArray) {
	reset()
	setInput(data)
	finish()
}

inline fun ByteArray.compress(): CompressedByteArray = ByteArrayOutputStream(this.size).use {
	deflater.with(this)
	val buffer = ByteArray(BUFFER_LENGTH)
	while (!deflater.finished()) it.write(buffer, 0, deflater.deflate(buffer))
	it.toCompressedByteArray()
}
