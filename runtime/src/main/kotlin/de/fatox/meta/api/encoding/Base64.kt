@file:JvmName("Base64Utils")

package de.fatox.meta.api.encoding

import java.util.*

private val BASE64_ENCODER: Base64.Encoder = Base64.getEncoder()

private val BASE64_DECODER: Base64.Decoder = Base64.getDecoder()

fun CompressedByteArray.toBase64(): Base64EncodedByteArray = Base64EncodedByteArray(BASE64_ENCODER.encode(byteArray))

fun Base64EncodedByteArray.decode(): CompressedByteArray = CompressedByteArray(BASE64_DECODER.decode(byteArray))
