package de.fatox.meta.assets

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.SeekableByteChannel
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A [SeekableByteChannel] implementation that wraps a byte[].
 *
 *
 * When this channel is used for writing an internal buffer grows to accommodate incoming data. The natural size
 * limit is the value of [Integer.MAX_VALUE] and it is not possible to [set the position][.position] or
 * [truncate][.truncate] to a value bigger than that.  Internal buffer can be accessed via [ ][XPKByteChannel.array].
 *
 * @since 1.13
 * @NotThreadSafe
 */
class XPKByteChannel @JvmOverloads constructor(private var data: ByteArray = ByteArray(0)) : SeekableByteChannel {
    private val closed = AtomicBoolean()
    private var position = 0
    private var size: Int

    /**
     * Constructor taking a size of storage to be allocated.
     *
     *
     * Creates a channel and allocates internal storage of a given size.
     *
     * @param size size of internal buffer to allocate, in bytes.
     */
    constructor(size: Int) : this(ByteArray(size)) {}

    /**
     * Returns this channel's position.
     *
     *
     * This method violates the contract of [SeekableByteChannel.position] as it will not throw any exception
     * when invoked on a closed channel. Instead it will return the position the channel had when close has been
     * called.
     */
    override fun position(): Long {
        return position.toLong()
    }

    @Throws(IOException::class)
    override fun position(newPosition: Long): SeekableByteChannel {
        ensureOpen()
        require(!(newPosition < 0L || newPosition > Int.MAX_VALUE)) { "Position has to be in range 0.. " + Int.MAX_VALUE }
        position = newPosition.toInt()
        return this
    }

    /**
     * Returns the current size of entity to which this channel is connected.
     *
     *
     * This method violates the contract of [SeekableByteChannel.size] as it will not throw any exception when
     * invoked on a closed channel. Instead it will return the size the channel had when close has been called.
     */
    override fun size(): Long {
        return size.toLong()
    }

    /**
     * Truncates the entity, to which this channel is connected, to the given size.
     *
     *
     * This method violates the contract of [SeekableByteChannel.truncate] as it will not throw any exception when
     * invoked on a closed channel.
     */
    override fun truncate(newSize: Long): SeekableByteChannel {
        require(!(newSize < 0L || newSize > Int.MAX_VALUE)) { "Size has to be in range 0.. " + Int.MAX_VALUE }
        if (size > newSize) {
            size = newSize.toInt()
        }
        if (position > newSize) {
            position = newSize.toInt()
        }
        return this
    }

    @Throws(IOException::class)
    override fun read(buf: ByteBuffer): Int {
        ensureOpen()
        var wanted = buf.remaining()
        val possible = size - position
        if (possible <= 0) {
            return -1
        }
        if (wanted > possible) {
            wanted = possible
        }
        buf.put(data, position, wanted)
        position += wanted
        return wanted
    }

    override fun close() {
        closed.set(true)
    }

    override fun isOpen(): Boolean {
        return !closed.get()
    }

    @Throws(IOException::class)
    override fun write(b: ByteBuffer): Int {
        ensureOpen()
        var wanted = b.remaining()
        val possibleWithoutResize = size - position
        if (wanted > possibleWithoutResize) {
            val newSize = position + wanted
            if (newSize < 0) { // overflow
                resize(Int.MAX_VALUE)
                wanted = Int.MAX_VALUE - position
            } else {
                resize(newSize)
            }
        }
        b[data, position, wanted]
        position += wanted
        if (size < position) {
            size = position
        }
        return wanted
    }

    /**
     * Obtains the array backing this channel.
     *
     *
     * NOTE:
     * The returned buffer is not aligned with containing data, use
     * [.size] to obtain the size of data stored in the buffer.
     *
     * @return internal byte array.
     */
    fun array(): ByteArray {
        return data
    }

    private fun resize(newLength: Int) {
        var len = data.size
        if (len <= 0) {
            len = 1
        }
        if (newLength < NAIVE_RESIZE_LIMIT) {
            while (len < newLength) {
                len = len shl 1
            }
        } else { // avoid overflow
            len = newLength
        }
        data = Arrays.copyOf(data, len)
    }

    @Throws(ClosedChannelException::class)
    private fun ensureOpen() {
        if (!isOpen) {
            throw ClosedChannelException()
        }
    }

    companion object {
        private const val NAIVE_RESIZE_LIMIT = Int.MAX_VALUE shr 1
    }
    /**
     * Constructor taking a byte array.
     *
     *
     * This constructor is intended to be used with pre-allocated buffer or when
     * reading from a given byte array.
     *
     * @param data input data or pre-allocated array.
     */
    /**
     * Parameterless constructor - allocates internal buffer by itself.
     */
    init {
        size = data.size - HASH_LENGTH
    }
}