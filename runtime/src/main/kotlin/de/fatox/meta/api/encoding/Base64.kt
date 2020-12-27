@file:JvmName("Base64Utils")

package de.fatox.meta.api.encoding

import com.badlogic.gdx.utils.StringBuilder
import de.fatox.meta.api.crypto.Base64EncodedHash
import de.fatox.meta.api.crypto.Hash
import de.fatox.meta.api.crypto.HexEncodedHash
import java.util.*

private val BASE64_ENCODER: Base64.Encoder = Base64.getEncoder()

private val BASE64_DECODER: Base64.Decoder = Base64.getDecoder()

fun CompressedByteArray.encode(): EncodedByteArray = EncodedByteArray(BASE64_ENCODER.encode(byteArray))

fun String.decode(): CompressedByteArray = CompressedByteArray(BASE64_DECODER.decode(this))

fun Hash.toBase64(): Base64EncodedHash =
	Base64EncodedHash(BASE64_ENCODER.encode(value).toString(Charsets.UTF_8))

fun Base64EncodedHash.decode(): Hash = Hash(BASE64_DECODER.decode(value.toByteArray(Charsets.UTF_8)))
