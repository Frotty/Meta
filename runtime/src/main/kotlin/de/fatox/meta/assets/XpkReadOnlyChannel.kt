package de.fatox.meta.assets

import de.fatox.meta.api.crypto.HASH_LENGTH
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.NonWritableChannelException
import java.nio.channels.SeekableByteChannel

/**
 * The read-only view over XPK bytes that the loader actually uses.
 *
 * Split out from the public [XPKByteChannel], which is a copy of Commons Compress' `SeekableInMemoryByteChannel` and
 * therefore advertises a growable write path. Reusing that class for decoding conflated two things: a published
 * general-purpose buffer, and the archive's own reader. Keeping them apart means the loader gets a channel that
 * cannot be written to or resized behind a running decompressor, and the public class keeps the behaviour it always
 * documented.
 *
 * [size] deliberately excludes the trailing [HASH_LENGTH]-byte hash so the 7z reader never sees it.
 *
 * Constructing a view is free - it holds the caller's array by reference and owns only a cursor - which matters
 * because `SevenZFile.close()` closes the channel it was handed. One archive outlives many readers, so each reader
 * gets its own view rather than sharing one.
 */
internal class XpkReadOnlyChannel(private val data: ByteArray) : SeekableByteChannel {
	private val contentSize: Int = (data.size - HASH_LENGTH).coerceAtLeast(0)
	private var closed = false
	private var position = 0

	override fun position(): Long = position.toLong()

	@Throws(IOException::class)
	override fun position(newPosition: Long): SeekableByteChannel {
		ensureOpen()
		require(newPosition in 0..contentSize.toLong()) { "Position $newPosition outside 0..$contentSize" }
		position = newPosition.toInt()
		return this
	}

	/** Archive content length, excluding the hash trailer. */
	override fun size(): Long = contentSize.toLong()

	@Throws(IOException::class)
	override fun read(buf: ByteBuffer): Int {
		ensureOpen()
		val available = contentSize - position
		if (available <= 0) return -1
		val count = minOf(buf.remaining(), available)
		buf.put(data, position, count)
		position += count
		return count
	}

	override fun write(src: ByteBuffer): Int = throw NonWritableChannelException()

	override fun truncate(size: Long): SeekableByteChannel = throw NonWritableChannelException()

	override fun isOpen(): Boolean = !closed

	override fun close() {
		closed = true
	}

	private fun ensureOpen() {
		if (closed) throw ClosedChannelException()
	}
}
