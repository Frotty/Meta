package de.fatox.meta.assets

import de.fatox.meta.api.crypto.HASH_LENGTH
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.NonWritableChannelException
import java.nio.channels.SeekableByteChannel

/**
 * A read-only [SeekableByteChannel] view over XPK bytes already in memory.
 *
 * [size] deliberately excludes the trailing [HASH_LENGTH]-byte hash so the 7z reader never sees it.
 *
 * Constructing a view is free - it holds the caller's array by reference and owns only a cursor - which matters
 * because `SevenZFile.close()` closes the channel it was handed. One archive outlives many readers, so each reader
 * gets its own view rather than sharing one; the previous implementation instead never closed its readers at all,
 * because closing would have taken the shared channel with it.
 *
 * The write, truncate and grow paths this class inherited from Commons Compress' `SeekableInMemoryByteChannel` are
 * gone: an archive being read is never written to, and a mutable buffer behind a decompressor is only a hazard.
 */
class XPKByteChannel(private val data: ByteArray) : SeekableByteChannel {
	private val contentSize: Int = (data.size - HASH_LENGTH).coerceAtLeast(0)
	private var closed = false
	private var position = 0

	/**
	 * Kept for binary compatibility with the parameterless constructor the previous default argument produced.
	 *
	 * That one left `size` at `-HASH_LENGTH`; this yields a valid empty channel instead of reinstating the defect.
	 */
	@Deprecated("An XPK channel is a view over archive bytes; construct it with them.")
	constructor() : this(ByteArray(0))

	/**
	 * The backing array, whose length includes the hash trailer that [size] excludes.
	 *
	 * Retained for compatibility only - this hands out a mutable reference to bytes a decompressor is reading.
	 */
	@Deprecated("Exposes the archive's mutable backing store; hold your own reference to the bytes instead.")
	fun array(): ByteArray = data

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

	@Throws(IOException::class)
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
