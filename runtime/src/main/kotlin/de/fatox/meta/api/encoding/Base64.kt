@file:JvmName("Base64Utils")

package de.fatox.meta.api.encoding

import de.fatox.meta.api.crypto.Base64EncodedHash
import de.fatox.meta.api.crypto.Hash
import de.fatox.meta.api.crypto.Sha1Hash
import java.util.*

private val BASE64_ENCODER: Base64.Encoder = Base64.getEncoder()

private val BASE64_DECODER: Base64.Decoder = Base64.getDecoder()

fun CompressedByteArray.toBase64(): Base64EncodedByteArray = Base64EncodedByteArray(BASE64_ENCODER.encode(byteArray))

fun Base64EncodedByteArray.decode(): CompressedByteArray = CompressedByteArray(BASE64_DECODER.decode(byteArray))

fun Sha1Hash.toBase64(): Base64EncodedHash =
	Base64EncodedHash(BASE64_ENCODER.encode(value).toString(Charsets.UTF_8))

fun Base64EncodedHash.decode(): Hash = Sha1Hash(BASE64_DECODER.decode(value.toByteArray(Charsets.UTF_8)))
