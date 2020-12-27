@file:JvmName("Base64Utils")
@file:Suppress("NOTHING_TO_INLINE")

package de.fatox.meta.api.encoding

import de.fatox.meta.api.crypto.EncodedHash
import de.fatox.meta.api.crypto.Hash
import java.util.*

@PublishedApi
internal val BASE64_ENCODER: Base64.Encoder = Base64.getEncoder()

@PublishedApi
internal val BASE64_DECODER: Base64.Decoder = Base64.getDecoder()

@PublishedApi
internal inline fun CompressedByteArray.encode(): EncodedByteArray = EncodedByteArray(BASE64_ENCODER.encode(byteArray))

@PublishedApi
internal inline fun String.decode(): CompressedByteArray = CompressedByteArray(BASE64_DECODER.decode(this))

internal inline fun Hash.encodeToString(): EncodedHash =
	EncodedHash(BASE64_ENCODER.encode(value).toString(Charsets.UTF_8))
